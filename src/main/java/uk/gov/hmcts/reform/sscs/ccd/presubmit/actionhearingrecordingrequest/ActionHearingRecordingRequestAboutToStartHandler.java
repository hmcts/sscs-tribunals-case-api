package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionhearingrecordingrequest;


import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
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
import uk.gov.hmcts.reform.sscs.service.actionhearingrecordingrequest.ActionHearingRecordingRequestService;

@Service
@Slf4j
public class ActionHearingRecordingRequestAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final ActionHearingRecordingRequestService actionHearingRecordingRequestService;

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static DateTimeFormatter resultFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Autowired
    public ActionHearingRecordingRequestAboutToStartHandler(ActionHearingRecordingRequestService actionHearingRecordingRequestService) {
        this.actionHearingRecordingRequestService = actionHearingRecordingRequestService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.ACTION_HEARING_RECORDING_REQUEST;
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

        if (isEmpty(sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings())) {
            response.addError("No hearing recordings on this case");
            return response;
        }

        List<String> hearingIdsWithRecording = emptyIfNull(sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings()).stream()
                .map(d -> d.getValue().getHearingId())
                .distinct()
                .collect(Collectors.toList());

        List<Hearing> hearingsWithRecording = emptyIfNull(sscsCaseData.getHearings()).stream()
                .filter(hearing -> hearingIdsWithRecording.contains(hearing.getValue().getHearingId()))
                .collect(Collectors.toList());

        List<DynamicListItem> validHearingsDynamicList = hearingsWithRecording.stream()
                .map(hearing -> new DynamicListItem(hearing.getValue().getHearingId(), selectHearing(hearing)))
                .collect(Collectors.toList());;

        sscsCaseData.getSscsHearingRecordingCaseData().setSelectHearingDetails(
                new DynamicList(new DynamicListItem("", ""), validHearingsDynamicList));

        return response;
    }

    @NotNull
    private String selectHearing(Hearing hearing) {
        return hearing.getValue().getVenue().getName() + " "
                + checkHearingTime(hearing.getValue().getTime()) + " "
                + LocalDate.parse(hearing.getValue().getHearingDate(), formatter).format(resultFormatter);
    }

    private ProcessHearingRecordingRequest getProcessHearingRecordingRequest(Hearing h, SscsCaseData sscsCaseData) {
        DynamicList dwp = toDynamicList(PartyItemList.DWP, null, h, sscsCaseData);
        DynamicList jointParty = toDynamicList(PartyItemList.JOINT_PARTY, null, h, sscsCaseData);
        DynamicList appellant = toDynamicList(PartyItemList.APPELLANT, null, h, sscsCaseData);
        DynamicList rep = toDynamicList(PartyItemList.REPRESENTATIVE, null, h, sscsCaseData);
        ProcessHearingRecordingRequestDetails value = new ProcessHearingRecordingRequestDetails(h.getValue().getHearingId(),
                selectHearingTitle(h, sscsCaseData.getHearings()),
                actionHearingRecordingRequestService.getFormattedHearingInformation(h),
                getRecordings(h, sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings()),
                dwp,
                jointParty,
                appellant,
                rep,
                getRequestedOtherParties(h, sscsCaseData)
        );
        return new ProcessHearingRecordingRequest(value);
    }

    private List<CcdValue<OtherPartyRequest>> getRequestedOtherParties(Hearing h, SscsCaseData sscsCaseData) {
        return sscsCaseData.getOtherParties().stream().map(CcdValue::getValue)
                .map(op -> toOtherPartyRequest(op, h, sscsCaseData))
                .collect(Collectors.toList());
    }

    private CcdValue<OtherPartyRequest> toOtherPartyRequest(OtherParty otherParty, Hearing h, SscsCaseData sscsCaseData) {
        DynamicList requestStatus = toDynamicList(PartyItemList.OTHER_PARTY, otherParty.getId(), h, sscsCaseData);
        DynamicList repRequestStatus = null;
        if (otherParty.hasRepresentative() && otherParty.getRep() != null && otherParty.getRep().getId() != null) {
            repRequestStatus = toDynamicList(PartyItemList.OTHER_PARTY_REPRESENTATIVE, otherParty.getRep().getId(), h, sscsCaseData);
        }

        return CcdValue.<OtherPartyRequest>builder()
                .value(OtherPartyRequest.builder()
                        .otherParty(otherParty)
                        .hasOtherPartyRep(repRequestStatus == null ? YesNo.NO.getValue() : YesNo.YES.getValue())
                        .requestStatus(requestStatus)
                        .repRequestStatus(repRequestStatus)
                        .build())
                .build();
    }

    private DynamicList toDynamicList(PartyItemList party, String otherPartyId, Hearing h, SscsCaseData sscsCaseData) {
        final Optional<RequestStatus> partyStatus = actionHearingRecordingRequestService.getRequestStatus(party, otherPartyId, h, sscsCaseData);
        final DynamicListItem selected = partyStatus
                .map(this::toDynamicListItem)
                .orElse(new DynamicListItem("", ""));

        List<DynamicListItem> others = List.of(GRANTED, REFUSED, REQUESTED).stream()
                .filter(status -> isPartyStatusRequestedOrOtherOptions(partyStatus, status))
                .map(this::toDynamicListItem)
                .collect(Collectors.toList());

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
                .flatMap(s -> s.getValue().getRecordings().stream())
                .map(r -> CcdValue.<DocumentLink>builder().value(r.getValue()).build())
                .collect(Collectors.toList());
    }

    @NotNull
    private String checkHearingTime(String hearingTime) {
        return (hearingTime.length() == 5) ? (hearingTime + ":00") : hearingTime;
    }

}
