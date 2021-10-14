package uk.gov.hmcts.reform.sscs.ccd.presubmit.requesthearingrecording;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;


@Service
@Slf4j
public class RequestHearingRecordingAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private static final String UPLOAD_DATE_FORMATTER = "yyyy-MM-dd";

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

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        DynamicListItem selectedRequestable = sscsCaseData.getSscsHearingRecordingCaseData().getRequestableHearingDetails().getValue();
        String hearingId = selectedRequestable.getCode();

        Optional<SscsHearingRecording> sscsHearingRecording = sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings()
                .stream().filter(r -> r.getValue().getHearingId().equals(hearingId)).findAny();

        if (sscsHearingRecording.isPresent()) {
            HearingRecordingRequest hearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                    .requestingParty(PartyItemList.DWP.getCode())
                    .dateRequested(LocalDateTime.now().format(DateTimeFormatter.ofPattern(UPLOAD_DATE_FORMATTER)))
                    .sscsHearingRecording(sscsHearingRecording.get().getValue()).build()).build();

            List<HearingRecordingRequest> hearingRecordingRequests = Optional.ofNullable(sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings()).orElse(new ArrayList<>());
            hearingRecordingRequests.add(hearingRecordingRequest);

            sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(hearingRecordingRequests);
            sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordingRequestOutstanding(YesNo.YES);
        } else {
            response.addError("Hearing record not found");
        }

        return response;
    }
}
