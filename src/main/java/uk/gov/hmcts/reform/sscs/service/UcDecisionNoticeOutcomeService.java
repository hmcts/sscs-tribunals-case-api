package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsUcCaseData;
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
        if (isYes(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {

            SscsUcCaseData ucCaseData = sscsCaseData.getSscsUcCaseData();

            if (sscsCaseData.isWcaAppeal()) {

                if (sscsCaseData.isSupportGroupOnlyAppeal()) {
                    ucCaseData.setDoesSchedule8Paragraph4Apply(null);
                    sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
                    sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMentalAssessmentQuestion(null);
                    // Ensure that we set the following fields taking into account the radio button
                    // for whether schedule 7 activities apply.   The getSchedule9Paragraph4Selection and
                    // getSchedule7Selections methods peform this check,  and we use these methods
                    // to set the final values of doesSchedule9Paragraph4Apply and ucWriteFinalDecisionSchedule7ActivitiesQuestion
                    ucCaseData.setDoesSchedule9Paragraph4Apply(ucCaseData.getSchedule9Paragraph4Selection());
                    sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(ucCaseData.getSchedule7Selections());
                } else {
                    int totalPoints = questionService.getTotalPoints(sscsCaseData,
                        UcPointsRegulationsAndSchedule7ActivitiesCondition.getAllAnswersExtractor().apply(sscsCaseData));

                    if (UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
                        ucCaseData.setDoesSchedule8Paragraph4Apply(null);
                        // Ensure that we set the following fields taking into account the radio button
                        // for whether schedule 7 activities apply.   The getSchedule9Paragraph4Selection and
                        // getSchedule7Selections methods peform this check,  and we use these methods
                        // to set the final values of doesSchedule9Paragraph4Apply and ucWriteFinalDecisionSchedule7ActivitiesQuestion
                        ucCaseData.setDoesSchedule9Paragraph4Apply(ucCaseData.getSchedule9Paragraph4Selection());
                        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(ucCaseData.getSchedule7Selections());
                    } else if (UcPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
                        if (YesNo.NO.equals(ucCaseData.getDoesSchedule8Paragraph4Apply())) {
                            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
                            ucCaseData.setDoesSchedule9Paragraph4Apply(null);
                            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(null);
                        }
                    }
                }
                if ("refused".equalsIgnoreCase(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused())) {
                    sscsCaseData.setDwpReassessTheAward(null);
                }
            } else {
                ucCaseData.setDoesSchedule9Paragraph4Apply(null);
                ucCaseData.setDoesSchedule8Paragraph4Apply(null);
                sscsCaseData.setSupportGroupOnlyAppeal(null);
                sscsCaseData.setDwpReassessTheAward(null);
                sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
                sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMentalAssessmentQuestion(null);
                sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
                sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(null);
            }
        }
    }

    @Override
    public Outcome determineOutcomeWithValidation(SscsCaseData sscsCaseData) {
        Outcome outcome = determineOutcome(sscsCaseData);
        if (isYes(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
            Optional<UcAllowedOrRefusedCondition> passingAllowedOrRefusedCondition = UcPointsRegulationsAndSchedule7ActivitiesCondition
                .getPassingAllowedOrRefusedCondition(questionService, sscsCaseData);
            if (passingAllowedOrRefusedCondition.isEmpty()) {
                throw new IllegalStateException("No matching allowed or refused condition");
            }
        }
        return outcome;
    }
}
