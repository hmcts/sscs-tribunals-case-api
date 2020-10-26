package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.mockito.MockitoAnnotations.openMocks;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
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
        return new Object[]{
            new Boolean[]{null, null, null},
            new Boolean[]{null, null, false},
            new Boolean[]{null, null, true},
            new Boolean[]{null, false, null},
            new Boolean[]{null, false, false},
            new Boolean[]{null, false, true},
            new Boolean[]{null, true, null},
            new Boolean[]{null, true, false},
            new Boolean[]{null, true, true},
            new Boolean[]{false, null, null},
            new Boolean[]{false, null, false},
            new Boolean[]{false, null, true},
            new Boolean[]{false, false, null},
            new Boolean[]{false, false, false},
            new Boolean[]{false, false, true},
            new Boolean[]{false, true, null},
            new Boolean[]{false, true, false},
            new Boolean[]{false, true, true},
            new Boolean[]{true, null, null},
            new Boolean[]{true, null, false},
            new Boolean[]{true, null, true},
            new Boolean[]{true, false, null},
            new Boolean[]{true, false, false},
            new Boolean[]{true, false, true},
            new Boolean[]{true, true, null},
            new Boolean[]{true, true, false},
            new Boolean[]{true, true, true},
        };
    }

    private boolean isValidCombinationFromSelectSchedule3ActivitiesOnwards(Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply) {


        if (schedule3ActivitiesSelected == null) {
            return false;
        } else {
            if (schedule3ActivitiesSelected.booleanValue()) {
                if (doesRegulation35Apply != null) {
                    return false;
                }
            } else {
                if (doesRegulation35Apply == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidCombinationExpected(int points, Boolean doesRegulation29Apply, Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply) {

        if (points < 15) {
            if (doesRegulation29Apply == null) {
                return false;
            } else {
                if (!doesRegulation29Apply.booleanValue()) {
                    if (schedule3ActivitiesSelected != null || doesRegulation35Apply != null) {
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

        for (int points = minPointsValue; points <= maxPointsValue; points++) {

            int conditionApplicableCount = 0;

            final boolean isValidCombinationExpected =
                isValidCombinationExpected(points, doesRegulation29Apply, schedule3ActivitiesSelected,
                    doesRegulation35Apply);

            SscsCaseData caseData = SscsCaseData.builder()
                .doesRegulation29Apply(getYesNoFieldValue(doesRegulation29Apply))
                .doesRegulation35Apply(getYesNoFieldValue(doesRegulation35Apply)).build();

            Mockito.when(questionService.getTotalPoints(Mockito.eq(caseData),Mockito.any())).thenReturn(points);

            EsaPointsRegulationsAndSchedule3ActivitiesCondition matchingCondition = null;

            for (EsaPointsRegulationsAndSchedule3ActivitiesCondition esaPointsCondition : EsaPointsRegulationsAndSchedule3ActivitiesCondition.values()) {

                if (esaPointsCondition.isApplicable(questionService, caseData)) {
                    conditionApplicableCount++;
                    matchingCondition = esaPointsCondition;
                }
            }

            Assert.assertEquals(
                "Expected 1 condition to be satisfied for points:" + points + ":"  + doesRegulation29Apply + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply + " but "
                    + conditionApplicableCount + " were satisfied",
                1, conditionApplicableCount);


            if (isValidCombinationExpected) {
                Assert.assertTrue("Unexpected error for:" + points + ":"  + doesRegulation29Apply
                    + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply, matchingCondition
                    .getOptionalErrorMessage(questionService, caseData).isEmpty());
            } else {
                // FIXME Once activities are implemented, assert that we always get
                // a non-empty error message here
            }

        }
    }


    /**
     * We have separate tests above to ensure that only a single PointsCondition exists for all valid activity/points combinations - this method returns that condition,
     * or throws an exception if no matching condition.
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
