package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

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
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@AllArgsConstructor
public class WriteFinalDecisionAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Value("${feature.postHearings.enabled}")
    private boolean isPostHearingsEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_START
                && callback.getEvent() == EventType.WRITE_FINAL_DECISION
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        SscsUtil.setCorrectionInProgress(caseDetails, isPostHearingsEnabled);

        if (!isPostHearingsEnabled
                || !isYes(sscsCaseData.getPostHearing().getCorrection().getCorrectionFinalDecisionInProgress())) {
            clearTransientFields(sscsCaseData);
        }

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void clearTransientFields(SscsCaseData sscsCaseData) {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionTypeOfHearing(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPresentingOfficerAttendedQuestion(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAppellantAttendedQuestion(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAppointeeAttendedQuestion(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDisabilityQualifiedPanelMemberName(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionMedicallyQualifiedPanelMemberName(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionStartDate(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDate(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(null);
        sscsCaseData.setWcaAppeal(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setOtherPartyAttendedQuestions(new ArrayList<>());

        //PIP
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionTakingNutritionQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionManagingTherapyQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionWashAndBatheQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionManagingToiletNeedsQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDressingAndUndressingQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionCommunicatingQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionReadingUnderstandingQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionEngagingWithOthersQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionBudgetingDecisionsQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPlanningAndFollowingQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionReasons(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPageSectionReference(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGeneratedDate(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAnythingElse(null);

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
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDetailsOfDecision(null);
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
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDetailsOfDecision(null);
        sscsCaseData.setSupportGroupOnlyAppeal(null);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(null);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(null);
    }
}
