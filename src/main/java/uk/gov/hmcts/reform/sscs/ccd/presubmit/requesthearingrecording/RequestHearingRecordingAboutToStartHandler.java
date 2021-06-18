package uk.gov.hmcts.reform.sscs.ccd.presubmit.requesthearingrecording;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
            response.addError("No hearing has been conducted on this case");
            return response;
        } else {
            LocalDateTime currentTime = LocalDateTime.now();

            //FIXME add logic for hearings with recordings
            List<DynamicListItem> validHearings = sscsCaseData.getHearings().stream()
                    .filter(hearing -> isBeforeDate(currentTime, hearing.getValue().getHearingDate(),
                            hearing.getValue().getTime()))
                    .map(hearing -> new DynamicListItem(hearing.getValue().getHearingId(), selectHearing(hearing)))
                    .collect(Collectors.toList());
            if (validHearings.isEmpty()) {
                response.addError("No hearing has been conducted on this case");
                return response;
            }

            List<HearingRecordingRequest> requests = sscsCaseData.getHearingRecordingsData().getHearingRecordingRequests();

            if (requests != null && !requests.isEmpty()) {
                List<HearingRecordingRequest> releasedHearingsCollection = new ArrayList<>();
                List<HearingRecordingRequest> requestedHearingsCollection = new ArrayList<>();
                StringBuilder requestedHearingText = new StringBuilder();
                StringBuilder releasedHearingText = new StringBuilder();

                for (HearingRecordingRequest request : requests) {
                    if (request.getValue().getRequestingParty().equals("dwp")) {
                        if (request.getValue().getStatus().equals("requested")) {
                            //moveFromListToList(request.getValue().getRequestedHearing(), validHearings, requestedHearings);
                            moveFromListToCollection(request, validHearings, requestedHearingsCollection);
                            requestedHearingText.append(request.getValue().getRequestedHearingName());
                            requestedHearingText.append(", ");
                        }
                        if (request.getValue().getStatus().equals("released")) {
                            moveFromListToCollection(request, validHearings, releasedHearingsCollection);
                            releasedHearingText.append(request.getValue().getRequestedHearingName());
                            releasedHearingText.append(", ");
                        }
                    }
                }

                if (validHearings.isEmpty()) {
                    response.addError("There are no hearings to request on this case");
                    return response;
                }

                if (!requestedHearingsCollection.isEmpty()) {
                    sscsCaseData.getHearingRecordingsData().setRequestedHearings(requestedHearingsCollection);
                    sscsCaseData.getHearingRecordingsData().setRequestedHearingsTextList(requestedHearingText.substring(0, requestedHearingText.length() - 2));
                    log.info("not empty");
                } else {
                    log.info("not setting the requested list");
                }
                if (!releasedHearingsCollection.isEmpty()) {
                    sscsCaseData.getHearingRecordingsData().setReleasedHearings(releasedHearingsCollection);
                    sscsCaseData.getHearingRecordingsData().setReleasedHearingsTextList(releasedHearingText.substring(0, releasedHearingText.length() - 2));
                    log.info("not empty");
                } else {
                    log.info("not setting the released list");
                }
            }

            sscsCaseData.getHearingRecordingsData().setRequestableHearingDetails(new DynamicList(new DynamicListItem("", ""), validHearings));
        }
        return response;
    }

    private void moveFromListToCollection(HearingRecordingRequest requestedHearing, List<DynamicListItem> validHearings, List<HearingRecordingRequest> requestedHearingsCollection) {
        for (DynamicListItem item: validHearings) {
            if (item.getCode().equals(requestedHearing.getValue().getRequestedHearing())) {
                validHearings.remove(item);
                requestedHearingsCollection.add(requestedHearing);
                break;
            }
        }
    }

    private void moveFromListToList(String requestedHearing, List<DynamicListItem> validHearings, List<DynamicListItem> otherHearings) {
        for (DynamicListItem item: validHearings) {
            if (item.getCode().equals(requestedHearing)) {
                validHearings.remove(item);
                otherHearings.add(item);
                break;
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
