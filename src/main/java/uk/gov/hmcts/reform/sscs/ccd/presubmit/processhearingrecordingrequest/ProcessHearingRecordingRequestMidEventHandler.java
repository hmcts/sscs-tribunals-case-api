package uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.model.RequestStatus;
import uk.gov.hmcts.reform.sscs.service.processhearingrecordingrequest.ProcessHearingRecordingRequestService;

@Component
@Slf4j
public class ProcessHearingRecordingRequestMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final ProcessHearingRecordingRequestService processHearingRecordingRequestService;

    @Autowired
    public ProcessHearingRecordingRequestMidEventHandler(ProcessHearingRecordingRequestService processHearingRecordingRequestService) {
        this.processHearingRecordingRequestService = processHearingRecordingRequestService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent() == EventType.PROCESS_HEARING_RECORDING_REQUEST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        final List<ProcessHearingRecordingRequest> processHearingRecordingRequest = caseData.getSscsHearingRecordingCaseData().getProcessHearingRecordingRequests();
        emptyIfNull(processHearingRecordingRequest)
                .forEach(p -> validateRequest(p, response));

        return response;
    }

    private void validateRequest(ProcessHearingRecordingRequest processHearingRecordingRequest, PreSubmitCallbackResponse<SscsCaseData> response) {
        validateParty(PartyItemList.APPELLANT, processHearingRecordingRequest, response);
        validateParty(PartyItemList.DWP, processHearingRecordingRequest, response);
        if (response.getData().isThereAJointParty()) {
            validateParty(PartyItemList.JOINT_PARTY, processHearingRecordingRequest, response);
        }
        boolean caseHasARepresentative = isYes(ofNullable(response.getData().getAppeal().getRep()).map(Representative::getHasRepresentative).orElse(NO.getValue()));

        if (caseHasARepresentative) {
            validateParty(PartyItemList.REPRESENTATIVE, processHearingRecordingRequest, response);
        }
    }

    private void validateParty(PartyItemList party, ProcessHearingRecordingRequest processHearingRecordingRequest, PreSubmitCallbackResponse<SscsCaseData> response) {
        Optional<Hearing> hearingOptional = getHearingFromHearingRecordingRequest(processHearingRecordingRequest, response);
        hearingOptional.ifPresent(hearing -> validateHearing(party, processHearingRecordingRequest, response, hearing));
    }

    private void validateHearing(PartyItemList party, ProcessHearingRecordingRequest processHearingRecordingRequest, PreSubmitCallbackResponse<SscsCaseData> response, Hearing hearing) {
        final Optional<RequestStatus> requestStatus = processHearingRecordingRequestService.getRequestStatus(party, hearing, response.getData());
        final Optional<RequestStatus> changedRequestStatus = processHearingRecordingRequestService.getChangedRequestStatus(party, processHearingRecordingRequest);
        if (requestStatus.isPresent() && changedRequestStatus.isPresent()) {
            validateIfRequestStatusChangedFromGrantedToRefused(requestStatus.get(), changedRequestStatus.get(), response);
            validateIfRequestStatusChangedFromRefusedToGranted(requestStatus.get(), changedRequestStatus.get(), response);
        }
    }

    private void validateIfRequestStatusChangedFromGrantedToRefused(RequestStatus requestStatus, RequestStatus changedRequestStatus, PreSubmitCallbackResponse<SscsCaseData> response) {
        if (requestStatus.equals(RequestStatus.GRANTED) && changedRequestStatus.equals(RequestStatus.REFUSED)) {
            response.addWarning("Are you sure you want to change the request status");
        }
    }

    private void validateIfRequestStatusChangedFromRefusedToGranted(RequestStatus requestStatus, RequestStatus changedRequestStatus, PreSubmitCallbackResponse<SscsCaseData> response) {
        if (requestStatus.equals(RequestStatus.REFUSED) && changedRequestStatus.equals(RequestStatus.GRANTED)) {
            response.addWarning("Are you sure you want to change the request status");
        }
    }

    private Optional<Hearing> getHearingFromHearingRecordingRequest(ProcessHearingRecordingRequest processHearingRecordingRequest, PreSubmitCallbackResponse<SscsCaseData> response) {
        return response.getData().getHearings().stream()
                .filter(hearing -> hearing.getValue().getHearingId().equals(processHearingRecordingRequest.getValue().getHearingId()))
                .findFirst();
    }

}
