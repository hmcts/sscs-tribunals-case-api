package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_EVIDENCE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.RESPONSE_SUBMITTED_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPOND;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.HandlerUtils.isANewJointParty;

import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsType;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Slf4j
@Service
public class DwpUploadResponseHandler implements CallbackHandler<SscsCaseData> {

    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;

    @Autowired
    public DwpUploadResponseHandler(UpdateCcdCaseService updateCcdCaseService, IdamService idamService) {
        this.updateCcdCaseService = updateCcdCaseService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.DWP_UPLOAD_RESPONSE
            && READY_TO_LIST.getId().equals(callback.getCaseDetails().getCaseData().getCreatedInGapsFrom())
            && callback.getCaseDetails().getCaseData().getAppeal() != null
            && callback.getCaseDetails().getCaseData().getAppeal().getBenefitType() != null
            && !StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().getCode(),
            Benefit.IIDB.getShortName());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            log.info("Cannot handle this event for case id: {}", callback.getCaseDetails().getId());
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        final BenefitType benefitType = sscsCaseData.getAppeal().getBenefitType();

        if (isPotentiallyHarmfulEvidenceOrHasEditedEvidenceBundle(sscsCaseData)) {
            triggerDwpResponseReceived(callback.getCaseDetails().getId(), "Response received",
                "Update to response received as an Admin has to review the case");
        } else if (equalsIgnoreCase(benefitType.getCode(), Benefit.UC.getShortName())) {
            handleUc(callback);
        } else if (StringUtils.equalsIgnoreCase(benefitType.getCode(), Benefit.CHILD_SUPPORT.getShortName())
            || isBenefitTypeSscs5(callback.getCaseDetails().getCaseData().getBenefitType())) {
            handleChildSupportAndSscs5Case(callback);
        } else if (StringUtils.equalsIgnoreCase(benefitType.getCode(), Benefit.INFECTED_BLOOD_COMPENSATION.getShortName())) {
            handleIbcaCase(callback);
        } else {
            handleNonUc(callback);
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }

    private static boolean isPotentiallyHarmfulEvidenceOrHasEditedEvidenceBundle(SscsCaseData sscsCaseData) {
        return equalsIgnoreCase(sscsCaseData.getDwpEditedEvidenceReason(), "phme") || Optional.ofNullable(
                sscsCaseData.getDwpDocuments()).orElse(emptyList()).stream()
            .anyMatch(d -> DWP_EVIDENCE_BUNDLE.getValue().equals(d.getValue().getDocumentType()));
    }

