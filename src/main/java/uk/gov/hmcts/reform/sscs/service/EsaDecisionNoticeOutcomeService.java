package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaAllowedOrRefusedCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaPointsCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaPointsRegulationsAndSchedule3ActivitiesCondition;

@Slf4j
@Service
public class EsaDecisionNoticeOutcomeService extends DecisionNoticeOutcomeService {

    @Autowired
    public EsaDecisionNoticeOutcomeService(EsaDecisionNoticeQuestionService questionService) {
        super("ESA", questionService);
    }

    public Outcome determineOutcome(SscsCaseData sscsCaseData) {
        return useExplicitySetOutcome(sscsCaseData);
    }

    @Override
    public void performPreOutcomeIntegrityAdjustments(SscsCaseData sscsCaseData) {

        if (isYes(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
            SscsEsaCaseData esaCaseData = sscsCaseData.getSscsEsaCaseData();

            if (sscsCaseData.isWcaAppeal()) {
                if (sscsCaseData.isSupportGroupOnlyAppeal()) {
                    esaCaseData.setDoesRegulation29Apply(null);
                    sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
                    sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMentalAssessmentQuestion(null);
                    // Ensure that we set the following fields taking into account the radio button
                    // for whether schedule 3 activities apply.   The getRegulation35Selection and
                    // getSchedule3Selections methods peform this check,  and we use these methods
                    // to set the final values of doesRegulation35Apply and esaWriteFinalDecisionSchedule3ActivitiesQuestion
                    esaCaseData.setDoesRegulation35Apply(esaCaseData.getRegulation35Selection());
                    sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(esaCaseData.getSchedule3Selections());
                } else {
                    int totalPoints = questionService.getTotalPoints(sscsCaseData,
                        EsaPointsRegulationsAndSchedule3ActivitiesCondition.getAllAnswersExtractor().apply(sscsCaseData));

                    if (EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
                        esaCaseData.setDoesRegulation29Apply(null);
                        // Ensure that we set the following fields taking into account the radio button
                        // for whether schedule 3 activities apply.   The getRegulation35Selection and
                        // getSchedule3Selections methods peform this check,  and we use these methods
                        // to set the final values of doesRegulation35Apply and esaWriteFinalDecisionSchedule3ActivitiesQuestion
                        esaCaseData.setDoesRegulation35Apply(esaCaseData.getRegulation35Selection());
                        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(esaCaseData.getSchedule3Selections());
                    } else if (EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
                        if (YesNo.NO.equals(esaCaseData.getDoesRegulation29Apply())) {
                            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(null);
                            esaCaseData.setDoesRegulation35Apply(null);
                            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(null);
                        }
                    }
                }
                if ("refused".equalsIgnoreCase(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused())) {
                    sscsCaseData.setDwpReassessTheAward(null);
                }
            } else {
                esaCaseData.setDoesRegulation35Apply(null);
                esaCaseData.setDoesRegulation29Apply(null);
                sscsCaseData.setSupportGroupOnlyAppeal(null);
                sscsCaseData.setDwpReassessTheAward(null);
                sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
                sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMentalAssessmentQuestion(null);
                sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(null);
                sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(null);
            }
        }
    }

    @Override
    public Outcome determineOutcomeWithValidation(SscsCaseData sscsCaseData) {
        Outcome outcome = determineOutcome(sscsCaseData);
        if (isYes(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
            Optional<EsaAllowedOrRefusedCondition> passingAllowedOrRefusedCondition = EsaPointsRegulationsAndSchedule3ActivitiesCondition.getPassingAllowedOrRefusedCondition(questionService, sscsCaseData);
            if (passingAllowedOrRefusedCondition.isEmpty()) {
                throw new IllegalStateException("No matching allowed or refused condition");
            }
        }
        return outcome;
    }
}
