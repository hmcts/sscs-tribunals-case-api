package uk.gov.hmcts.reform.sscs.ccd.presubmit.requesthearingrecording;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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

        DynamicListItem selectedRequestable = sscsCaseData.getHearingRecordingsData().getRequestableHearingDetails().getValue();
        String hearingId = selectedRequestable.getCode();
        String hearingName = selectedRequestable.getLabel();

        HearingRecordingRequest hearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .requestingParty(UploadParty.DWP.getValue()).status("requested").dateRequested(LocalDateTime.now())
                .requestedHearing(hearingId).requestedHearingName(hearingName).build()).build();

        List<HearingRecordingRequest> hearingRecordingRequests = sscsCaseData.getHearingRecordingsData().getRequestedHearings();
        if (hearingRecordingRequests == null) {
            hearingRecordingRequests = new ArrayList<>();
        }
        hearingRecordingRequests.add(hearingRecordingRequest);

        sscsCaseData.getHearingRecordingsData().setRequestedHearings(hearingRecordingRequests);
        sscsCaseData.getHearingRecordingsData().setHearingRecordingRequestOutstanding(YesNo.YES);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        return response;
    }
}
