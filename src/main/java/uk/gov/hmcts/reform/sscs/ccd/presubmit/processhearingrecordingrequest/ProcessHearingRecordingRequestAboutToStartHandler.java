package uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest;


import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.GRANTED;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.REFUSED;
import static uk.gov.hmcts.reform.sscs.model.RequestStatus.REQUESTED;

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
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.model.RequestStatus;
import uk.gov.hmcts.reform.sscs.service.processhearingrecordingrequest.ProcessHearingRecordingRequestService;

@Service
@Slf4j
public class ProcessHearingRecordingRequestAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final ProcessHearingRecordingRequestService processHearingRecordingRequestService;

    @Autowired
    public ProcessHearingRecordingRequestAboutToStartHandler(ProcessHearingRecordingRequestService processHearingRecordingRequestService) {
        this.processHearingRecordingRequestService = processHearingRecordingRequestService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
                && callback.getEvent() == EventType.PROCESS_HEARING_RECORDING_REQUEST;
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

        final List<ProcessHearingRecordingRequest> requests = hearingsWithRecording.stream()
                .map(h -> getProcessHearingRecordingRequest(h, sscsCaseData))
                .collect(Collectors.toList());

        sscsCaseData.getSscsHearingRecordingCaseData().setProcessHearingRecordingRequests(requests);

        return response;
    }

    private ProcessHearingRecordingRequest getProcessHearingRecordingRequest(Hearing h, SscsCaseData sscsCaseData) {
        DynamicList dwp = toDynamicList(PartyItemList.DWP, h, sscsCaseData);
        DynamicList jointParty = toDynamicList(PartyItemList.JOINT_PARTY, h, sscsCaseData);
        DynamicList appellant = toDynamicList(PartyItemList.APPELLANT, h, sscsCaseData);
        ProcessHearingRecordingRequestDetails value = new ProcessHearingRecordingRequestDetails(h.getValue().getHearingId(),
                selectHearingTitle(h, sscsCaseData.getHearings()),
                processHearingRecordingRequestService.getFormattedHearingInformation(h),
                getRecordings(h, sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings()),
                dwp,
                jointParty,
                appellant
        );
        return new ProcessHearingRecordingRequest(value);
    }

    private DynamicList toDynamicList(PartyItemList party, Hearing h, SscsCaseData sscsCaseData) {
        final Optional<RequestStatus> partyStatus = processHearingRecordingRequestService.getRequestStatus(party, h, sscsCaseData);
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
    private String selectHearingTitle(Hearing hearing, List<Hearing> hearings) {
        int index = hearings.indexOf(hearing) + 1;
        return String.format("Hearing %s", index);
    }

}