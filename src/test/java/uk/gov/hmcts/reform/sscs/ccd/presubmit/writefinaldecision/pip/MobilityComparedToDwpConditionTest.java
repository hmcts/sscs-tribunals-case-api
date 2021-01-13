package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class MobilityComparedToDwpConditionTest {

    @Test
    public void testGetErrorMessageWhenNotSet() {
        MobilityComparedToDwpCondition condition = new MobilityComparedToDwpCondition(ComparedToDwpPredicate.HIGHER);
        SscsCaseData caseData = SscsCaseData.builder().build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertEquals("a missing answer for the 'mobility compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
    }

    @Test
    public void testIsHigherWhenSame() {
        MobilityComparedToDwpCondition condition = new MobilityComparedToDwpCondition(ComparedToDwpPredicate.HIGHER);
        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpMobilityQuestion("same").build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertEquals("same for the 'mobility compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for mobility is higher than that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsHigherWhenLower() {
        MobilityComparedToDwpCondition condition = new MobilityComparedToDwpCondition(ComparedToDwpPredicate.HIGHER);
        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpMobilityQuestion("lower").build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertEquals("lower for the 'mobility compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for mobility is higher than that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsHigherWhenHigher() {
        MobilityComparedToDwpCondition condition = new MobilityComparedToDwpCondition(ComparedToDwpPredicate.HIGHER);
        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpMobilityQuestion("higher").build();
        Assert.assertTrue(condition.isSatisified(caseData));
        Assert.assertFalse(condition.getOptionalErrorMessage(caseData).isPresent());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for mobility is higher than that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsLowerWhenSame() {
        MobilityComparedToDwpCondition condition = new MobilityComparedToDwpCondition(ComparedToDwpPredicate.LOWER);
        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpMobilityQuestion("same").build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertEquals("same for the 'mobility compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for mobility is lower than that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsLowerWhenLower() {
        MobilityComparedToDwpCondition condition = new MobilityComparedToDwpCondition(ComparedToDwpPredicate.LOWER);
        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpMobilityQuestion("lower").build();
        Assert.assertTrue(condition.isSatisified(caseData));
        Assert.assertFalse(condition.getOptionalErrorMessage(caseData).isPresent());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for mobility is lower than that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsLowerWhenHigher() {
        MobilityComparedToDwpCondition condition = new MobilityComparedToDwpCondition(ComparedToDwpPredicate.LOWER);
        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpMobilityQuestion("higher").build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertEquals("higher for the 'mobility compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for mobility is lower than that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsSameWhenSame() {
        MobilityComparedToDwpCondition condition = new MobilityComparedToDwpCondition(ComparedToDwpPredicate.SAME);
        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpMobilityQuestion("same").build();
        Assert.assertTrue(condition.isSatisified(caseData));
        Assert.assertFalse(condition.getOptionalErrorMessage(caseData).isPresent());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for mobility is the same as that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsSameWhenLower() {
        MobilityComparedToDwpCondition condition = new MobilityComparedToDwpCondition(ComparedToDwpPredicate.SAME);
        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpMobilityQuestion("lower").build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertTrue(condition.getOptionalErrorMessage(caseData).isPresent());
        Assert.assertEquals("lower for the 'mobility compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for mobility is the same as that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }

    @Test
    public void testIsSameWhenHigher() {
        MobilityComparedToDwpCondition condition = new MobilityComparedToDwpCondition(ComparedToDwpPredicate.SAME);
        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpMobilityQuestion("higher").build();
        Assert.assertFalse(condition.isSatisified(caseData));
        Assert.assertTrue(condition.getOptionalErrorMessage(caseData).isPresent());
        Assert.assertEquals("higher for the 'mobility compared to DWP' question", condition.getOptionalErrorMessage(caseData).get());
        Assert.assertTrue(condition.getOptionalIsSatisfiedMessage(caseData).isPresent());
        Assert.assertEquals("specified that the award for mobility is the same as that awarded by DWP", condition.getOptionalIsSatisfiedMessage(caseData).get());
    }


}
