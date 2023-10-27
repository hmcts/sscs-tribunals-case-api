package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
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
    private boolean isValidAllowedOrRefusedCombinationExpected(int points, Boolean wcaAppeal, Boolean doesRegulation29Apply, Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply, boolean allowed, Boolean supportGroupOnly) {
        if (!wcaAppeal.booleanValue()) {
            return true;
        }
        if (supportGroupOnly == null) {
            return false;
        }
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

    private boolean isValidPointsBasedCombinationExpected(int points, Boolean wcaAppeal, Boolean doesRegulation29Apply, Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply, Boolean supportGroupOnly) {

        // If it's not a wca appeal we don't do any points-based validation - always valid
        if (!wcaAppeal.booleanValue()) {
            return true;
        }
        // For WCA appeals, if points < 15
        if (points < 15) {
            if (doesRegulation29Apply == null) {
                if (supportGroupOnly != null && supportGroupOnly) {
                    return isValidCombinationFromSelectSchedule3ActivitiesOnwards(schedule3ActivitiesSelected, doesRegulation35Apply);
                } else {
                    return false;
                }
            } else {
                if (supportGroupOnly != null && supportGroupOnly) {
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
            }
        } else {
            // For WCA appeals, if points >= 15
            if (supportGroupOnly != null && supportGroupOnly.booleanValue()) {
                // If points >= 15 there must have been an error, because we skip the points pages and we should be
                return false;
            }
            if (doesRegulation29Apply != null) {
                return false;
            }
            return isValidCombinationFromSelectSchedule3ActivitiesOnwards(schedule3ActivitiesSelected, doesRegulation35Apply);
        }
    }

    private YesNo getYesNoFieldValue(Boolean value) {
        return value == null ? null : (value.booleanValue() ? YES : YesNo.NO);
    }

    /**
     * Test the continuity of boundaries between point ranges and regulation and schedule answers. (ie. this test will fail if there are any gaps, or overlap between the boundaries)
     */
    @Test
    @Parameters(named = "wcaAppealAndScheduleAndRegulationQuestionCombinations")
    public void testThatAtExactlyOneConditionIsApplicableForAllPointsAndActivityCombinations(
        Boolean wcaAppeal,
        Boolean doesRegulation29Apply, Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply) {

        int minPointsValue = 0;
        int maxPointsValue = 0;

        if (wcaAppeal != null && wcaAppeal.booleanValue()) {
            minPointsValue = 14;
            maxPointsValue = 15;
        }

        for (Boolean supportGroupOnly : Arrays.asList(null, false, true)) {

            for (boolean allowed : Arrays.asList(false, true)) {

                for (int points = minPointsValue; points <= maxPointsValue; points++) {

                    int conditionApplicableCount = 0;

                    final boolean isValidCombinationExpected =
                        isValidPointsBasedCombinationExpected(points, wcaAppeal, doesRegulation29Apply, schedule3ActivitiesSelected,
                            doesRegulation35Apply, supportGroupOnly);

                    List<String> schedule3Activities = null;
                    String schedule3ActivitesApply = null;
                    if (schedule3ActivitiesSelected != null) {
                        schedule3Activities = schedule3ActivitiesSelected.booleanValue() ? Arrays.asList("someActivity") : new ArrayList<>();
                        schedule3ActivitesApply = schedule3ActivitiesSelected.booleanValue() ? "Yes" : "No";
                    } else {
                        schedule3ActivitesApply = null;
                    }

                    SscsCaseData caseData = null;

                    if (wcaAppeal.booleanValue()) {

                        caseData = SscsCaseData.builder()
                            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                                .writeFinalDecisionGenerateNotice(YES)
                                .writeFinalDecisionAllowedOrRefused(allowed ? "allowed" : "refused")
                                .build())
                            .supportGroupOnlyAppeal(supportGroupOnly == null ? null : supportGroupOnly ? "Yes" : "No")
                            .dwpReassessTheAward(null)
                            .wcaAppeal(YES)
                            .sscsEsaCaseData(
                                SscsEsaCaseData.builder().esaWriteFinalDecisionSchedule3ActivitiesApply(schedule3ActivitesApply)
                                    .esaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities)
                                    .doesRegulation29Apply(getYesNoFieldValue(doesRegulation29Apply))
                                    .doesRegulation35Apply(getYesNoFieldValue(doesRegulation35Apply)).build()).build();
                    } else {
                        caseData = SscsCaseData.builder()
                            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                                .writeFinalDecisionGenerateNotice(YES)
                                .writeFinalDecisionAllowedOrRefused(allowed ? "allowed" : "refused")
                                .build())
                            .supportGroupOnlyAppeal(supportGroupOnly == null ? null : supportGroupOnly ? "Yes" : "No")
                            .wcaAppeal(NO)
                            .sscsEsaCaseData(
                                SscsEsaCaseData.builder().esaWriteFinalDecisionSchedule3ActivitiesApply(schedule3ActivitesApply)
                                    .esaWriteFinalDecisionSchedule3ActivitiesQuestion(schedule3Activities).build()).build();
                    }

                    Mockito.when(questionService.getTotalPoints(Mockito.eq(caseData), Mockito.any())).thenReturn(points);

                    EsaPointsRegulationsAndSchedule3ActivitiesCondition matchingCondition = null;

                    for (EsaPointsRegulationsAndSchedule3ActivitiesCondition esaPointsCondition : EsaPointsRegulationsAndSchedule3ActivitiesCondition.values()) {

                        if (esaPointsCondition.isApplicable(questionService, caseData)) {
                            conditionApplicableCount++;
                            matchingCondition = esaPointsCondition;
                        }
                    }

                    Assert.assertEquals(
                        "Expected 1 condition to be satisfied for points:" + points + ":" + wcaAppeal + ":" + doesRegulation29Apply + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply + ":" + supportGroupOnly +  " but "
                            + conditionApplicableCount + " were satisfied",
                        1, conditionApplicableCount);

                    if (isValidCombinationExpected) {

                        Assert.assertTrue("Unexpected error for:" + points + ":" + wcaAppeal + ":" + doesRegulation29Apply
                            + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply + ":" + supportGroupOnly, matchingCondition
                            .getOptionalErrorMessage(questionService, caseData).isEmpty());
                    } else {
                        Assert.assertTrue("Expected an error for:" + points + ":" + wcaAppeal + ":" + doesRegulation29Apply
                            + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply + ":" + supportGroupOnly, matchingCondition
                            .getOptionalErrorMessage(questionService, caseData).isPresent());
                    }

                    if (matchingCondition != null && matchingCondition
                        .getOptionalErrorMessage(questionService, caseData).isEmpty()) {
                        // If we have a matching condition and the errors are null, now check the allowed/refused conditions
                        int allowedOrRefusedConditionApplicableCount = 0;

                        final boolean isValidAllowedOrRefusedCombinationExpected =
                            isValidAllowedOrRefusedCombinationExpected(points, wcaAppeal, doesRegulation29Apply, schedule3ActivitiesSelected,
                                doesRegulation35Apply, allowed, supportGroupOnly);

                        EsaAllowedOrRefusedCondition matchingAllowedOrRefusedCondition = null;
                        for (EsaAllowedOrRefusedCondition esaPointsCondition : EsaAllowedOrRefusedCondition.values()) {

                            if (esaPointsCondition.isApplicable(questionService, caseData)) {
                                allowedOrRefusedConditionApplicableCount++;
                                matchingAllowedOrRefusedCondition = esaPointsCondition;
                            }
                        }

                        Assert.assertEquals(
                            "Expected 1 allowed or refused condition to be satisfied for points:" + points + ":" + wcaAppeal + ":" + doesRegulation29Apply + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply + ":" + allowed
                                + ":" + supportGroupOnly + " but "
                                + allowedOrRefusedConditionApplicableCount + " were satisfied",
                            1, allowedOrRefusedConditionApplicableCount);

                        if (isValidAllowedOrRefusedCombinationExpected) {


                            Assert.assertTrue("Unexpected error for:" + points + ":" + wcaAppeal + ":" + doesRegulation29Apply
                                + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply, matchingAllowedOrRefusedCondition
                                .getOptionalErrorMessage(questionService, caseData).isEmpty());


                            // Assert that for a valid allowed/refused condition, we can always retrieve a single scenario
                            // which we will use to generate content
                            EsaScenario scenario = matchingAllowedOrRefusedCondition.getEsaScenario(caseData);
                            Assert.assertNotNull(scenario);

                        } else {
                            Assert.assertTrue("Expected an error for:" + points + ":" + wcaAppeal + ":" + doesRegulation29Apply
                                + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply, matchingAllowedOrRefusedCondition
                                .getOptionalErrorMessage(questionService, caseData).isPresent());
                        }

                    }

                }

            }
        }
    }

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
