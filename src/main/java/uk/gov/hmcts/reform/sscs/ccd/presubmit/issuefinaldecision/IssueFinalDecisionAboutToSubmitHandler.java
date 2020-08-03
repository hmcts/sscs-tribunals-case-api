package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Component
@Slf4j
public class IssueFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;
    private final DecisionNoticeOutcomeService decisionNoticeOutcomeService;
    private final Validator validator;

    @Autowired
    public IssueFinalDecisionAboutToSubmitHandler(FooterService footerService,
        DecisionNoticeOutcomeService decisionNoticeOutcomeService, Validator validator) {
        this.footerService = footerService;
        this.decisionNoticeOutcomeService = decisionNoticeOutcomeService;
        this.validator = validator;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.ISSUE_FINAL_DECISION
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        calculateOutcomeCode(sscsCaseData, preSubmitCallbackResponse);

        if (preSubmitCallbackResponse.getErrors().isEmpty()) {

            sscsCaseData.setDwpState(FINAL_DECISION_ISSUED.getId());

            Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
            for (ConstraintViolation<SscsCaseData> violation : violations) {
                preSubmitCallbackResponse.addError(violation.getMessage());
            }
            if (!preSubmitCallbackResponse.getErrors().isEmpty()) {
                return preSubmitCallbackResponse;
            }

            if (sscsCaseData.getWriteFinalDecisionPreviewDocument() != null) {
                createFinalDecisionNoticeFromPreviewDraft(preSubmitCallbackResponse);
                clearTransientFields(preSubmitCallbackResponse);
            } else {
                preSubmitCallbackResponse.addError("There is no Preview Draft Decision Notice on the case so decision cannot be issued");
            }
        }

        return preSubmitCallbackResponse;
    }

    private void calculateOutcomeCode(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        Outcome outcome = decisionNoticeOutcomeService.determineOutcome(sscsCaseData);

        if (outcome != null) {
            sscsCaseData.setOutcome(outcome.getId());
        } else {
            log.error("Outcome cannot be empty when generating final decision. Something has gone wrong for caseId: ", sscsCaseData.getCcdCaseId());
            preSubmitCallbackResponse.addError("Outcome cannot be empty. Please check case data. If problem continues please contact support");
        }

    }

    private void createFinalDecisionNoticeFromPreviewDraft(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        SscsCaseData sscsCaseData = preSubmitCallbackResponse.getData();

        DocumentLink docLink = sscsCaseData.getWriteFinalDecisionPreviewDocument();

        DocumentLink documentLink = DocumentLink.builder()
            .documentUrl(docLink.getDocumentUrl())
            .documentFilename(docLink.getDocumentFilename())
            .documentBinaryUrl(docLink.getDocumentBinaryUrl())
            .build();


        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"));

        footerService.createFooterAndAddDocToCase(documentLink, sscsCaseData, DocumentType.FINAL_DECISION_NOTICE, now,
                null, null);
    }

    private void clearTransientFields(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        SscsCaseData sscsCaseData = preSubmitCallbackResponse.getData();

        sscsCaseData.setWriteFinalDecisionTypeOfHearing(null);
        sscsCaseData.setWriteFinalDecisionPresentingOfficerAttendedQuestion(null);
        sscsCaseData.setWriteFinalDecisionAppellantAttendedQuestion(null);
        sscsCaseData.setWriteFinalDecisionDisabilityQualifiedPanelMemberName(null);
        sscsCaseData.setWriteFinalDecisionMedicallyQualifiedPanelMemberName(null);
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);
        sscsCaseData.setWriteFinalDecisionStartDate(null);
        sscsCaseData.setWriteFinalDecisionEndDateType(null);
        sscsCaseData.setWriteFinalDecisionEndDate(null);
        sscsCaseData.setWriteFinalDecisionDateOfDecision(null);
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionTakingNutritionQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionManagingTherapyQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionWashAndBatheQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionManagingToiletNeedsQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionDressingAndUndressingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionCommunicatingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionReadingUnderstandingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionEngagingWithOthersQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionBudgetingDecisionsQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionPlanningAndFollowingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion(null);
        sscsCaseData.setWriteFinalDecisionReasons(null);
        sscsCaseData.setWriteFinalDecisionPageSectionReference(null);
        sscsCaseData.setWriteFinalDecisionPreviewDocument(null);
        sscsCaseData.setWriteFinalDecisionGeneratedDate(null);
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow(null);
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused(null);

        preSubmitCallbackResponse.getData().getSscsDocument()
                .removeIf(doc -> doc.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue()));
    }

}
