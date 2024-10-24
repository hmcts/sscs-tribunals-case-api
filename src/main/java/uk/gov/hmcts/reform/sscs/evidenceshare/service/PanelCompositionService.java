package uk.gov.hmcts.reform.sscs.evidenceshare.service;


import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;


@Service
public class PanelCompositionService {

    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;

    @Autowired
    public PanelCompositionService(UpdateCcdCaseService updateCcdCaseService, IdamService idamService) {
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
    }

    public void processCaseState(Callback<SscsCaseData> callback, SscsCaseData caseData, EventType eventType) {
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();

        if (State.RESPONSE_RECEIVED.equals(caseDetails.getState())) {
            updateCase(caseDetails.getId(),
                EventType.INTERLOC_REVIEW_STATE_AMEND,
                "",
                "",
                sscsCaseDetails -> {
                    SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                    sscsCaseData.setInterlocReviewState(InterlocReviewState.NONE);
                });
            return;
        }

        if (stateNotDormant(caseDetails.getState())) {
            if (caseData.getIsFqpmRequired() == null
                || hasDueDateSetAndOtherPartyWithoutHearingOption(caseData)) {
                if (stateNotWithFtaOrResponseReceived(caseDetails)) {
                    triggerCaseEventV2(
                        caseDetails.getId(),
                        EventType.NOT_LISTABLE,
                        "Not listable",
                        "Update to Not Listable as the case is either awaiting hearing enquiry form or for FQPM to be set"
                    );
                }
            } else {
                updateCase(caseDetails.getId(),
                    EventType.READY_TO_LIST,
                    "Ready to list",
                    "Update to ready to list event as there is no further information to assist the tribunal and no dispute.",
                    sscsCaseDetails -> {
                        SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                        if (eventType.equals(EventType.UPDATE_OTHER_PARTY_DATA)) {
                            sscsCaseData.setDirectionDueDate(null);
                        }
                    });
            }
        }
    }

    private boolean stateNotWithFtaOrResponseReceived(CaseDetails<SscsCaseData> caseDetails) {
        return !(State.WITH_DWP.equals(caseDetails.getState())
            || State.RESPONSE_RECEIVED.equals(caseDetails.getState()));
    }

    private static boolean stateNotDormant(State caseState) {
        return !State.DORMANT_APPEAL_STATE.equals(caseState);
    }

    private boolean hasDueDateSetAndOtherPartyWithoutHearingOption(SscsCaseData sscsCaseData) {
        return StringUtils.isNotBlank(sscsCaseData.getDirectionDueDate())
            && !everyOtherPartyHasAtLeastOneHearingOption(sscsCaseData);
    }

    private boolean everyOtherPartyHasAtLeastOneHearingOption(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getOtherParties() != null && !sscsCaseData.getOtherParties().isEmpty()) {
            return sscsCaseData.getOtherParties().stream().noneMatch(this::hasNoHearingOption);
        } else {
            return false;
        }
    }

    private boolean hasNoHearingOption(CcdValue<OtherParty> otherPartyCcdValue) {
        HearingOptions hearingOptions = otherPartyCcdValue.getValue().getHearingOptions();
        return hearingOptions == null
            || (StringUtils.isBlank(hearingOptions.getWantsToAttend())
            && StringUtils.isBlank(hearingOptions.getWantsSupport())
            && StringUtils.isBlank(hearingOptions.getLanguageInterpreter())
            && StringUtils.isBlank(hearingOptions.getLanguages())
            && StringUtils.isBlank(hearingOptions.getSignLanguageType())
            && (hearingOptions.getArrangements() == null || hearingOptions.getArrangements().isEmpty())
            && StringUtils.isBlank(hearingOptions.getScheduleHearing())
            && (hearingOptions.getExcludeDates() == null || hearingOptions.getExcludeDates().isEmpty())
            && StringUtils.isBlank(hearingOptions.getAgreeLessNotice())
            && StringUtils.isBlank(hearingOptions.getOther()));
    }

    private void updateCase(Long caseId, EventType eventType, String summary, String description, Consumer<SscsCaseDetails> caseDetailsConsumer) {
        updateCcdCaseService.updateCaseV2(caseId, eventType.getCcdType(), summary, description, idamService.getIdamTokens(), caseDetailsConsumer);
    }

    private void triggerCaseEventV2(Long caseId, EventType eventType, String summary, String description) {
        updateCcdCaseService.triggerCaseEventV2(caseId, eventType.getCcdType(), summary, description, idamService.getIdamTokens());
    }
}
