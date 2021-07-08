package uk.gov.hmcts.reform.sscs.ccd.presubmit.requesthearingrecording;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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

        DynamicListItem selectedRequestable = sscsCaseData.getSscsHearingRecordingCaseData().getRequestableHearingDetails().getValue();
        String hearingId = selectedRequestable.getCode();
        String hearingName = selectedRequestable.getLabel();

        List<SscsHearingRecording> sscsHearingRecordingList = sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings()
                .stream().filter(r -> r.getValue().getHearingId().equals(hearingId)).collect(Collectors.toList());

        HearingRecordingRequest hearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                .requestingParty(PartyItemList.DWP.getCode()).status("requested")
                .dateRequested(LocalDateTime.now().format(DateTimeFormatter.ofPattern(UPLOAD_DATE_FORMATTER)))
                .sscsHearingRecordingList(sscsHearingRecordingList).build()).build();


        List<HearingRecordingRequest> hearingRecordingRequests = sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings();
        if (hearingRecordingRequests == null) {
            hearingRecordingRequests = new ArrayList<>();
        }
        hearingRecordingRequests.add(hearingRecordingRequest);

        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(hearingRecordingRequests);
        sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordingRequestOutstanding(YesNo.YES);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        return response;
    }
}
