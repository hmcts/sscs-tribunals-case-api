package uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.mutableEmptyListIfNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;


@Service
@Slf4j
public class ProcessHearingRecordingRequestAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.PROCESS_HEARING_RECORDING_REQUEST;

    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        SscsHearingRecordingCaseData sscsHearingRecordingCaseData =
                sscsCaseData.getSscsHearingRecordingCaseData();

        sscsHearingRecordingCaseData.getProcessHearingRecordingRequests().stream()
                .forEach(processHearingRecordingRequest -> {
                    processHearingRecordings(sscsCaseData,
                            processHearingRecordingRequest, PartyItemList.DWP);
                    processHearingRecordings(sscsCaseData,
                            processHearingRecordingRequest, PartyItemList.APPELLANT);
                    processHearingRecordings(sscsCaseData,
                            processHearingRecordingRequest, PartyItemList.JOINT_PARTY);
                });

        if (mutableEmptyListIfNull(sscsHearingRecordingCaseData.getRequestedHearings()).isEmpty()) {
            sscsHearingRecordingCaseData.setHearingRecordingRequestOutstanding(YesNo.NO);
        }

        sscsHearingRecordingCaseData.setProcessHearingRecordingRequests(Collections.emptyList());

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void processHearingRecordings(SscsCaseData sscsCaseData,
                                          ProcessHearingRecordingRequest processHearingRecordingRequest,
                                          PartyItemList partyItemList) {


        SscsHearingRecordingCaseData sscsHearingRecordingCaseData =
                sscsCaseData.getSscsHearingRecordingCaseData();
        ProcessHearingRecordingRequestDetails processHearingRecordingRequestValue =
                processHearingRecordingRequest.getValue();

        String status = null;
        switch (partyItemList) {
            case DWP:
                DynamicList dwpRequestStatus = processHearingRecordingRequestValue.getDwp();
                if (dwpRequestStatus != null) {
                    status = dwpRequestStatus.getValue().getCode();
                    break;
                } else {
                    return;
                }
                //TODO:Add unit tests for this, should be part of SSCS-9239
                /*
            case APPELLANT:
                DynamicList appellantRequestStatus = processHearingRecordingRequestValue.getAppellant();
                if (appellantRequestStatus != null) {
                    status = appellantRequestStatus.getValue().getCode();
                    break;
                } else {
                    return;
                }
            case JOINT_PARTY:
                DynamicList jointPartyStatus = processHearingRecordingRequestValue.getJointParty();
                if (jointPartyStatus != null) {
                    status = jointPartyStatus.getValue().getCode();
                    break;
                } else {
                    return;
                }
                 */
            default:
        }

        organiseHearingRequestsLists(sscsCaseData, partyItemList, sscsHearingRecordingCaseData,
                processHearingRecordingRequestValue, status);
    }

    private void organiseHearingRequestsLists(SscsCaseData sscsCaseData, PartyItemList partyItemList,
                                              SscsHearingRecordingCaseData sscsHearingRecordingCaseData,
                                              ProcessHearingRecordingRequestDetails processHearingRecordingRequestValue,
                                              String status) {

        List<HearingRecordingRequest> dwpReleasedHearings =
                mutableEmptyListIfNull(sscsHearingRecordingCaseData.getDwpReleasedHearings());
        List<HearingRecordingRequest> citizenReleasedHearings =
                mutableEmptyListIfNull(sscsHearingRecordingCaseData.getCitizenReleasedHearings());
        List<HearingRecordingRequest> refusedHearings =
                mutableEmptyListIfNull(sscsHearingRecordingCaseData.getRefusedHearings());
        List<HearingRecordingRequest> requestedHearings =
                mutableEmptyListIfNull(sscsHearingRecordingCaseData.getRequestedHearings());

        Set<HearingRecordingRequest> allHearingRecordingsRequests = Stream
                .of(requestedHearings, dwpReleasedHearings, citizenReleasedHearings, refusedHearings)
                .flatMap(Collection::stream).collect(Collectors.toSet());

        if (StringUtils.isNotBlank(status) && !status.equals(RequestStatus.REQUESTED.getValue())) {

            Set<HearingRecordingRequest> partyHearingRecordingsRequests = allHearingRecordingsRequests.stream()
                    .filter(isFromRequestingParty(partyItemList))
                    .filter(hasHearingId(processHearingRecordingRequestValue.getHearingId()))
                    .collect(Collectors.toSet());

            if (status.equals(RequestStatus.GRANTED.getValue())) {
                if (partyItemList.equals(PartyItemList.DWP)) {
                    dwpReleasedHearings.addAll(partyHearingRecordingsRequests);
                    sscsCaseData.setDwpState(DwpState.HEARING_RECORDING_PROCESSED.getId());
                } else {
                    citizenReleasedHearings.addAll(partyHearingRecordingsRequests);
                }
                refusedHearings.removeAll(partyHearingRecordingsRequests);
                requestedHearings.removeAll(partyHearingRecordingsRequests);
            } else if (status.equals(RequestStatus.REFUSED.getValue())) {
                if (partyItemList.equals(PartyItemList.DWP)) {
                    dwpReleasedHearings.removeAll(partyHearingRecordingsRequests);
                    sscsCaseData.setDwpState(DwpState.HEARING_RECORDING_PROCESSED.getId());
                } else {
                    citizenReleasedHearings.removeAll(partyHearingRecordingsRequests);
                }
                refusedHearings.addAll(partyHearingRecordingsRequests);
                requestedHearings.removeAll(partyHearingRecordingsRequests);
            }

            sscsHearingRecordingCaseData.setDwpReleasedHearings(dwpReleasedHearings);
            sscsHearingRecordingCaseData.setCitizenReleasedHearings(citizenReleasedHearings);
            sscsHearingRecordingCaseData.setRequestedHearings(requestedHearings);
            sscsHearingRecordingCaseData.setRefusedHearings(refusedHearings);
        }
    }

    @NotNull
    private Predicate<HearingRecordingRequest> isFromRequestingParty(PartyItemList party) {
        return recordingRequest -> recordingRequest.getValue().getRequestingParty().equals(party.getCode());
    }

    @NotNull
    private Predicate<HearingRecordingRequest> hasHearingId(String hearingId) {
        return recordingRequest ->
                recordingRequest.getValue().getSscsHearingRecordingList().stream()
                        .filter(hearing -> hearing.getValue().getHearingId()
                                .equals(hearingId))
                        .findAny().isPresent();
    }
}