    private void handleChildSupportAndSscs5Case(Callback<SscsCaseData> callback) {
        if (StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getDwpFurtherInfo(), "Yes")) {
            updateEventDetails(callback.getCaseDetails().getId(), EventType.DWP_RESPOND, "Response received",
                "Update to response received as an Admin has to review the case", sscsCaseDetails -> {
                    SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                    sscsCaseData.setDwpState(RESPONSE_SUBMITTED_DWP);
                    sscsCaseData.setInterlocReviewState(AWAITING_ADMIN_ACTION);
                    log.info("Updated case v2 with dwp load response event {} for id {}", EventType.DWP_RESPOND,
                        callback.getCaseDetails().getId());
                });
        } else if (StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getDwpFurtherInfo(), "No")) {
            if (isBenefitTypeSscs5(callback.getCaseDetails().getCaseData().getBenefitType()) && !StringUtils.equalsIgnoreCase(
                callback.getCaseDetails().getCaseData().getDwpEditedEvidenceReason(), "phme")) {
                triggerReadyToListEvent(callback);
            } else {
                if (isBenefitTypeSscs5(callback.getCaseDetails().getCaseData().getBenefitType())) {
                    updateEventDetails(callback.getCaseDetails().getId(), EventType.DWP_RESPOND, "Response received",
                        "Update to response received as an Admin has to review the case", sscsCaseDetails -> {
                            SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                            sscsCaseData.setDwpState(RESPONSE_SUBMITTED_DWP);
                            sscsCaseData.setInterlocReviewState(REVIEW_BY_JUDGE);
                            log.info("Updated case v2 with dwp load response event {} for id {}", EventType.DWP_RESPOND,
                                callback.getCaseDetails().getId());
                        });
                }
            }
        }
    }

    private boolean isBenefitTypeSscs5(Optional<Benefit> benefitType) {
        return benefitType.filter(benefit -> SscsType.SSCS5.equals(benefit.getSscsType())).isPresent();
    }

    private void handleNonUc(Callback<SscsCaseData> callback) {
        if ("Yes".equalsIgnoreCase(callback.getCaseDetails().getCaseData().getUrgentCase())) {
            triggerDwpRespondEventForUrgentCase(callback);
        } else if (StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getDwpFurtherInfo(), "no")) {
            triggerReadyToListEvent(callback);
        }
    }

    private void handleUc(Callback<SscsCaseData> callback) {
        boolean dwpFurtherInfo = StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getDwpFurtherInfo(), "yes");

        boolean disputedDecision = false;
        if (callback.getCaseDetails().getCaseData().getElementsDisputedIsDecisionDisputedByOthers() != null) {
            disputedDecision = StringUtils.equalsIgnoreCase(
                callback.getCaseDetails().getCaseData().getElementsDisputedIsDecisionDisputedByOthers(), "yes");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if ("Yes".equalsIgnoreCase(callback.getCaseDetails().getCaseData().getUrgentCase())) {
            triggerDwpRespondEventForUrgentCase(callback);
        } else if (!dwpFurtherInfo && !disputedDecision) {
            triggerReadyToListEvent(callback);
        } else {
            triggerDwpRespondEventForUc(callback, dwpFurtherInfo, disputedDecision, caseData);
        }

        if (isANewJointParty(callback, caseData)) {
            updateEventDetails(callback.getCaseDetails().getId(), EventType.JOINT_PARTY_ADDED, "Joint party added",
                "A joint party was added to the appeal", sscsCaseDetails -> {
                    SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                    sscsCaseData.setJointParty(caseData.getJointParty());
                    log.info("Updated case v2 with dwp load response event {} for id {}", EventType.JOINT_PARTY_ADDED,
                        callback.getCaseDetails().getId());
                });
        }
    }

    private void handleIbcaCase(Callback<SscsCaseData> callback) {
        final long caseId = callback.getCaseDetails().getId();
        updateEventDetails(caseId, EventType.DWP_RESPOND, "Response received.", "IBC case must move to responseReceived.",
            sscsCaseDetails -> {
                SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                sscsCaseData.setDwpState(RESPONSE_SUBMITTED_DWP);
                log.info("Updated case v2 with dwp respond event {} for id {}", EventType.DWP_RESPOND, caseId);
            });
    }

    private void triggerDwpRespondEventForUc(Callback<SscsCaseData> callback, boolean dwpFurtherInfo, boolean disputedDecision,
        SscsCaseData caseData) {
        log.info("updating to response received for case id: {}", caseData.getCcdCaseId());

        String description;
        if (dwpFurtherInfo && disputedDecision) {
            description = "update to response received event as there is further information to "
                + "assist the tribunal and there is a dispute.";
        } else if (dwpFurtherInfo) {
            description = "update to response received event as there is further information to " + "assist the tribunal.";
        } else {
            description = "update to response received event as there is a dispute.";
        }

        updateEventDetails(callback.getCaseDetails().getId(), EventType.DWP_RESPOND, "Response received", description,
            sscsCaseDetails -> {
                SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                sscsCaseData.setDwpState(RESPONSE_SUBMITTED_DWP);
                log.info("Updated case v2 with dwp load response event {} for id {}", EventType.DWP_RESPOND,
                    callback.getCaseDetails().getId());
            });
    }

    private void triggerDwpRespondEventForUrgentCase(Callback<SscsCaseData> callback) {
        updateEventDetails(callback.getCaseDetails().getId(), EventType.DWP_RESPOND, "Response received",
            "urgent hearing set to response received event", sscsCaseDetails -> {
                SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                sscsCaseData.setDwpState(RESPONSE_SUBMITTED_DWP);
                log.info("Updated case v2 with dwp load response event {} for id {}", EventType.DWP_RESPOND,
                    callback.getCaseDetails().getId());
            });
    }

    private void triggerReadyToListEvent(Callback<SscsCaseData> callback) {
        updateEventDetails(callback.getCaseDetails().getId(), EventType.READY_TO_LIST, "ready to list",
            "update to ready to list event as there is no further information to assist the tribunal and no dispute.",
            sscsCaseDetails -> {
                SscsCaseData sscsCaseData = sscsCaseDetails.getData();
                sscsCaseData.setDwpState(RESPONSE_SUBMITTED_DWP);
                sscsCaseData.setIgnoreCallbackWarnings(YesNo.YES);
                log.info("Updated case v2 with dwp load response event {} for id {}", EventType.READY_TO_LIST,
                    callback.getCaseDetails().getId());
            });
    }

    private void triggerDwpResponseReceived(long caseId, String summary, String description) {
        updateEventDetails(caseId, DWP_RESPOND, summary, description, sscsCaseDetails -> {
            SscsCaseData sscsCaseData = sscsCaseDetails.getData();
            sscsCaseData.setDwpState(RESPONSE_SUBMITTED_DWP);
            log.info("Updated case v2 with dwp load response event {} for id {}", EventType.DWP_RESPOND, caseId);
        });
    }

    private void updateEventDetails(Long caseId, EventType eventType, String summary, String description,
        Consumer<SscsCaseDetails> caseDetailsConsumer) {
        updateCcdCaseService.updateCaseV2(caseId, eventType.getCcdType(), summary, description, idamService.getIdamTokens(),
            caseDetailsConsumer);
    }
}
