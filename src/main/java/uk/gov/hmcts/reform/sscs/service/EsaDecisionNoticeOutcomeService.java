package uk.gov.hmcts.reform.sscs.service;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaAllowedOrRefusedCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaPointsCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaPointsRegulationsAndSchedule3ActivitiesCondition;

@Slf4j
@Service
public class EsaDecisionNoticeOutcomeService extends DecisionNoticeOutcomeService {

    private EsaDecisionNoticeQuestionService questionService;

    @Autowired
    public EsaDecisionNoticeOutcomeService(EsaDecisionNoticeQuestionService questionService) {
        super("ESA");
        this.questionService = questionService;
    }
    
    public Outcome determineOutcome(SscsCaseData sscsCaseData) {
        return useExplicitySetOutcome(sscsCaseData);
    }

    @Override
    public void performPreOutcomeIntegrityAdjustments(SscsCaseData sscsCaseData) {
        int totalPoints = questionService.getTotalPoints(sscsCaseData,
            EsaPointsRegulationsAndSchedule3ActivitiesCondition.getAllAnswersExtractor().apply(sscsCaseData));

        if (EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
            sscsCaseData.setDoesRegulation29Apply(null);
            // Ensure that the following values are set correctly, using the intelligent helper methods that
            // check other relevant fields.
            sscsCaseData.setDoesRegulation35Apply(sscsCaseData.getRegulation35Selection());
            sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(sscsCaseData.getSchedule3Selections());
        } else if (EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
            if (YesNo.NO.equals(sscsCaseData.getDoesRegulation29Apply())) {
                sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesApply(null);
                sscsCaseData.setDoesRegulation35Apply(null);
                sscsCaseData.setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(null);
            }
        }

    }

    @Override
    public Outcome determineOutcomeWithValidation(SscsCaseData sscsCaseData) {
        Outcome outcome = determineOutcome(sscsCaseData);
        if (sscsCaseData.isWcaAppeal()) {
            Optional<EsaAllowedOrRefusedCondition> passingAllowedOrRefusedCondition = EsaPointsRegulationsAndSchedule3ActivitiesCondition.getPassingAllowedOrRefusedCondition(questionService, sscsCaseData);
            if (passingAllowedOrRefusedCondition.isEmpty()) {
                throw new IllegalStateException("No matching allowed or refused condition");
            }
        }
        return outcome;
    }
}
