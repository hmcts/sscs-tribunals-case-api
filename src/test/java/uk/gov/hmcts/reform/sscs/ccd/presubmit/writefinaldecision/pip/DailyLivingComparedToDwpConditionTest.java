package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;

public class DailyLivingComparedToDwpConditionTest {

    @Test
    public void testGetErrorMessageWhenNotSet() {
        DailyLivingComparedToDwpCondition condition = new DailyLivingComparedToDwpCondition(ComparedToDwpPredicate.HIGHER);
        SscsCaseData caseData = SscsCaseData.builder().build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertEquals("a missing answer for the 'daily living compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
    }

    @Test
    public void testIsHigherWhenSame() {
        DailyLivingComparedToDwpCondition condition = new DailyLivingComparedToDwpCondition(ComparedToDwpPredicate.HIGHER);
        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("same").build()).build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertEquals("same for the 'daily living compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for daily living is higher than that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsHigherWhenLower() {
        DailyLivingComparedToDwpCondition condition = new DailyLivingComparedToDwpCondition(ComparedToDwpPredicate.HIGHER);
        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("lower").build()).build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertEquals("lower for the 'daily living compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for daily living is higher than that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsHigherWhenHigher() {
        DailyLivingComparedToDwpCondition condition = new DailyLivingComparedToDwpCondition(ComparedToDwpPredicate.HIGHER);
        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher").build()).build();
        Assert.assertTrue(condition.isSatisified(caseData));
        Assert.assertFalse(condition.getOptionalErrorMessage(caseData).isPresent());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for daily living is higher than that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsLowerWhenSame() {
        DailyLivingComparedToDwpCondition condition = new DailyLivingComparedToDwpCondition(ComparedToDwpPredicate.LOWER);
        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("same").build())
                .build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertEquals("same for the 'daily living compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for daily living is lower than that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsLowerWhenLower() {
        DailyLivingComparedToDwpCondition condition = new DailyLivingComparedToDwpCondition(ComparedToDwpPredicate.LOWER);
        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("lower").build()).build();
        Assert.assertTrue(condition.isSatisified(caseData));
        Assert.assertFalse(condition.getOptionalErrorMessage(caseData).isPresent());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for daily living is lower than that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsLowerWhenHigher() {
        DailyLivingComparedToDwpCondition condition = new DailyLivingComparedToDwpCondition(ComparedToDwpPredicate.LOWER);
        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher").build()).build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertEquals("higher for the 'daily living compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for daily living is lower than that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsSameWhenSame() {
        DailyLivingComparedToDwpCondition condition = new DailyLivingComparedToDwpCondition(ComparedToDwpPredicate.SAME);
        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("same").build()).build();
        Assert.assertTrue(condition.isSatisified(caseData));
        Assert.assertFalse(condition.getOptionalErrorMessage(caseData).isPresent());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for daily living is the same as that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsSameWhenLower() {
        DailyLivingComparedToDwpCondition condition = new DailyLivingComparedToDwpCondition(ComparedToDwpPredicate.SAME);
        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("lower").build()).build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertTrue(condition.getOptionalErrorMessage(caseData).isPresent());
        Assert.assertEquals("lower for the 'daily living compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for daily living is the same as that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsSameWhenHigher() {
        DailyLivingComparedToDwpCondition condition = new DailyLivingComparedToDwpCondition(ComparedToDwpPredicate.SAME);
        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher").build()).build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertTrue(condition.getOptionalErrorMessage(caseData).isPresent());
        Assert.assertEquals("higher for the 'daily living compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for daily living is the same as that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }


}
