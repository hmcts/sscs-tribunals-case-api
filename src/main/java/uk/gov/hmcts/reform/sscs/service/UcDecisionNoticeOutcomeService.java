package uk.gov.hmcts.reform.sscs.service;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcAllowedOrRefusedCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcPointsCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcPointsRegulationsAndSchedule7ActivitiesCondition;

@Slf4j
@Service
public class UcDecisionNoticeOutcomeService extends DecisionNoticeOutcomeService {

    @Autowired
    public UcDecisionNoticeOutcomeService(UcDecisionNoticeQuestionService questionService) {
        super("UC", questionService);
    }
    
    public Outcome determineOutcome(SscsCaseData sscsCaseData) {
        return useExplicitySetOutcome(sscsCaseData);
    }

    @Override
    public void performPreOutcomeIntegrityAdjustments(SscsCaseData sscsCaseData) {

        if ("Yes".equalsIgnoreCase(sscsCaseData.getWriteFinalDecisionGenerateNotice())) {

            if (sscsCaseData.isWcaAppeal()) {

                int totalPoints = questionService.getTotalPoints(sscsCaseData,
                    UcPointsRegulationsAndSchedule7ActivitiesCondition.getAllAnswersExtractor().apply(sscsCaseData));

                if (UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
                    sscsCaseData.setDoesSchedule8Paragraph4Apply(null);
                    // Ensure that we set the following fields taking into account the radio button
                    // for whether schedule 7 activities apply.   The getRegulation35Selection and
                    // getSchedule3Selections methods peform this check,  and we use these methods
                    // to set the final values of doesRegulation35Apply and esaWriteFinalDecisionSchedule3ActivitiesQuestion
                    sscsCaseData.setDoesSchedule9Paragraph4Apply(sscsCaseData.getSchedule9Paragraph4Selection());
                    sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(sscsCaseData.getSchedule7Selections());
                } else if (UcPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
                    if (YesNo.NO.equals(sscsCaseData.getDoesSchedule8Paragraph4Apply())) {
                        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
                        sscsCaseData.setDoesSchedule9Paragraph4Apply(null);
                        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(null);
                    }
                }
            } else {
                sscsCaseData.setDoesSchedule9Paragraph4Apply(null);
                sscsCaseData.setDoesSchedule8Paragraph4Apply(null);
                sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
                sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(null);
            }
        }
    }

    @Override
    public Outcome determineOutcomeWithValidation(SscsCaseData sscsCaseData) {
        Outcome outcome = determineOutcome(sscsCaseData);
        if ("Yes".equalsIgnoreCase(sscsCaseData.getWriteFinalDecisionGenerateNotice())) {
            Optional<UcAllowedOrRefusedCondition> passingAllowedOrRefusedCondition = UcPointsRegulationsAndSchedule7ActivitiesCondition
                .getPassingAllowedOrRefusedCondition(questionService, sscsCaseData);
            if (passingAllowedOrRefusedCondition.isEmpty()) {
                throw new IllegalStateException("No matching allowed or refused condition");
            }
        }
        return outcome;
    }
}
