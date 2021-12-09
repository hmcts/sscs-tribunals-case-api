package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionhearingrecordingrequest;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.mutableEmptyListIfNull;

import java.time.LocalDate;
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
public class ActionHearingRecordingRequestAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.ACTION_HEARING_RECORDING_REQUEST;

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

        processPartyRequests(sscsCaseData, sscsHearingRecordingCaseData, sscsHearingRecordingCaseData.getProcessHearingRecordingRequest());

        if (mutableEmptyListIfNull(sscsHearingRecordingCaseData.getRequestedHearings()).isEmpty()) {
            sscsHearingRecordingCaseData.setHearingRecordingRequestOutstanding(YesNo.NO);
        }

        sscsHearingRecordingCaseData.setProcessHearingRecordingRequest(null);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void processPartyRequests(SscsCaseData sscsCaseData, SscsHearingRecordingCaseData sscsHearingRecordingCaseData, ProcessHearingRecordingRequest processHearingRecordingRequest) {
        ProcessHearingRecordingRequestDetails processHearingRecordingRequestValue =
                processHearingRecordingRequest.getValue();
        String hearingId = processHearingRecordingRequestValue.getHearingId();
        if (isPartyItemSet(processHearingRecordingRequestValue.getDwp())) {
            processHearingRecordingsRequestsForParty(sscsCaseData, PartyItemList.DWP, sscsHearingRecordingCaseData,
                    hearingId, processHearingRecordingRequestValue.getDwp().getValue().getCode(), null);
        }
        if (isPartyItemSet(processHearingRecordingRequest.getAppellant())) {
            processHearingRecordingsRequestsForParty(sscsCaseData, PartyItemList.APPELLANT, sscsHearingRecordingCaseData,
                    hearingId, processHearingRecordingRequestValue.getAppellant().getValue().getCode(), null);
        }
        if (isPartyItemSet(processHearingRecordingRequest.getJointParty())) {
            processHearingRecordingsRequestsForParty(sscsCaseData, PartyItemList.JOINT_PARTY, sscsHearingRecordingCaseData,
                    hearingId, processHearingRecordingRequestValue.getJointParty().getValue().getCode(), null);
        }
        if (isPartyItemSet(processHearingRecordingRequest.getRep())) {
            processHearingRecordingsRequestsForParty(sscsCaseData, PartyItemList.REPRESENTATIVE, sscsHearingRecordingCaseData,
                    hearingId, processHearingRecordingRequestValue.getRep().getValue().getCode(), null);
        }
        processHearingRecordingRequestValue.getOtherPartyRequests().stream()
                .map(CcdValue::getValue).forEach(opr -> processOtherPartyRequest(sscsCaseData, sscsHearingRecordingCaseData, hearingId, opr));
    }

    private void processOtherPartyRequest(SscsCaseData sscsCaseData, SscsHearingRecordingCaseData sscsHearingRecordingCaseData, String hearingId, OtherPartyRequest otherPartyRequest) {
        if (isPartyItemSet(otherPartyRequest.getRequestStatus())) {
            processHearingRecordingsRequestsForParty(sscsCaseData, PartyItemList.OTHER_PARTY, sscsHearingRecordingCaseData,
                    hearingId, otherPartyRequest.getRequestStatus().getValue().getCode(), otherPartyRequest.getOtherParty().getId());
        }
        if (isPartyItemSet(otherPartyRequest.getRepRequestStatus())) {
            processHearingRecordingsRequestsForParty(sscsCaseData, PartyItemList.OTHER_PARTY_REPRESENTATIVE, sscsHearingRecordingCaseData,
                    hearingId, otherPartyRequest.getRepRequestStatus().getValue().getCode(), otherPartyRequest.getOtherParty().getRep().getId());
        }
    }

    private boolean isPartyItemSet(DynamicList item) {
        return nonNull(item) && nonNull(item.getValue()) && nonNull(item.getValue().getCode());
    }

    private void processHearingRecordingsRequestsForParty(SscsCaseData sscsCaseData, PartyItemList partyItemList,
                                                          SscsHearingRecordingCaseData sscsHearingRecordingCaseData,
                                                          String hearingId, String status, String otherPartyId) {

        Set<HearingRecordingRequest> dwpReleasedHearings =
                new HashSet<>(mutableEmptyListIfNull(sscsHearingRecordingCaseData.getDwpReleasedHearings()));
        Set<HearingRecordingRequest> citizenReleasedHearings =
                new HashSet<>(mutableEmptyListIfNull(sscsHearingRecordingCaseData.getCitizenReleasedHearings()));
        Set<HearingRecordingRequest> refusedHearings =
                new HashSet<>(mutableEmptyListIfNull(sscsHearingRecordingCaseData.getRefusedHearings()));
        Set<HearingRecordingRequest> requestedHearings =
                new HashSet<>(mutableEmptyListIfNull(sscsHearingRecordingCaseData.getRequestedHearings()));

        Set<HearingRecordingRequest> allHearingRecordingsRequests = Stream
                .of(requestedHearings, dwpReleasedHearings, citizenReleasedHearings, refusedHearings)
                .flatMap(Collection::stream).collect(Collectors.toSet());

        if (StringUtils.isNotBlank(status) && !status.equals(RequestStatus.REQUESTED.getValue())) {
            Set<HearingRecordingRequest> partyHearingRecordingsRequests = allHearingRecordingsRequests.stream()
                    .filter(isFromRequestingParty(partyItemList, otherPartyId))
                    .filter(hr -> nonNull(hr.getValue().getSscsHearingRecording()))
                    .filter(hr -> nonNull(hr.getValue().getSscsHearingRecording().getHearingId()))
                    .filter(hasHearingId(hearingId))
                    .collect(Collectors.toSet());

            if (partyHearingRecordingsRequests.isEmpty()) {
                createNewHearingRecordingRequest(partyItemList, sscsHearingRecordingCaseData, hearingId, partyHearingRecordingsRequests);
            }

            if (status.equals(RequestStatus.GRANTED.getValue())) {
                refusedHearings.removeAll(partyHearingRecordingsRequests);
                requestedHearings.removeAll(partyHearingRecordingsRequests);
                partyHearingRecordingsRequests.forEach(req -> req.getValue()
                        .setDateApproved(LocalDate.now().toString()));
                if (partyItemList.equals(PartyItemList.DWP)) {
                    dwpReleasedHearings.addAll(partyHearingRecordingsRequests);
                    sscsCaseData.setDwpState(DwpState.HEARING_RECORDING_PROCESSED.getId());
                } else {
                    citizenReleasedHearings.addAll(partyHearingRecordingsRequests);
                }
            } else if (status.equals(RequestStatus.REFUSED.getValue())) {
                requestedHearings.removeAll(partyHearingRecordingsRequests);
                if (partyItemList.equals(PartyItemList.DWP)) {
                    dwpReleasedHearings.removeAll(partyHearingRecordingsRequests);
                    sscsCaseData.setDwpState(DwpState.HEARING_RECORDING_PROCESSED.getId());
                } else {
                    citizenReleasedHearings.removeAll(partyHearingRecordingsRequests);
                }
                refusedHearings.addAll(partyHearingRecordingsRequests);
                refusedHearings.forEach(req -> req.getValue().setDateApproved(null));
            }

            sscsHearingRecordingCaseData.setDwpReleasedHearings(List.copyOf(dwpReleasedHearings));
            sscsHearingRecordingCaseData.setCitizenReleasedHearings(List.copyOf(citizenReleasedHearings));
            sscsHearingRecordingCaseData.setRequestedHearings(List.copyOf(requestedHearings));
            sscsHearingRecordingCaseData.setRefusedHearings(List.copyOf(refusedHearings));
        }
    }

    private void createNewHearingRecordingRequest(PartyItemList partyItemList, SscsHearingRecordingCaseData sscsHearingRecordingCaseData, String hearingId, Set<HearingRecordingRequest> partyHearingRecordingsRequests) {
        Optional<SscsHearingRecording> recording = sscsHearingRecordingCaseData.getSscsHearingRecordings().stream()
                .filter(sscsHearing -> sscsHearing.getValue().getHearingId().equals(hearingId)).findAny();

        if (recording.isPresent()) {
            HearingRecordingRequest recordingRequest = HearingRecordingRequest.builder()
                    .value(HearingRecordingRequestDetails.builder()
                            .sscsHearingRecording(recording.get().getValue()).dateRequested(LocalDate.now().toString())
                            .requestingParty(partyItemList.getCode()).build()).build();
            partyHearingRecordingsRequests.add(recordingRequest);
        }
    }

    @NotNull
    private Predicate<HearingRecordingRequest> isFromRequestingParty(PartyItemList party, String otherPartyId) {
        return r -> (PartyItemList.OTHER_PARTY.equals(party) || PartyItemList.OTHER_PARTY_REPRESENTATIVE.equals(party)) ?
                (r.getValue().getRequestingParty().equals(party.getCode()) && otherPartyId.equals(r.getValue().getOtherPartyId())) :
                r.getValue().getRequestingParty().equals(party.getCode());
    }

    @NotNull
    private Predicate<HearingRecordingRequest> hasHearingId(String hearingId) {
        return recordingRequest -> recordingRequest.getValue().getSscsHearingRecording().getHearingId()
                                .equals(hearingId);
    }
}
