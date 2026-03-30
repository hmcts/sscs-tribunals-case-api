package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcPointsRegulationsAndSchedule7ActivitiesCondition.HIGH_POINTS_SCHEDULE_9_PARAGRAPH_4_UNSPECIFIED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcPointsRegulationsAndSchedule7ActivitiesCondition.LOW_POINTS_SCHEDULE6_AND_REG_29_SKIPPED_SCHEDULE_9_PARAGRAPH_4_UNSPECIFIED_SUPPORT_GROUP_ONLY;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcPointsRegulationsAndSchedule7ActivitiesCondition.SEVERE_CONDITIONS_WITH_SV_ISSUE_CODE;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.ExtendedSscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
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
            new Boolean[] {false, null, null, null, false},
            new Boolean[] {false, null, null, false, false},
            new Boolean[] {false, null, null, true, false},
            new Boolean[] {false, null, false, null, false},
            new Boolean[] {false, null, false, false, false},
            new Boolean[] {false, null, false, true, false},
            new Boolean[] {false, null, true, null, false},
            new Boolean[] {false, null, true, false, false},
            new Boolean[] {false, null, true, true, false},
            new Boolean[] {false, false, null, null, false},
            new Boolean[] {false, false, null, false, false},
            new Boolean[] {false, false, null, true, false},
            new Boolean[] {false, false, false, null, false},
            new Boolean[] {false, false, false, false, false},
            new Boolean[] {false, false, false, true, false},
            new Boolean[] {false, false, true, null, false},
            new Boolean[] {false, false, true, false, false},
            new Boolean[] {false, false, true, true, false},
            new Boolean[] {false, true, null, null, false},
            new Boolean[] {false, true, null, false, false},
            new Boolean[] {false, true, null, true, false},
            new Boolean[] {false, true, false, null, false},
            new Boolean[] {false, true, false, false, false},
            new Boolean[] {false, true, false, true, false},
            new Boolean[] {false, true, true, null, false},
            new Boolean[] {false, true, true, false, false},
            new Boolean[] {false, true, true, true, false},

            new Boolean[] {true, null, null, null, false},
            new Boolean[] {true, null, null, false, false},
            new Boolean[] {true, null, null, true, false},
            new Boolean[] {true, null, false, null, false},
            new Boolean[] {true, null, false, false, false},
            new Boolean[] {true, null, false, true, false},
            new Boolean[] {true, null, true, null, false},
            new Boolean[] {true, null, true, false, false},
            new Boolean[] {true, null, true, true, false},
            new Boolean[] {true, false, null, null, false},
            new Boolean[] {true, false, null, false, false},
            new Boolean[] {true, false, null, true, false},
            new Boolean[] {true, false, false, null, false},
            new Boolean[] {true, false, false, false, false},
            new Boolean[] {true, false, false, true, false},
            new Boolean[] {true, false, true, null, false},
            new Boolean[] {true, false, true, false, false},
            new Boolean[] {true, false, true, true, false},
            new Boolean[] {true, true, null, null, false},
            new Boolean[] {true, true, null, false, false},
            new Boolean[] {true, true, null, true, false},
            new Boolean[] {true, true, false, null, false},
            new Boolean[] {true, true, false, false, false},
            new Boolean[] {true, true, false, true, false},
            new Boolean[] {true, true, true, null, false},
            new Boolean[] {true, true, true, false, false},
            new Boolean[] {true, true, true, true, false},
            new Boolean[] {true, null, null, null, true}
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
     * Valid allowed/refused combinations described by the following logic after discussion with PO.
     * If you select allowed, then 15 points or more points awarded. If less than 15 points, then reg 29 needs to apply.
     * If you select refused less than 15 points awarded & reg 29 doesn’t apply.
     * There are also appeals when the App is already in the WRAG and appeals because they want to be in the Support Group.
     * If allowed then at least one schedule 3 activity selected. If no Sch 3 activity then Reg 35 needs to apply.
     * If refused no sch 3 activity and Reg 35 doesn’t apply.
     *
     */
    private boolean isValidAllowedOrRefusedCombinationExpected(int points, Boolean wcaAppeal, Boolean isSevereCondition, Boolean doesSchedule8Paragraph4Apply, Boolean schedule7ActivitiesSelected,
        Boolean doesSchedule9Paragraph4Apply, boolean allowed, Boolean supportGroupOnly) {
        if (!wcaAppeal.booleanValue()) {
            return true;
        }
        if (supportGroupOnly == null) {
            return false;
        }
        if (isSevereCondition && !isTrue(supportGroupOnly)) {
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

    private boolean isValidPointsBasedCombinationExpected(int points, Boolean wcaAppeal, Boolean isSevereCondition, Boolean doesSchedule8Paragraph4Apply, Boolean schedule7ActivitiesSelected,
        Boolean doesSchedule9Paragraph4, Boolean supportGroupOnly) {

        // If it's not a wca appeal we don't do any points-based validation - always valid
        if (!wcaAppeal.booleanValue()) {
            return true;
        }
        // For WCA appeals, if severe condition only appeal, then valid regardless of points or schedule/regulation answers
        if (wcaAppeal.booleanValue() && isSevereCondition.booleanValue() && !isTrue(supportGroupOnly)) {
            return true;
        }
        // For WCA appeals, if points < 15
        if (points < 15) {
            if (doesSchedule8Paragraph4Apply == null) {
                if (supportGroupOnly != null && supportGroupOnly) {
                    return isValidCombinationFromSelectSchedule7ActivitiesOnwards(schedule7ActivitiesSelected, doesSchedule9Paragraph4);
                } else {
                    return false;
                }
            } else {
                if (supportGroupOnly != null && supportGroupOnly) {
                    return false;
                } else {
                    if (!doesSchedule8Paragraph4Apply.booleanValue()) {
                        if (schedule7ActivitiesSelected != null) {
                            return false;
                        } else {
                            return true;
                        }
                    } else {
                        return isValidCombinationFromSelectSchedule7ActivitiesOnwards(schedule7ActivitiesSelected, doesSchedule9Paragraph4);
                    }
                }
            }
        } else {
            // For WCA appeals, if points >= 15
            if (supportGroupOnly != null && supportGroupOnly.booleanValue()) {
                // If points >= 15 there must have been an error, because we skip the points pages and we should be
                return false;
            }
            if (doesSchedule8Paragraph4Apply != null) {
                return false;
            }
            return isValidCombinationFromSelectSchedule7ActivitiesOnwards(schedule7ActivitiesSelected, doesSchedule9Paragraph4);
        }
    }

    private YesNo getYesNoFieldValue(Boolean value) {
        return value == null ? null : (value.booleanValue() ? YES : NO);
    }

    /**
     * Test the continuity of boundaries between point ranges and regulation and schedule answers. (ie. this test will fail if there are any gaps, or overlap between the boundaries)
     */
    @Test
    @Parameters(named = "wcaAppealAndScheduleAndRegulationQuestionCombinations")
    public void testThatAtExactlyOneConditionIsApplicableForAllPointsAndActivityCombinations(
        Boolean wcaAppeal, Boolean doesSchedule8Paragraph4Apply, Boolean schedule7ActivitiesSelected,
        Boolean doesSchedule9Paragraph4Apply, Boolean isSevereCondition) {

        int minPointsValue = 0;
        int maxPointsValue = 0;

        if (wcaAppeal != null && wcaAppeal && !isTrue(isSevereCondition)) {
            minPointsValue = 14;
            maxPointsValue = 15;
        }

        for (Boolean supportGroupOnly : Arrays.asList(null, false, true)) {

            for (boolean allowed : Arrays.asList(false, true)) {

                if (isTrue(isSevereCondition) && isTrue(supportGroupOnly)) {
                    minPointsValue = 14;
                    maxPointsValue = 15;
                }

                for (int points = minPointsValue; points <= maxPointsValue; points++) {

                    int conditionApplicableCount = 0;

                    final boolean isValidCombinationExpected =
                        isValidPointsBasedCombinationExpected(points, wcaAppeal, isSevereCondition, doesSchedule8Paragraph4Apply, schedule7ActivitiesSelected,
                            doesSchedule9Paragraph4Apply, supportGroupOnly);

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
                            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                                .writeFinalDecisionGenerateNotice(YES)
                                .writeFinalDecisionAllowedOrRefused(allowed ? "allowed" : "refused")
                                .build())
                            .supportGroupOnlyAppeal(supportGroupOnly == null ? null : supportGroupOnly ? "Yes" : "No")
                            .extendedSscsCaseData(ExtendedSscsCaseData.builder().writeFinalDecisionSevereCriteriaApply(isSevereCondition ? YES : null).build())
                            .dwpReassessTheAward(null)
                            .wcaAppeal(YES)
                            .sscsUcCaseData(
                                SscsUcCaseData.builder().ucWriteFinalDecisionSchedule7ActivitiesApply(schedule7ActivitesApply)
                                    .ucWriteFinalDecisionSchedule7ActivitiesQuestion(schedule7Activities)
                                    .doesSchedule8Paragraph4Apply(getYesNoFieldValue(doesSchedule8Paragraph4Apply))
                                    .doesSchedule9Paragraph4Apply(getYesNoFieldValue(doesSchedule9Paragraph4Apply)).build()).build();
                    } else {
                        caseData = SscsCaseData.builder()
                            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                                .writeFinalDecisionGenerateNotice(YES)
                                .writeFinalDecisionAllowedOrRefused(allowed ? "allowed" : "refused")
                                .build())
                            .supportGroupOnlyAppeal(supportGroupOnly == null ? null : supportGroupOnly ? "Yes" : "No")
                            .wcaAppeal(NO)
                            .sscsUcCaseData(
                                SscsUcCaseData.builder().ucWriteFinalDecisionSchedule7ActivitiesApply(schedule7ActivitesApply)
                                    .ucWriteFinalDecisionSchedule7ActivitiesQuestion(schedule7Activities).build()).build();
                    }

                    Mockito.when(questionService.getTotalPoints(Mockito.eq(caseData), Mockito.any())).thenReturn(points);

                    UcPointsRegulationsAndSchedule7ActivitiesCondition matchingCondition = null;

                    for (UcPointsRegulationsAndSchedule7ActivitiesCondition ucPointsCondition : UcPointsRegulationsAndSchedule7ActivitiesCondition.values()) {

                        if (ucPointsCondition.isApplicable(questionService, caseData)) {
                            conditionApplicableCount++;
                            matchingCondition = ucPointsCondition;
                        }
                    }

                    Assert.assertEquals(
                        "Expected 1 condition to be satisfied for points:" + points + ":" + wcaAppeal + ":" + doesSchedule8Paragraph4Apply + ":" + schedule7ActivitiesSelected + ":" + doesSchedule9Paragraph4Apply + ":" + supportGroupOnly +  " but "
                            + conditionApplicableCount + " were satisfied",
                        1, conditionApplicableCount);

                    if (isValidCombinationExpected) {
                        Assert.assertTrue("Unexpected error for:" + points + ":" + wcaAppeal + ":" + doesSchedule8Paragraph4Apply
                            + ":" + schedule7ActivitiesSelected + ":" + doesSchedule9Paragraph4Apply + ":" + supportGroupOnly, matchingCondition
                            .getOptionalErrorMessage(questionService, caseData).isEmpty());
                    } else {
                        Assert.assertTrue("Expected an error for:" + points + ":" + wcaAppeal + ":" + doesSchedule8Paragraph4Apply
                            + ":" + schedule7ActivitiesSelected + ":" + doesSchedule9Paragraph4Apply + ":" + supportGroupOnly, matchingCondition
                            .getOptionalErrorMessage(questionService, caseData).isPresent());
                    }

                    if (matchingCondition != null && matchingCondition
                        .getOptionalErrorMessage(questionService, caseData).isEmpty()) {
                        // If we have a matching condition and the errors are null, now check the allowed/refused conditions
                        int allowedOrRefusedConditionApplicableCount = 0;

                        final boolean isValidAllowedOrRefusedCombinationExpected =
                            isValidAllowedOrRefusedCombinationExpected(points, wcaAppeal, isSevereCondition, doesSchedule8Paragraph4Apply, schedule7ActivitiesSelected,
                                doesSchedule9Paragraph4Apply, allowed, supportGroupOnly);

                        UcAllowedOrRefusedCondition matchingAllowedOrRefusedCondition = null;
                        for (UcAllowedOrRefusedCondition ucPointsCondition : UcAllowedOrRefusedCondition.values()) {

                            if (ucPointsCondition.isApplicable(questionService, caseData)) {
                                allowedOrRefusedConditionApplicableCount++;
                                matchingAllowedOrRefusedCondition = ucPointsCondition;
                            }
                        }

                        Assert.assertEquals(
                            "Expected 1 allowed or refused condition to be satisfied for points:" + points + ":" + wcaAppeal + ":" + doesSchedule8Paragraph4Apply + ":" + schedule7ActivitiesSelected + ":" + doesSchedule9Paragraph4Apply + ":" + allowed
                                + ":" + supportGroupOnly + " but "
                                + allowedOrRefusedConditionApplicableCount + " were satisfied",
                            1, allowedOrRefusedConditionApplicableCount);

                        if (isValidAllowedOrRefusedCombinationExpected) {


                            Assert.assertTrue("Unexpected error for:" + points + ":" + wcaAppeal + ":" + doesSchedule8Paragraph4Apply
                                + ":" + schedule7ActivitiesSelected + ":" + doesSchedule9Paragraph4Apply, matchingAllowedOrRefusedCondition
                                .getOptionalErrorMessage(questionService, caseData).isEmpty());


                            // Assert that for a valid allowed/refused condition, we can always retrieve a single scenario
                            // which we will use to generate content
                            UcScenario scenario = matchingAllowedOrRefusedCondition.getUcScenario(caseData);
                            Assert.assertNotNull(scenario);

                        } else {
                            Assert.assertTrue("Expected an error for:" + points + ":" + wcaAppeal + ":" + doesSchedule8Paragraph4Apply
                                + ":" + schedule7ActivitiesSelected + ":" + doesSchedule9Paragraph4Apply, matchingAllowedOrRefusedCondition
                                .getOptionalErrorMessage(questionService, caseData).isPresent());
                        }

                    }

                }

            }
        }
    }

    @Test
    public void testAllPointsConditionAttributesAreNotNull() {
        for (UcPointsRegulationsAndSchedule7ActivitiesCondition ucPointsCondition : UcPointsRegulationsAndSchedule7ActivitiesCondition.values()) {
            Assert.assertNotNull(ucPointsCondition.getAnswersExtractor());
            Assert.assertSame(ucPointsCondition.getAnswersExtractor(), UcPointsRegulationsAndSchedule7ActivitiesCondition.getAllAnswersExtractor());
            Assert.assertNotNull(ucPointsCondition.getEnumClass());
            Assert.assertEquals(UcPointsRegulationsAndSchedule7ActivitiesCondition.class, ucPointsCondition.getEnumClass());
            Assert.assertNotNull(ucPointsCondition.getPointsRequirementCondition());
        }
    }

    @Test
    public void testThatWcaAppealSevereConditionsOnlyCasePassesWithoutError() {
        SscsCaseData caseData = SscsCaseData.builder()
                .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                        .writeFinalDecisionGenerateNotice(YES)
                        .writeFinalDecisionAllowedOrRefused("allowed")
                        .build())
                .supportGroupOnlyAppeal("No")
                .extendedSscsCaseData(ExtendedSscsCaseData.builder()
                        .writeFinalDecisionSevereYesNo(YES)
                        .writeFinalDecisionSevereCriteriaApply(YES)
                        .writeFinalDecisionSevereYesNo(YES).build())
                .dwpReassessTheAward(null)
                .wcaAppeal(YES)
                .sscsUcCaseData(SscsUcCaseData.builder().build()).build();

        Mockito.when(questionService.getTotalPoints(Mockito.eq(caseData), Mockito.any())).thenReturn(0);

        List<UcPointsRegulationsAndSchedule7ActivitiesCondition> applicableConditions = getAllApplicableRegulationsAndSchedule7ActivitiesConditionsForCaseData(caseData);

        Assert.assertEquals(1,applicableConditions.size());
        Assert.assertEquals(SEVERE_CONDITIONS_WITH_SV_ISSUE_CODE, applicableConditions.getFirst());
        Assert.assertTrue(applicableConditions.getFirst().getOptionalErrorMessage(questionService, caseData).isEmpty());

        List<UcAllowedOrRefusedCondition> applicableAllowedOrRefusedConditions = getAllApplicableUcAllowedOrRefusedConditionsForCaseData(caseData);

        Assert.assertEquals(1, applicableAllowedOrRefusedConditions.size());
        Assert.assertEquals(UcAllowedOrRefusedCondition.SEVERE_CONDITIONS_ALLOWED_SV_ISSUE_CODE_CASE, applicableAllowedOrRefusedConditions.getFirst());
        Assert.assertTrue(applicableAllowedOrRefusedConditions.getFirst().getOptionalErrorMessage(questionService, caseData).isEmpty());
    }


    @Test
    public void testThatWcaAppealWithHighPointsSchedule7AndSevereCriteriaApplyPassesWithoutError() {

        SscsCaseData caseData = SscsCaseData.builder()
                .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                        .writeFinalDecisionGenerateNotice(YES)
                        .writeFinalDecisionAllowedOrRefused("allowed")
                        .build())
                .supportGroupOnlyAppeal("No")
                .extendedSscsCaseData(ExtendedSscsCaseData.builder()
                        .writeFinalDecisionSevereYesNo(YES)
                        .writeFinalDecisionSevereCriteriaApply(YES).build())
                .dwpReassessTheAward(null)
                .wcaAppeal(YES)
                .sscsUcCaseData(
                        SscsUcCaseData.builder().ucWriteFinalDecisionSchedule7ActivitiesApply("Yes")
                                .ucWriteFinalDecisionSchedule7ActivitiesQuestion(List.of("someActivity"))
                                .build()).build();

        Mockito.when(questionService.getTotalPoints(Mockito.eq(caseData), Mockito.any())).thenReturn(15);

        List<UcPointsRegulationsAndSchedule7ActivitiesCondition> applicableConditions = getAllApplicableRegulationsAndSchedule7ActivitiesConditionsForCaseData(caseData);

        Assert.assertEquals(1, applicableConditions.size());
        Assert.assertEquals(HIGH_POINTS_SCHEDULE_9_PARAGRAPH_4_UNSPECIFIED, applicableConditions.getFirst());
        Assert.assertTrue(applicableConditions.getFirst().getOptionalErrorMessage(questionService, caseData).isEmpty());

        List<UcAllowedOrRefusedCondition> applicableAllowedOrRefusedConditions = getAllApplicableUcAllowedOrRefusedConditionsForCaseData(caseData);

        Assert.assertEquals(1, applicableAllowedOrRefusedConditions.size());
        Assert.assertEquals(UcAllowedOrRefusedCondition.SEVERE_CONDITIONS_ALLOWED_HIGH_POINTS_SEVERE_CONDITIONS_CASE, applicableAllowedOrRefusedConditions.getFirst());
        Assert.assertTrue(applicableAllowedOrRefusedConditions.getFirst().getOptionalErrorMessage(questionService, caseData).isEmpty());
    }


    @Test
    public void testThatWcaAppealSupportGroupOnlySchedule7AndSevereCriteriaApplyPassesWithoutError() {

        SscsCaseData caseData = SscsCaseData.builder()
                .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                        .writeFinalDecisionGenerateNotice(YES)
                        .writeFinalDecisionAllowedOrRefused("allowed")
                        .build())
                .supportGroupOnlyAppeal("yes")
                .extendedSscsCaseData(ExtendedSscsCaseData.builder()
                        .writeFinalDecisionSevereCriteriaApply(YES).build())
                .dwpReassessTheAward(null)
                .wcaAppeal(YES)
                .sscsUcCaseData(
                        SscsUcCaseData.builder().ucWriteFinalDecisionSchedule7ActivitiesApply("Yes")
                                .ucWriteFinalDecisionSchedule7ActivitiesQuestion(List.of("someActivity"))
                                .build()).build();

        Mockito.when(questionService.getTotalPoints(Mockito.eq(caseData), Mockito.any())).thenReturn(0);

        List<UcPointsRegulationsAndSchedule7ActivitiesCondition> applicableConditions = getAllApplicableRegulationsAndSchedule7ActivitiesConditionsForCaseData(caseData);

        Assert.assertEquals(1, applicableConditions.size());
        Assert.assertEquals(LOW_POINTS_SCHEDULE6_AND_REG_29_SKIPPED_SCHEDULE_9_PARAGRAPH_4_UNSPECIFIED_SUPPORT_GROUP_ONLY, applicableConditions.getFirst());
        Assert.assertTrue(applicableConditions.getFirst().getOptionalErrorMessage(questionService, caseData).isEmpty());

        List<UcAllowedOrRefusedCondition> applicableAllowedOrRefusedConditions = getAllApplicableUcAllowedOrRefusedConditionsForCaseData(caseData);

        Assert.assertEquals(1, applicableAllowedOrRefusedConditions.size());
        Assert.assertEquals(UcAllowedOrRefusedCondition.ALLOWED_SUPPORT_GROUP_ONLY_SEVERE_CONDITIONS_CASE, applicableAllowedOrRefusedConditions.getFirst());
        Assert.assertTrue(applicableAllowedOrRefusedConditions.getFirst().getOptionalErrorMessage(questionService, caseData).isEmpty());
    }

    @Test
    public void testThatWcaAppealLowPointsSchedule8Paragraph4Schedule7AndSevereCriteriaApplyPassesWithoutError() {

        SscsCaseData caseData = SscsCaseData.builder()
                .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                        .writeFinalDecisionGenerateNotice(YES)
                        .writeFinalDecisionAllowedOrRefused("allowed")
                        .build())
                .supportGroupOnlyAppeal("no")
                .extendedSscsCaseData(ExtendedSscsCaseData.builder()
                        .writeFinalDecisionSevereCriteriaApply(YES).build())
                .dwpReassessTheAward(null)
                .wcaAppeal(YES)
                .sscsUcCaseData(
                        SscsUcCaseData.builder().ucWriteFinalDecisionSchedule7ActivitiesApply("Yes")
                                .ucWriteFinalDecisionMobilisingUnaidedQuestion("someAnswer")
                                .doesSchedule8Paragraph4Apply(YES)
                                .ucWriteFinalDecisionSchedule7ActivitiesQuestion(List.of("someActivity"))
                                .build()).build();

        Mockito.when(questionService.getTotalPoints(Mockito.eq(caseData), Mockito.any())).thenReturn(10);

        List<UcPointsRegulationsAndSchedule7ActivitiesCondition> applicableConditions = getAllApplicableRegulationsAndSchedule7ActivitiesConditionsForCaseData(caseData);

        Assert.assertEquals(1, applicableConditions.size());
        Assert.assertEquals(UcPointsRegulationsAndSchedule7ActivitiesCondition.LOW_POINTS_SCHEDULE_8_PARAGRAPH_4_DOES_APPLY_SCHEDULE_9_PARAGRAPH_4_UNSPECIFIED_NON_SUPPORT_GROUP_ONLY, applicableConditions.getFirst());
        Assert.assertTrue(applicableConditions.getFirst().getOptionalErrorMessage(questionService, caseData).isEmpty());

        List<UcAllowedOrRefusedCondition> applicableAllowedOrRefusedConditions = getAllApplicableUcAllowedOrRefusedConditionsForCaseData(caseData);

        Assert.assertEquals(1, applicableAllowedOrRefusedConditions.size());
        Assert.assertEquals(UcAllowedOrRefusedCondition.ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS_SEVERE_CONDITIONS_CASE, applicableAllowedOrRefusedConditions.getFirst());
        Assert.assertTrue(applicableAllowedOrRefusedConditions.getFirst().getOptionalErrorMessage(questionService, caseData).isEmpty());
    }

    private List<UcPointsRegulationsAndSchedule7ActivitiesCondition> getAllApplicableRegulationsAndSchedule7ActivitiesConditionsForCaseData(SscsCaseData caseData) {
        List<UcPointsRegulationsAndSchedule7ActivitiesCondition> applicablePointsConditions = new ArrayList<>();

        for (UcPointsRegulationsAndSchedule7ActivitiesCondition ucPointsCondition : UcPointsRegulationsAndSchedule7ActivitiesCondition.values()) {

            if (ucPointsCondition.isApplicable(questionService, caseData)) {
                applicablePointsConditions.add(ucPointsCondition);
            }
        }
        return applicablePointsConditions;
    }

    private List<UcAllowedOrRefusedCondition> getAllApplicableUcAllowedOrRefusedConditionsForCaseData(SscsCaseData caseData) {
        List<UcAllowedOrRefusedCondition> applicableUcConditions = new ArrayList<>();

        for (UcAllowedOrRefusedCondition ucPointsCondition : UcAllowedOrRefusedCondition.values()) {

            if (ucPointsCondition.isApplicable(questionService, caseData)) {
                applicableUcConditions.add(ucPointsCondition);
            }
        }
        return applicableUcConditions;
    }
}