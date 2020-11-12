package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios.EsaScenario;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class EsaPointsRegulationsAndSchedule3ActivitiesConditionTest {

    @Mock
    private SscsCaseData sscsCaseData;

    @Mock
    private DecisionNoticeQuestionService questionService;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @NamedParameters("scheduleAndRegulationQuestionCombinations")
    @SuppressWarnings("unused")
    private Object[] scheduleAndRegulationQuestionCombinations() {
        return new Object[] {
            new Boolean[] {null, null, null},
            new Boolean[] {null, null, false},
            new Boolean[] {null, null, true},
            new Boolean[] {null, false, null},
            new Boolean[] {null, false, false},
            new Boolean[] {null, false, true},
            new Boolean[] {null, true, null},
            new Boolean[] {null, true, false},
            new Boolean[] {null, true, true},
            new Boolean[] {false, null, null},
            new Boolean[] {false, null, false},
            new Boolean[] {false, null, true},
            new Boolean[] {false, false, null},
            new Boolean[] {false, false, false},
            new Boolean[] {false, false, true},
            new Boolean[] {false, true, null},
            new Boolean[] {false, true, false},
            new Boolean[] {false, true, true},
            new Boolean[] {true, null, null},
            new Boolean[] {true, null, false},
            new Boolean[] {true, null, true},
            new Boolean[] {true, false, null},
            new Boolean[] {true, false, false},
            new Boolean[] {true, false, true},
            new Boolean[] {true, true, null},
            new Boolean[] {true, true, false},
            new Boolean[] {true, true, true},
        };
    }

    private boolean isValidCombinationFromSelectSchedule3ActivitiesOnwards(Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply) {
        if (schedule3ActivitiesSelected == null) {
            return false;
        } else {
            return schedule3ActivitiesSelected.booleanValue() || doesRegulation35Apply != null;
        }
    }

    /**
     * Valid allowed/refused combinations described by the following logic after discussion with PO.
     * If you select allowed, then 15 points or more points awarded. If less than 15 points, then reg 29 needs to apply.
     * If you select refused less than 15 points awarded & reg 29 doesn’t apply.
     * There are also appeals when the App is already in the WRAG and appeals because they want to be in the Support Group.
     * If allowed then at least one schedule 3 activity selected. If no Sch 3 activity then Reg 35 needs to apply.
     * If refused no sch 3 activity and Reg 35 doesn’t apply.
     *
     */
    private boolean isValidAllowedOrRefusedCombinationExpected(int points, Boolean doesRegulation29Apply, Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply, boolean allowed, boolean supportGroupOnly) {
        if (allowed && !supportGroupOnly) {
            if (points >= 15) {
                return true;
            } else {
                return doesRegulation29Apply != null && doesRegulation29Apply.booleanValue();
            }
        } else if (allowed && supportGroupOnly) {
            if (schedule3ActivitiesSelected != null && schedule3ActivitiesSelected.booleanValue()) {
                return true;
            } else {
                if (schedule3ActivitiesSelected != null && !schedule3ActivitiesSelected.booleanValue()) {
                    return doesRegulation35Apply != null && doesRegulation35Apply.booleanValue();
                }
            }
        } else if (!allowed && !supportGroupOnly) {
            return points < 15 && doesRegulation29Apply != null && !doesRegulation29Apply.booleanValue();
        } else if (!allowed && supportGroupOnly) {
            if (schedule3ActivitiesSelected != null && !schedule3ActivitiesSelected.booleanValue() && doesRegulation35Apply != null && !doesRegulation35Apply.booleanValue()) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidCombinationExpected(int points, Boolean doesRegulation29Apply, Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply) {

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
                    return isValidCombinationFromSelectSchedule3ActivitiesOnwards(schedule3ActivitiesSelected, doesRegulation35Apply);
                }
            }
        } else {
            if (doesRegulation29Apply != null) {
                return false;
            }
            return isValidCombinationFromSelectSchedule3ActivitiesOnwards(schedule3ActivitiesSelected, doesRegulation35Apply);
        }
    }

    private YesNo getYesNoFieldValue(Boolean value) {
        return value == null ? null : (value.booleanValue() ? YesNo.YES : YesNo.NO);
    }

    /**
     * Test the continuity of boundaries between point ranges and regulation and schedule answers. (ie. this test will fail if there are any gaps, or overlap between the boundaries)
     */
    @Test
    @Parameters(named = "scheduleAndRegulationQuestionCombinations")
    public void testThatAtExactlyOneConditionIsApplicableForAllPointsAndActivityCombinations(
        Boolean doesRegulation29Apply, Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply) {

        int minPointsValue = 14;
        int maxPointsValue = 15;

        for (boolean supportGroupOnly : Arrays.asList(false, true)) {

            for (boolean allowed : Arrays.asList(false, true)) {

                for (int points = minPointsValue; points <= maxPointsValue; points++) {

                    int conditionApplicableCount = 0;

                    final boolean isValidCombinationExpected =
                        isValidCombinationExpected(points, doesRegulation29Apply, schedule3ActivitiesSelected,
                            doesRegulation35Apply);

                    List<String> schedule3Activities = null;
                    String schedule3ActivitesApply = null;
                    if (schedule3ActivitiesSelected != null) {
                        schedule3Activities = schedule3ActivitiesSelected.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>();
                        schedule3ActivitesApply = schedule3ActivitiesSelected.booleanValue() ? "Yes" : "No";
                    } else {
                        schedule3ActivitesApply = null;
                    }

                    SscsCaseData caseData = SscsCaseData.builder()
                        .wcaAppeal("Yes")
                        .supportGroupOnlyAppeal(supportGroupOnly ? "Yes" : "No")
                        .writeFinalDecisionAllowedOrRefused(allowed ? "allowed" : "refused")
                        .doesRegulation29Apply(getYesNoFieldValue(doesRegulation29Apply))
                        .doesRegulation35Apply(getYesNoFieldValue(doesRegulation35Apply))
                        .esaSscsCaseData(SscsEsaCaseData.builder().esaWriteFinalDecisionSchedule3ActivitiesApply(schedule3ActivitesApply)
                            .esaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities).build()).build();

                    Mockito.when(questionService.getTotalPoints(Mockito.eq(caseData), Mockito.any())).thenReturn(points);

                    EsaPointsRegulationsAndSchedule3ActivitiesCondition matchingCondition = null;

                    for (EsaPointsRegulationsAndSchedule3ActivitiesCondition esaPointsCondition : EsaPointsRegulationsAndSchedule3ActivitiesCondition.values()) {

                        if (esaPointsCondition.isApplicable(questionService, caseData)) {
                            conditionApplicableCount++;
                            matchingCondition = esaPointsCondition;
                        }
                    }

                    Assert.assertEquals(
                        "Expected 1 condition to be satisfied for points:" + points + ":" + doesRegulation29Apply + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply + " but "
                            + conditionApplicableCount + " were satisfied",
                        1, conditionApplicableCount);

                    if (isValidCombinationExpected) {
                        Assert.assertTrue("Unexpected error for:" + points + ":" + doesRegulation29Apply
                            + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply, matchingCondition
                            .getOptionalErrorMessage(questionService, caseData).isEmpty());
                    } else {
                        Assert.assertTrue("Expected an error for:" + points + ":" + doesRegulation29Apply
                            + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply, matchingCondition
                            .getOptionalErrorMessage(questionService, caseData).isPresent());
                    }

                    if (matchingCondition != null && matchingCondition
                        .getOptionalErrorMessage(questionService, caseData).isEmpty()) {
                        // If we have a matching condition and the errors are null, now check the allowed/refused conditions
                        int allowedOrRefusedConditionApplicableCount = 0;

                        final boolean isValidAllowedOrRefusedCombinationExpected =
                            isValidAllowedOrRefusedCombinationExpected(points, doesRegulation29Apply, schedule3ActivitiesSelected,
                                doesRegulation35Apply, allowed, supportGroupOnly);

                        EsaAllowedOrRefusedCondition matchingAllowedOrRefusedCondition = null;
                        for (EsaAllowedOrRefusedCondition esaPointsCondition : EsaAllowedOrRefusedCondition.values()) {

                            if (esaPointsCondition.isApplicable(questionService, caseData)) {
                                allowedOrRefusedConditionApplicableCount++;
                                matchingAllowedOrRefusedCondition = esaPointsCondition;
                            }
                        }

                        Assert.assertEquals(
                            "Expected 1 condition to be satisfied for points:" + points + ":" + doesRegulation29Apply + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply + ":" + allowed
                                + ":" + supportGroupOnly + " but "
                                + allowedOrRefusedConditionApplicableCount + " were satisfied",
                            1, allowedOrRefusedConditionApplicableCount);

                        if (isValidAllowedOrRefusedCombinationExpected) {
                            Assert.assertTrue("Unexpected error for:" + points + ":" + doesRegulation29Apply
                                + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply, matchingAllowedOrRefusedCondition
                                .getOptionalErrorMessage(questionService, caseData).isEmpty());


                            // Assert that for a valid allowed/refused condition, we can always retrieve a single scenario
                            // which we will use to generate content
                            EsaScenario scenario = matchingAllowedOrRefusedCondition.getEsaScenario(caseData);
                            Assert.assertNotNull(scenario);

                        } else {
                            Assert.assertTrue("Expected an error for:" + points + ":" + doesRegulation29Apply
                                + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply, matchingAllowedOrRefusedCondition
                                .getOptionalErrorMessage(questionService, caseData).isPresent());
                        }

                    }

                }

            }
        }
    }


    /**
     * We have separate tests above to ensure that only a single PointsCondition exists for all valid activity/points combinations - this method returns that condition, or throws an exception if no
     * matching condition.
     */
    /*
    private EsaPointsAndActivitiesCondition getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(
        Boolean doesRegulation29Apply, Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply, int points) {
        for (EsaPointsAndActivitiesCondition esaPointsAndActivitiesCondition : EsaPointsAndActivitiesCondition.values()) {

            if (esaPointsAndActivitiesCondition.isSatisfied(points, doesRegulation29Apply, schedule3ActivitiesSelected,
                doesRegulation35Apply)) {
                if (esaPointsAndActivitiesCondition.getPointsCondition().getPointsRequirementCondition().test(points)) {
                    return esaPointsAndActivitiesCondition;
                }
            }
        }
        throw new IllegalStateException("No points condition found for points:" + points + " and " + doesRegulation29Apply + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply);
    }

     */
    @Test
    public void testAllPointsConditionAttributesAreNotNull() {
        for (EsaPointsRegulationsAndSchedule3ActivitiesCondition esaPointsCondition : EsaPointsRegulationsAndSchedule3ActivitiesCondition.values()) {
            Assert.assertNotNull(esaPointsCondition.getAnswersExtractor());
            Assert.assertSame(esaPointsCondition.getAnswersExtractor(), EsaPointsRegulationsAndSchedule3ActivitiesCondition.getAllAnswersExtractor());
            Assert.assertNotNull(esaPointsCondition.getEnumClass());
            Assert.assertEquals(EsaPointsRegulationsAndSchedule3ActivitiesCondition.class, esaPointsCondition.getEnumClass());
            Assert.assertNotNull(esaPointsCondition.getPointsRequirementCondition());
        }
    }
}
