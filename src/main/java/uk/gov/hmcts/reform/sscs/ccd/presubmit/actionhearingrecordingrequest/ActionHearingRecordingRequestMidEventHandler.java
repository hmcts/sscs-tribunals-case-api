package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionhearingrecordingrequest;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.*;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.REQUESTED;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
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
import uk.gov.hmcts.reform.sscs.service.actionhearingrecordingrequest.ActionHearingRecordingRequestService;

@Component
@Slf4j
public class ActionHearingRecordingRequestMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final ActionHearingRecordingRequestService actionHearingRecordingRequestService;

    @Autowired
    public ActionHearingRecordingRequestMidEventHandler(ActionHearingRecordingRequestService actionHearingRecordingRequestService) {
        this.actionHearingRecordingRequestService = actionHearingRecordingRequestService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent() == EventType.ACTION_HEARING_RECORDING_REQUEST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        if ("selectHearing".equals(callback.getPageId())) {
            buildHearingRequestUi(caseData);
        } else {
            final ProcessHearingRecordingRequest processHearingRecordingRequest = caseData.getSscsHearingRecordingCaseData().getProcessHearingRecordingRequest();
            validateRequest(processHearingRecordingRequest, response);
        }

        return response;
    }

    private void buildHearingRequestUi(SscsCaseData sscsCaseData) {
        Optional<Hearing> hearing = emptyIfNull(sscsCaseData.getHearings()).stream()
                .filter(h -> sscsCaseData.getSscsHearingRecordingCaseData().getSelectHearingDetails().getValue().getCode().equals(h.getValue().getHearingId()))
                .findFirst();

        if (hearing.isPresent()) {
            final ProcessHearingRecordingRequest request = getProcessHearingRecordingRequest(hearing.get(), sscsCaseData);

            sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequest(request);

            if (sscsCaseData.getOtherParties() != null && sscsCaseData.getOtherParties().size() > 0) {
                sscsCaseData.getSscsHearingRecordingCaseData().setOtherPartyHearingRecordingReqUi(buildOtherPartyHearingRequestUi(hearing.get(), sscsCaseData));
            }
        }
    }

    private List<OtherPartyHearingRecordingReqUi> buildOtherPartyHearingRequestUi(Hearing hearing, SscsCaseData sscsCaseData) {
        List<OtherPartyHearingRecordingReqUi> otherPartyHearingRecordingReqUi = new ArrayList<>();
        for (CcdValue<OtherParty> otherParty : sscsCaseData.getOtherParties()) {

            otherPartyHearingRecordingReqUi.add(buildOtherPartyElement(sscsCaseData, hearing, otherParty.getValue().getId(), otherParty.getValue().getName(), PartyItemList.OTHER_PARTY));

            if (null != otherParty.getValue().getRep() && "Yes".equals(otherParty.getValue().getRep().getHasRepresentative())) {
                otherPartyHearingRecordingReqUi.add(buildOtherPartyElement(sscsCaseData, hearing, otherParty.getValue().getRep().getId(), otherParty.getValue().getRep().getName(), PartyItemList.OTHER_PARTY_REPRESENTATIVE));
            }
        }
        return otherPartyHearingRecordingReqUi;
    }

    private OtherPartyHearingRecordingReqUi buildOtherPartyElement(SscsCaseData sscsCaseData, Hearing hearing, String otherPartyId, Name name, PartyItemList otherPartyItem) {
        DynamicList otherPartyList = toDynamicList(otherPartyItem, otherPartyId, hearing, sscsCaseData);

        return OtherPartyHearingRecordingReqUi.builder()
                .value(OtherPartyHearingRecordingReqUiDetails.builder()
                        .otherPartyName(name.getFullNameNoTitle() + (PartyItemList.OTHER_PARTY_REPRESENTATIVE.equals(otherPartyItem) ? " - Representative" : ""))
                        .otherPartyId(otherPartyId)
                        .requestingParty(otherPartyItem.getCode())
                        .hearingRecordingStatus(otherPartyList).build()).build();
    }

    private ProcessHearingRecordingRequest getProcessHearingRecordingRequest(Hearing h, SscsCaseData sscsCaseData) {
        DynamicList dwp = toDynamicList(PartyItemList.DWP, h, sscsCaseData);
        DynamicList jointParty = toDynamicList(PartyItemList.JOINT_PARTY, h, sscsCaseData);
        DynamicList appellant = toDynamicList(PartyItemList.APPELLANT, h, sscsCaseData);
        DynamicList rep = toDynamicList(PartyItemList.REPRESENTATIVE, h, sscsCaseData);
        return new ProcessHearingRecordingRequest(h.getValue().getHearingId(),
                selectHearingTitle(h, sscsCaseData.getHearings()),
                actionHearingRecordingRequestService.getFormattedHearingInformation(h),
                getRecordings(h, sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings()),
                dwp,
                jointParty,
                appellant,
                rep
        );
    }

    private DynamicList toDynamicList(PartyItemList party, Hearing h, SscsCaseData sscsCaseData) {
        return toDynamicList(party, null, h, sscsCaseData);
    }

    private DynamicList toDynamicList(PartyItemList party, String otherPartyId, Hearing h, SscsCaseData sscsCaseData) {
        final Optional<RequestStatus> partyStatus = actionHearingRecordingRequestService.getRequestStatus(party, otherPartyId, h, sscsCaseData);
        final DynamicListItem selected = partyStatus
                .map(this::toDynamicListItem)
                .orElse(new DynamicListItem("", ""));

        List<DynamicListItem> others = Stream.of(GRANTED, REFUSED, REQUESTED)
                .filter(status -> isPartyStatusRequestedOrOtherOptions(partyStatus, status))
                .map(this::toDynamicListItem)
                .toList();

        return new DynamicList(selected, others);
    }

    private boolean isPartyStatusRequestedOrOtherOptions(Optional<RequestStatus> partyStatus, RequestStatus status) {
        return !status.equals(REQUESTED) || isPartyStatusRequested(partyStatus);
    }

    private boolean isPartyStatusRequested(Optional<RequestStatus> partyStatus) {
        return partyStatus.filter(s -> s.equals(REQUESTED)).isPresent();
    }

    private DynamicListItem toDynamicListItem(RequestStatus rs) {
        return new DynamicListItem(rs.getLabel(), rs.getLabel());
    }


    private List<CcdValue<DocumentLink>> getRecordings(Hearing h, List<SscsHearingRecording> sscsHearingRecordings) {
        return emptyIfNull(sscsHearingRecordings).stream()
                .filter(s -> s.getValue().getHearingId().equals(h.getValue().getHearingId()))
                .flatMap(s -> emptyIfNull(s.getValue().getRecordings()).stream())
                .map(r -> CcdValue.<DocumentLink>builder().value(r.getValue()).build())
                .toList();
    }

    @NotNull
    private String selectHearingTitle(Hearing hearing, List<Hearing> hearings) {
        int index = hearings.indexOf(hearing) + 1;
        return String.format("Hearing %s", index);
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
        if (response.getData().getOtherParties() != null && response.getData().getOtherParties().size() > 0) {
            validateOtherPartyUiData(processHearingRecordingRequest, response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi(), response);
        }
    }

    private void validateOtherPartyUiData(ProcessHearingRecordingRequest processHearingRecordingRequest, List<OtherPartyHearingRecordingReqUi> otherPartyHearingRecordingReqUi, PreSubmitCallbackResponse<SscsCaseData> response) {
        int numberOfParties = 0;
        for (CcdValue<OtherParty> otherParty : response.getData().getOtherParties()) {
            validateParty(PartyItemList.OTHER_PARTY, processHearingRecordingRequest, otherParty.getValue().getId(), otherPartyHearingRecordingReqUi, response);
            numberOfParties++;

            if (null != otherParty.getValue().getRep() && "Yes".equals(otherParty.getValue().getRep().getHasRepresentative())) {
                validateParty(PartyItemList.OTHER_PARTY_REPRESENTATIVE, processHearingRecordingRequest, otherParty.getValue().getRep().getId(), otherPartyHearingRecordingReqUi, response);
                numberOfParties++;
            }
        }

        if (numberOfParties != response.getData().getSscsHearingRecordingCaseData().getOtherPartyHearingRecordingReqUi().size()) {
            addWarningOtherPartyUiError(response);
        }
    }

    private void addWarningOtherPartyUiError(PreSubmitCallbackResponse<SscsCaseData> response) {
        response.addError("Please do not use the remove buttons within this event. You may need to start again.");
    }

    private void validateParty(PartyItemList party, ProcessHearingRecordingRequest processHearingRecordingRequest, PreSubmitCallbackResponse<SscsCaseData> response) {
        validateParty(party, processHearingRecordingRequest, null, null,  response);
    }

    private void validateParty(PartyItemList party, ProcessHearingRecordingRequest processHearingRecordingRequest, String otherPartyId, List<OtherPartyHearingRecordingReqUi> otherPartyHearingRecordingReqUi, PreSubmitCallbackResponse<SscsCaseData> response) {
        Optional<Hearing> hearingOptional = getHearingFromHearingRecordingRequest(processHearingRecordingRequest, response);
        hearingOptional.ifPresent(hearing -> validateHearing(party, otherPartyId, processHearingRecordingRequest, otherPartyHearingRecordingReqUi, response, hearing));
    }

    private void validateHearing(PartyItemList party, String otherPartyId, ProcessHearingRecordingRequest processHearingRecordingRequest, List<OtherPartyHearingRecordingReqUi> otherPartyHearingRecordingReqUi, PreSubmitCallbackResponse<SscsCaseData> response, Hearing hearing) {
        final Optional<RequestStatus> requestStatus = actionHearingRecordingRequestService.getRequestStatus(party, otherPartyId, hearing, response.getData());
        final Optional<RequestStatus> changedRequestStatus = actionHearingRecordingRequestService.getChangedRequestStatus(party, otherPartyId, processHearingRecordingRequest, otherPartyHearingRecordingReqUi);
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
                .filter(hearing -> hearing.getValue().getHearingId().equals(processHearingRecordingRequest.getHearingId()))
                .findFirst();
    }

}
