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
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Component
@Slf4j
public class IssueFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;
    private final DecisionNoticeService decisionNoticeService;
    private final Validator validator;

    @Autowired
    public IssueFinalDecisionAboutToSubmitHandler(FooterService footerService,
        DecisionNoticeService decisionNoticeService, Validator validator) {
        this.footerService = footerService;
        this.decisionNoticeService = decisionNoticeService;
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

        Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
        for (ConstraintViolation<SscsCaseData> violation : violations) {
            preSubmitCallbackResponse.addError(violation.getMessage());
        }

        calculateOutcomeCode(sscsCaseData, preSubmitCallbackResponse);

        if (preSubmitCallbackResponse.getErrors().isEmpty()) {

            sscsCaseData.setDwpState(FINAL_DECISION_ISSUED.getId());

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

        String benefitType = sscsCaseData.getAppeal().getBenefitType() == null ? null : sscsCaseData.getAppeal().getBenefitType().getCode();

        if (benefitType == null) {
            throw new IllegalStateException("Unable to determine benefit type");
        }

        DecisionNoticeOutcomeService decisionNoticeOutcomeService = decisionNoticeService.getOutcomeService(benefitType);

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

        sscsCaseData.setWriteFinalDecisionGenerateNotice(null);
        sscsCaseData.setWriteFinalDecisionTypeOfHearing(null);
        sscsCaseData.setWriteFinalDecisionPresentingOfficerAttendedQuestion(null);
        sscsCaseData.setWriteFinalDecisionAppellantAttendedQuestion(null);
        sscsCaseData.setWriteFinalDecisionDisabilityQualifiedPanelMemberName(null);
        sscsCaseData.setWriteFinalDecisionMedicallyQualifiedPanelMemberName(null);
        sscsCaseData.setWriteFinalDecisionStartDate(null);
        sscsCaseData.setWriteFinalDecisionEndDateType(null);
        sscsCaseData.setWriteFinalDecisionEndDate(null);
        sscsCaseData.setWriteFinalDecisionDateOfDecision(null);

        //PIP
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(null);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);
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
        sscsCaseData.setWriteFinalDecisionAnythingElse(null);

        //ESA
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMentalAssessmentQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionStandingAndSittingQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionReachingQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPickingUpQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionManualDexterityQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMakingSelfUnderstoodQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionCommunicationQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionNavigationQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionLossOfControlQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionConsciousnessQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionLearningTasksQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionAwarenessOfHazardsQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPersonalActionQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionCopingWithChangeQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionGettingAboutQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSocialEngagementQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionAppropriatenessOfBehaviourQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(null);
        sscsCaseData.setDwpReassessTheAward(null);
        sscsCaseData.getSscsEsaCaseData().setShowRegulation29Page(null);
        sscsCaseData.getSscsEsaCaseData().setShowSchedule3ActivitiesPage(null);
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(null);
        sscsCaseData.setWriteFinalDecisionDetailsOfDecision(null);
        sscsCaseData.getSscsEsaCaseData().setWcaAppeal(null);
        sscsCaseData.setSupportGroupOnlyAppeal(null);
        sscsCaseData.getSscsEsaCaseData().setDoesRegulation29Apply(null);
        sscsCaseData.getSscsEsaCaseData().setDoesRegulation35Apply(null);

        //UC
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMentalAssessmentQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionStandingAndSittingQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionReachingQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPickingUpQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionManualDexterityQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMakingSelfUnderstoodQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionCommunicationQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionNavigationQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionLossOfControlQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionConsciousnessQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionLearningTasksQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionAwarenessOfHazardsQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPersonalActionQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionCopingWithChangeQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionGettingAboutQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSocialEngagementQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionAppropriatenessOfBehaviourQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(null);
        sscsCaseData.setDwpReassessTheAward(null);
        sscsCaseData.getSscsUcCaseData().setShowSchedule8Paragraph4Page(null);
        sscsCaseData.getSscsUcCaseData().setShowSchedule7ActivitiesPage(null);
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(null);
        sscsCaseData.setWriteFinalDecisionDetailsOfDecision(null);
        sscsCaseData.getSscsUcCaseData().setLcwaAppeal(null);
        sscsCaseData.setSupportGroupOnlyAppeal(null);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(null);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(null);

        preSubmitCallbackResponse.getData().getSscsDocument()
                .removeIf(doc -> doc.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue()));
    }

}
