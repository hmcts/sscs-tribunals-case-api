package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsUcCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class UcPointsRegulationsAndSchedule7ActivitiesConditionTest {

    @Mock
    private SscsCaseData sscsCaseData;

    @Mock
    private DecisionNoticeQuestionService questionService;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @NamedParameters("wcaAppealAndScheduleAndRegulationQuestionCombinations")
    @SuppressWarnings("unused")
    private Object[] wcaAppealAndScheduleAndRegulationQuestionCombinations() {
        return new Object[] {
            new Boolean[] {false, null, null, null},
            new Boolean[] {false, null, null, false},
            new Boolean[] {false, null, null, true},
            new Boolean[] {false, null, false, null},
            new Boolean[] {false, null, false, false},
            new Boolean[] {false, null, false, true},
            new Boolean[] {false, null, true, null},
            new Boolean[] {false, null, true, false},
            new Boolean[] {false, null, true, true},
            new Boolean[] {false, false, null, null},
            new Boolean[] {false, false, null, false},
            new Boolean[] {false, false, null, true},
            new Boolean[] {false, false, false, null},
            new Boolean[] {false, false, false, false},
            new Boolean[] {false, false, false, true},
            new Boolean[] {false, false, true, null},
            new Boolean[] {false, false, true, false},
            new Boolean[] {false, false, true, true},
            new Boolean[] {false, true, null, null},
            new Boolean[] {false, true, null, false},
            new Boolean[] {false, true, null, true},
            new Boolean[] {false, true, false, null},
            new Boolean[] {false, true, false, false},
            new Boolean[] {false, true, false, true},
            new Boolean[] {false, true, true, null},
            new Boolean[] {false, true, true, false},
            new Boolean[] {false, true, true, true},

            new Boolean[] {true, null, null, null},
            new Boolean[] {true, null, null, false},
            new Boolean[] {true, null, null, true},
            new Boolean[] {true, null, false, null},
            new Boolean[] {true, null, false, false},
            new Boolean[] {true, null, false, true},
            new Boolean[] {true, null, true, null},
            new Boolean[] {true, null, true, false},
            new Boolean[] {true, null, true, true},
            new Boolean[] {true, false, null, null},
            new Boolean[] {true, false, null, false},
            new Boolean[] {true, false, null, true},
            new Boolean[] {true, false, false, null},
            new Boolean[] {true, false, false, false},
            new Boolean[] {true, false, false, true},
            new Boolean[] {true, false, true, null},
            new Boolean[] {true, false, true, false},
            new Boolean[] {true, false, true, true},
            new Boolean[] {true, true, null, null},
            new Boolean[] {true, true, null, false},
            new Boolean[] {true, true, null, true},
            new Boolean[] {true, true, false, null},
            new Boolean[] {true, true, false, false},
            new Boolean[] {true, true, false, true},
            new Boolean[] {true, true, true, null},
            new Boolean[] {true, true, true, false},
            new Boolean[] {true, true, true, true},

        };
    }

    private boolean isValidCombinationFromSelectSchedule7ActivitiesOnwards(Boolean schedule7ActivitiesSelected,
        Boolean doesSchedule9Paragraph4Apply) {
        if (schedule7ActivitiesSelected == null) {
            return false;
        } else {
            return schedule7ActivitiesSelected.booleanValue() || doesSchedule9Paragraph4Apply != null;
        }
    }

    /**
     * FIXME with UC logic
     * Valid allowed/refused combinations described by the following logic after discussion with PO.
     * If you select allowed, then 15 points or more points awarded. If less than 15 points, then reg 29 needs to apply.
     * If you select refused less than 15 points awarded & reg 29 doesn’t apply.
     * There are also appeals when the App is already in the WRAG and appeals because they want to be in the Support Group.
     * If allowed then at least one schedule 3 activity selected. If no Sch 3 activity then Reg 35 needs to apply.
     * If refused no sch 3 activity and Reg 35 doesn’t apply.
     *
     */
    private boolean isValidAllowedOrRefusedCombinationExpected(int points, Boolean wcaAppeal, Boolean doesSchedule8Paragraph4Apply, Boolean schedule7ActivitiesSelected,
        Boolean doesSchedule9Paragraph4Apply, boolean allowed, boolean supportGroupOnly) {
        if (!wcaAppeal.booleanValue()) {
            return true;
        }
        if (allowed && !supportGroupOnly) {
            if (points >= 15) {
                return true;
            } else {
                return doesSchedule8Paragraph4Apply != null && doesSchedule8Paragraph4Apply.booleanValue();
            }
        } else if (allowed && supportGroupOnly) {
            if (schedule7ActivitiesSelected != null && schedule7ActivitiesSelected.booleanValue()) {
                return true;
            } else {
                if (schedule7ActivitiesSelected != null && !schedule7ActivitiesSelected.booleanValue()) {
                    return doesSchedule9Paragraph4Apply != null && doesSchedule9Paragraph4Apply.booleanValue();
                }
            }
        } else if (!allowed && !supportGroupOnly) {
            return points < 15 && doesSchedule8Paragraph4Apply != null && !doesSchedule8Paragraph4Apply.booleanValue();
        } else if (!allowed && supportGroupOnly) {
            if (schedule7ActivitiesSelected != null && !schedule7ActivitiesSelected.booleanValue() && doesSchedule9Paragraph4Apply != null && !doesSchedule9Paragraph4Apply.booleanValue()) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidCombinationExpected(int points, Boolean wcaAppeal, Boolean doesRegulation29Apply, Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply) {

        if (!wcaAppeal.booleanValue()) {
            return true;
        }
        if (points < 15) {
            if (doesRegulation29Apply == null) {
                return false;
            } else {
                if (!doesRegulation29Apply.booleanValue()) {
                    if (schedule3ActivitiesSelected != null) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return isValidCombinationFromSelectSchedule7ActivitiesOnwards(schedule3ActivitiesSelected, doesRegulation35Apply);
                }
            }
        } else {
            if (doesRegulation29Apply != null) {
                return false;
            }
            return isValidCombinationFromSelectSchedule7ActivitiesOnwards(schedule3ActivitiesSelected, doesRegulation35Apply);
        }
    }

    private YesNo getYesNoFieldValue(Boolean value) {
        return value == null ? null : (value.booleanValue() ? YesNo.YES : YesNo.NO);
    }

    /**
     * Test the continuity of boundaries between point ranges and regulation and schedule answers. (ie. this test will fail if there are any gaps, or overlap between the boundaries)
     */
    @Test
    @Parameters(named = "wcaAppealAndScheduleAndRegulationQuestionCombinations")
    public void testThatAtExactlyOneConditionIsApplicableForAllPointsAndActivityCombinations(
        Boolean wcaAppeal,
        Boolean doeSchedule8Paragraph4Apply, Boolean schedule7ActivitiesSelected,
        Boolean doeSchedule9Paragraph4Apply) {

        int minPointsValue = 0;
        int maxPointsValue = 0;

        if (wcaAppeal != null && wcaAppeal.booleanValue()) {
            minPointsValue = 14;
            maxPointsValue = 15;
        }

        for (boolean supportGroupOnly : Arrays.asList(false, true)) {

            for (boolean allowed : Arrays.asList(false, true)) {

                for (int points = minPointsValue; points <= maxPointsValue; points++) {

                    int conditionApplicableCount = 0;

                    final boolean isValidCombinationExpected =
                        isValidCombinationExpected(points, wcaAppeal, doeSchedule8Paragraph4Apply, schedule7ActivitiesSelected,
                            doeSchedule9Paragraph4Apply);

                    List<String> schedule7Activities = null;
                    String schedule7ActivitesApply = null;
                    if (schedule7ActivitiesSelected != null) {
                        schedule7Activities = schedule7ActivitiesSelected.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>();
                        schedule7ActivitesApply = schedule7ActivitiesSelected.booleanValue() ? "Yes" : "No";
                    } else {
                        schedule7ActivitesApply = null;
                    }

                    SscsCaseData caseData = null;

                    if (wcaAppeal.booleanValue()) {

                        caseData = SscsCaseData.builder()
                            .wcaAppeal("Yes")
                            .writeFinalDecisionGenerateNotice("Yes")
                            .supportGroupOnlyAppeal(supportGroupOnly ? "Yes" : "No")
                            .writeFinalDecisionAllowedOrRefused(allowed ? "allowed" : "refused")
                            .doesSchedule8Paragraph4Apply(getYesNoFieldValue(doeSchedule8Paragraph4Apply))
                            .doesSchedule9Paragraph4Apply(getYesNoFieldValue(doeSchedule9Paragraph4Apply))
                            .sscsUcCaseData(
                                SscsUcCaseData.builder().dwpReassessTheAward(!wcaAppeal.booleanValue() ? "something" : null).ucWriteFinalDecisionSchedule7ActivitiesApply(schedule7ActivitesApply)
                                    .ucWriteFinalDecisionSchedule7ActivitiesQuestion(schedule7Activities).build()).build();
                    } else {
                        caseData = SscsCaseData.builder()
                            .wcaAppeal("No")
                            .writeFinalDecisionGenerateNotice("Yes")
                            .supportGroupOnlyAppeal(supportGroupOnly ? "Yes" : "No")
                            .writeFinalDecisionAllowedOrRefused(allowed ? "allowed" : "refused")
                            .sscsUcCaseData(
                                SscsUcCaseData.builder().dwpReassessTheAward("something").ucWriteFinalDecisionSchedule7ActivitiesApply(schedule7ActivitesApply)
                                    .ucWriteFinalDecisionSchedule7ActivitiesQuestion(schedule7Activities).build()).build();
                    }

                    Mockito.when(questionService.getTotalPoints(Mockito.eq(caseData), Mockito.any())).thenReturn(points);

                    UcPointsRegulationsAndSchedule7ActivitiesCondition matchingCondition = null;

                    for (UcPointsRegulationsAndSchedule7ActivitiesCondition esaPointsCondition : UcPointsRegulationsAndSchedule7ActivitiesCondition.values()) {

                        if (esaPointsCondition.isApplicable(questionService, caseData)) {
                            conditionApplicableCount++;
                            matchingCondition = esaPointsCondition;
                        }
                    }

                    Assert.assertEquals(
                        "Expected 1 condition to be satisfied for points:" + points + ":" + wcaAppeal + ":" + doeSchedule8Paragraph4Apply + ":" + schedule7ActivitiesSelected + ":" + doeSchedule9Paragraph4Apply + " but "
                            + conditionApplicableCount + " were satisfied",
                        1, conditionApplicableCount);

                    if (isValidCombinationExpected) {

                        Assert.assertTrue("Unexpected error for:" + points + ":" + wcaAppeal + ":" + doeSchedule8Paragraph4Apply
                            + ":" + schedule7ActivitiesSelected + ":" + doeSchedule8Paragraph4Apply, matchingCondition
                            .getOptionalErrorMessage(questionService, caseData).isEmpty());
                    } else {
                        Assert.assertTrue("Expected an error for:" + points + ":" + wcaAppeal + ":" + doeSchedule8Paragraph4Apply
                            + ":" + schedule7ActivitiesSelected + ":" + doeSchedule9Paragraph4Apply, matchingCondition
                            .getOptionalErrorMessage(questionService, caseData).isPresent());
                    }

                    if (matchingCondition != null && matchingCondition
                        .getOptionalErrorMessage(questionService, caseData).isEmpty()) {
                        // If we have a matching condition and the errors are null, now check the allowed/refused conditions
                        int allowedOrRefusedConditionApplicableCount = 0;

                        final boolean isValidAllowedOrRefusedCombinationExpected =
                            isValidAllowedOrRefusedCombinationExpected(points, wcaAppeal, doeSchedule8Paragraph4Apply, schedule7ActivitiesSelected,
                                doeSchedule9Paragraph4Apply, allowed, supportGroupOnly);

                        UcAllowedOrRefusedCondition matchingAllowedOrRefusedCondition = null;
                        for (UcAllowedOrRefusedCondition esaPointsCondition : UcAllowedOrRefusedCondition.values()) {

                            if (esaPointsCondition.isApplicable(questionService, caseData)) {
                                allowedOrRefusedConditionApplicableCount++;
                                matchingAllowedOrRefusedCondition = esaPointsCondition;
                            }
                        }

                        Assert.assertEquals(
                            "Expected 1 allowed or refused condition to be satisfied for points:" + points + ":" + wcaAppeal + ":" + doeSchedule8Paragraph4Apply + ":" + schedule7ActivitiesSelected + ":" + doeSchedule9Paragraph4Apply + ":" + allowed
                                + ":" + supportGroupOnly + " but "
                                + allowedOrRefusedConditionApplicableCount + " were satisfied",
                            1, allowedOrRefusedConditionApplicableCount);

                        if (isValidAllowedOrRefusedCombinationExpected) {


                            Assert.assertTrue("Unexpected error for:" + points + ":" + wcaAppeal + ":" + doeSchedule8Paragraph4Apply
                                + ":" + schedule7ActivitiesSelected + ":" + doeSchedule9Paragraph4Apply, matchingAllowedOrRefusedCondition
                                .getOptionalErrorMessage(questionService, caseData).isEmpty());


                            // Assert that for a valid allowed/refused condition, we can always retrieve a single scenario
                            // which we will use to generate content
                            UcScenario scenario = matchingAllowedOrRefusedCondition.getUcScenario(caseData);
                            Assert.assertNotNull(scenario);

                        } else {
                            Assert.assertTrue("Expected an error for:" + points + ":" + wcaAppeal + ":" + doeSchedule8Paragraph4Apply
                                + ":" + schedule7ActivitiesSelected + ":" + doeSchedule9Paragraph4Apply, matchingAllowedOrRefusedCondition
                                .getOptionalErrorMessage(questionService, caseData).isPresent());
                        }

                    }

                }

            }
        }
    }

    @Test
    public void testAllPointsConditionAttributesAreNotNull() {
        for (UcPointsRegulationsAndSchedule7ActivitiesCondition esaPointsCondition : UcPointsRegulationsAndSchedule7ActivitiesCondition.values()) {
            Assert.assertNotNull(esaPointsCondition.getAnswersExtractor());
            Assert.assertSame(esaPointsCondition.getAnswersExtractor(), UcPointsRegulationsAndSchedule7ActivitiesCondition.getAllAnswersExtractor());
            Assert.assertNotNull(esaPointsCondition.getEnumClass());
            Assert.assertEquals(UcPointsRegulationsAndSchedule7ActivitiesCondition.class, esaPointsCondition.getEnumClass());
            Assert.assertNotNull(esaPointsCondition.getPointsRequirementCondition());
        }
    }
}
