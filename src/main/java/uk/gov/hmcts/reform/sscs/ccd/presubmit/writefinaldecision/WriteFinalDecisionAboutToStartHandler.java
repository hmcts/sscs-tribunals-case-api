package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@AllArgsConstructor
public class WriteFinalDecisionAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final UserDetailsService userDetailsService;
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_START
                && callback.getEvent() == EventType.WRITE_FINAL_DECISION
                && nonNull(callback.getCaseDetails())
                && nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();
        State state = callback.getCaseDetails().getState();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (isPostHearingsEnabled && (State.DORMANT_APPEAL_STATE.equals(state) || State.POST_HEARING.equals(state))) {
            List<String> userRoles = userDetailsService.getUserRoles(userAuthorisation);

            if (userRoles.contains(UserRole.JUDGE.getValue())
                    && !userRoles.contains(UserRole.SALARIED_JUDGE.getValue())) {
                preSubmitCallbackResponse.addError("You have already issued a final decision, only a salaried Judge can correct it");

                return preSubmitCallbackResponse;
            }
        }

        SscsUtil.setCorrectionInProgress(caseDetails, isPostHearingsEnabled);
        clearTransientFields(sscsCaseData);

        return preSubmitCallbackResponse;
    }

    private void clearTransientFields(SscsCaseData caseData) {
        if (isDraftDecisionNotOnCase(caseData) && !isCorrectionInProgress(caseData)) {
            clearFinalDecisionTransientFields(caseData);
        } else if (isCorrectionInProgress(caseData)) {
            caseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(null);
        }
    }

    private boolean isDraftDecisionNotOnCase(SscsCaseData caseData) {
        return isNull(caseData.getSscsDocument())
                || caseData.getSscsDocument().stream()
                .noneMatch(doc -> doc.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue()));
    }

    private boolean isCorrectionInProgress(SscsCaseData caseData) {
        return isPostHearingsEnabled && isYes(caseData.getPostHearing().getCorrection().getIsCorrectionFinalDecisionInProgress());
    }

    private void clearFinalDecisionTransientFields(SscsCaseData sscsCaseData) {
        SscsFinalDecisionCaseData finalDecisionCaseData = sscsCaseData.getSscsFinalDecisionCaseData();
        finalDecisionCaseData.setWriteFinalDecisionGenerateNotice(null);
        finalDecisionCaseData.setWriteFinalDecisionTypeOfHearing(null);
        finalDecisionCaseData.setWriteFinalDecisionPresentingOfficerAttendedQuestion(null);
        finalDecisionCaseData.setWriteFinalDecisionAppellantAttendedQuestion(null);
        finalDecisionCaseData.setWriteFinalDecisionAppointeeAttendedQuestion(null);
        finalDecisionCaseData.setWriteFinalDecisionDisabilityQualifiedPanelMemberName(null);
        finalDecisionCaseData.setWriteFinalDecisionMedicallyQualifiedPanelMemberName(null);
        finalDecisionCaseData.setWriteFinalDecisionStartDate(null);
        finalDecisionCaseData.setWriteFinalDecisionEndDateType(null);
        finalDecisionCaseData.setWriteFinalDecisionEndDate(null);
        finalDecisionCaseData.setWriteFinalDecisionDateOfDecision(null);

        sscsCaseData.setWcaAppeal(null);
        finalDecisionCaseData.setOtherPartyAttendedQuestions(new ArrayList<>());

        //PIP
        SscsPipCaseData sscsPipCaseData = sscsCaseData.getSscsPipCaseData();
        sscsPipCaseData.setPipWriteFinalDecisionDailyLivingQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionMobilityQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionPreparingFoodQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionTakingNutritionQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionManagingTherapyQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionWashAndBatheQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionManagingToiletNeedsQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionDressingAndUndressingQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionCommunicatingQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionReadingUnderstandingQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionEngagingWithOthersQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionBudgetingDecisionsQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionPlanningAndFollowingQuestion(null);
        sscsPipCaseData.setPipWriteFinalDecisionMovingAroundQuestion(null);
        finalDecisionCaseData.setWriteFinalDecisionReasons(null);
        finalDecisionCaseData.setWriteFinalDecisionPageSectionReference(null);
        finalDecisionCaseData.setWriteFinalDecisionPreviewDocument(null);
        finalDecisionCaseData.setWriteFinalDecisionGeneratedDate(null);
        finalDecisionCaseData.setWriteFinalDecisionIsDescriptorFlow(null);
        finalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused(null);
        finalDecisionCaseData.setWriteFinalDecisionAnythingElse(null);

        //ESA
        SscsEsaCaseData sscsEsaCaseData = sscsCaseData.getSscsEsaCaseData();
        sscsEsaCaseData.setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionMentalAssessmentQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionMobilisingUnaidedQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionStandingAndSittingQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionReachingQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionPickingUpQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionManualDexterityQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionMakingSelfUnderstoodQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionCommunicationQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionNavigationQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionLossOfControlQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionConsciousnessQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionLearningTasksQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionAwarenessOfHazardsQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionPersonalActionQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionCopingWithChangeQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionGettingAboutQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionSocialEngagementQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionAppropriatenessOfBehaviourQuestion(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesApply(null);
        sscsEsaCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(null);
        sscsCaseData.setDwpReassessTheAward(null);
        sscsEsaCaseData.setShowRegulation29Page(null);
        sscsEsaCaseData.setShowSchedule3ActivitiesPage(null);
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(null);
        finalDecisionCaseData.setWriteFinalDecisionDetailsOfDecision(null);
        sscsCaseData.setSupportGroupOnlyAppeal(null);
        sscsEsaCaseData.setDoesRegulation29Apply(null);
        sscsEsaCaseData.setDoesRegulation35Apply(null);

        //UC
        SscsUcCaseData sscsUcCaseData = sscsCaseData.getSscsUcCaseData();
        sscsUcCaseData.setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionMentalAssessmentQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionMobilisingUnaidedQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionStandingAndSittingQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionReachingQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionPickingUpQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionManualDexterityQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionMakingSelfUnderstoodQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionCommunicationQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionNavigationQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionLossOfControlQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionConsciousnessQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionLearningTasksQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionAwarenessOfHazardsQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionPersonalActionQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionCopingWithChangeQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionGettingAboutQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionSocialEngagementQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionAppropriatenessOfBehaviourQuestion(null);
        sscsUcCaseData.setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
        sscsUcCaseData.setUcWriteFinalDecisionSchedule7ActivitiesQuestion(null);
        sscsCaseData.setDwpReassessTheAward(null);
        sscsUcCaseData.setShowSchedule8Paragraph4Page(null);
        sscsUcCaseData.setShowSchedule7ActivitiesPage(null);
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(null);
        finalDecisionCaseData.setWriteFinalDecisionDetailsOfDecision(null);
        sscsCaseData.setSupportGroupOnlyAppeal(null);
        sscsUcCaseData.setDoesSchedule8Paragraph4Apply(null);
        sscsUcCaseData.setDoesSchedule9Paragraph4Apply(null);
    }
}
