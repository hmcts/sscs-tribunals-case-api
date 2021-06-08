package uk.gov.hmcts.reform.sscs.ccd.presubmit.requesthearingrecording;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
public class RequestHearingRecordingAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
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

        DynamicListItem selectedRequestable = sscsCaseData.getRequestableHearingDetails().getValue();
        String hearingId = selectedRequestable.getCode();
        String hearingName = selectedRequestable.getLabel();

        HearingRecordingRequest hearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .requestingParty("dwp").status("requested").requestedHearing(hearingId).requestedHearingName(hearingName).build()).build();

        List<HearingRecordingRequest> hearingRecordingRequests = sscsCaseData.getHearingRecordingRequests();
        if (hearingRecordingRequests == null) {
            hearingRecordingRequests = new ArrayList<>();
        }
        hearingRecordingRequests.add(hearingRecordingRequest);

        sscsCaseData.setHearingRecordingRequests(hearingRecordingRequests);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);


        return response;
    }

    private void moveFromListToList(String requestedHearing, List<DynamicListItem> validHearings, List<DynamicListItem> otherHearings) {
        for (DynamicListItem item: validHearings) {
            if (item.getCode().equals(requestedHearing)) {
                validHearings.remove(item);
                otherHearings.add(item);
            }
        }
    }

    private boolean isBeforeDate(LocalDateTime currentDate, String hearingDate, String hearingTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return LocalDateTime.parse(hearingDate + " " + hearingTime, formatter).isBefore(currentDate);
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
