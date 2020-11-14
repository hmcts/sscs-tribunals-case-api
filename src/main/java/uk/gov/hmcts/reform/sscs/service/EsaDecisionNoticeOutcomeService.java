package uk.gov.hmcts.reform.sscs.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.PointsCondition;
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
        int totalPoints = questionService.getTotalPoints(sscsCaseData,
            EsaPointsRegulationsAndSchedule3ActivitiesCondition.getAllAnswersExtractor().apply(sscsCaseData));

        if (EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
            sscsCaseData.setDoesRegulation29Apply(null);
            // Ensure that we set the following fields taking into account the radio button
            // for whether schedule 3 activities apply.   The getRegulation35Selection and
            // getSchedule3Selections methods peform this check,  and we use these methods
            // to set the final values of doesRegulation35Apply and esaWriteFinalDecisionSchedule3ActivitiesQuestion
            sscsCaseData.setDoesRegulation35Apply(sscsCaseData.getRegulation35Selection());
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(sscsCaseData.getSchedule3Selections());
        } else if (EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
            if (YesNo.NO.equals(sscsCaseData.getDoesRegulation29Apply())) {
                sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(null);
                sscsCaseData.setDoesRegulation35Apply(null);
                sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(null);
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
