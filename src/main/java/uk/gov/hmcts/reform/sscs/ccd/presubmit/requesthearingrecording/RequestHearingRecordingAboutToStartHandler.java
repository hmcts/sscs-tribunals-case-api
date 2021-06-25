package uk.gov.hmcts.reform.sscs.ccd.presubmit.requesthearingrecording;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;


@Service
@Slf4j
public class RequestHearingRecordingAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Autowired
    public RequestHearingRecordingAboutToStartHandler() {
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
            && callback.getEvent() == EventType.DWP_REQUEST_HEARING_RECORDING;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        //create the lists that user won't have permission for



        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (sscsCaseData.getHearings() == null || sscsCaseData.getHearings().isEmpty()) {
            return returnError(response, "No hearing has been conducted on this case");

        } else {
            List<DynamicListItem> validHearings = sscsCaseData.getHearings().stream()
                    .filter(hearing -> isHearingWithRecording(hearing, sscsCaseData.getSscsHearingRecordingCaseData()))
                    .map(hearing -> new DynamicListItem(hearing.getValue().getHearingId(), selectHearing(hearing)))
                    .collect(Collectors.toList());
            if (validHearings.isEmpty()) {
                return returnError(response, "No hearing has been conducted on this case");
            }

            List<HearingRecordingRequest> requestedHearingsCollection = sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings();
            List<HearingRecordingRequest> releasedHearingsCollection = sscsCaseData.getSscsHearingRecordingCaseData().getReleasedHearings();
            StringBuilder requestedHearingText = new StringBuilder();
            StringBuilder releasedHearingText = new StringBuilder();

            if (requestedHearingsCollection != null) {
                requestedHearingsCollection.stream().filter(r -> r.getValue().getRequestingParty().equals(UploadParty.DWP.getValue()))
                        .forEach(r -> removeFromListAndAddText(r, validHearings, requestedHearingText));
            }

            if (releasedHearingsCollection != null) {
                releasedHearingsCollection.stream().filter(r -> r.getValue().getRequestingParty().equals(UploadParty.DWP.getValue()))
                        .forEach(r -> removeFromListAndAddText(r, validHearings, releasedHearingText));
            }


            if (validHearings.isEmpty()) {
                return returnError(response, "There are no hearings to request on this case");
            }

            sscsCaseData.getSscsHearingRecordingCaseData().setRequestableHearingDetails(new DynamicList(new DynamicListItem("", ""), validHearings));
            if (requestedHearingsCollection != null && !requestedHearingsCollection.isEmpty()) {
                sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(requestedHearingsCollection);
                sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearingsTextList(requestedHearingText.substring(0, requestedHearingText.length() - 2));
            } else {
                sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearingsTextList("There are no outstanding DWP hearing recording requests on this case");
            }
            if (releasedHearingsCollection != null && !releasedHearingsCollection.isEmpty()) {
                sscsCaseData.getSscsHearingRecordingCaseData().setReleasedHearings(releasedHearingsCollection);
                sscsCaseData.getSscsHearingRecordingCaseData().setReleasedHearingsTextList(releasedHearingText.substring(0, releasedHearingText.length() - 2));
            } else {
                sscsCaseData.getSscsHearingRecordingCaseData().setReleasedHearingsTextList("No hearing recordings have been released to DWP on this case");
            }
        }
        return response;
    }

    private PreSubmitCallbackResponse<SscsCaseData> returnError(PreSubmitCallbackResponse<SscsCaseData> response, String message) {
        response.addError(message);
        return response;
    }

    private boolean isHearingWithRecording(Hearing hearing, SscsHearingRecordingCaseData hearingRecordingsData) {
        List<SscsHearingRecording> sscsHearingRecordings = hearingRecordingsData.getSscsHearingRecordings();

        if (sscsHearingRecordings != null) {
            return sscsHearingRecordings.stream().anyMatch(r -> r.getValue().getHearingId().equals(hearing.getValue().getHearingId()));
        }
        return false;
    }

    private void removeFromListAndAddText(HearingRecordingRequest request, List<DynamicListItem> validHearings, StringBuilder stringBuilder) {
        stringBuilder.append(request.getValue().getRequestedHearingName());
        stringBuilder.append(", ");
        DynamicListItem item = validHearings.stream().filter(i -> i.getCode().equals(request.getValue().getRequestedHearing())).findFirst().orElse(null);
        if (item != null) {
            validHearings.remove(item);
        }
    }

    @NotNull
    private String selectHearing(Hearing hearing) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter resultFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        return hearing.getValue().getVenue().getName() + " "
            + hearing.getValue().getTime() + " "
            + LocalDate.parse(hearing.getValue().getHearingDate(), formatter).format(resultFormatter);
    }

}
