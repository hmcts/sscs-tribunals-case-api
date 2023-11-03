package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen;

import static org.mockito.MockitoAnnotations.openMocks;

import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class GenAllowedOrRefusedConditionTest {

    @Mock
    private DecisionNoticeQuestionService questionService;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @NamedParameters("allowedOrRefusedConditions")
    @SuppressWarnings("unused")
    private Object[] allowedOrRefusedConditions() {
        return new Object[] {
            new String[] {"allowed"},
            new String[] {"refused"},
            new String[] {"somethingElse"},
            new String[] {null}
        };
    }

    private boolean isValidAllowedOrRefusedCombinationExpected(String allowedOrRefused) {
        return ("allowed".equals(allowedOrRefused) || "refused".equals(allowedOrRefused));
    }

    @Test
    @Parameters(named = "allowedOrRefusedConditions")
    public void testThatAtExactlyOneConditionIsApplicableForAllAllowedAndRefusedConditions(
        String allowedOrRefused) {

        int conditionApplicableCount = 0;

        final boolean isValidCombinationExpected =
            isValidAllowedOrRefusedCombinationExpected(allowedOrRefused);

        SscsCaseData caseData = SscsCaseData.builder()
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionGenerateNotice(YesNo.YES)
                .writeFinalDecisionAllowedOrRefused(allowedOrRefused)
                .build())
            .dwpReassessTheAward(null).build();

        GenAllowedOrRefusedCondition matchingCondition = null;

        for (GenAllowedOrRefusedCondition genAllowedOrRefusedCondition : GenAllowedOrRefusedCondition.values()) {

            if (genAllowedOrRefusedCondition.isApplicable(questionService, caseData)) {
                conditionApplicableCount++;
                matchingCondition = genAllowedOrRefusedCondition;
            }
        }

        if (isValidCombinationExpected) {

            Assert.assertEquals(
                "Expected 1 condition to be satisfied for points:" + allowedOrRefused + " but "
                    + conditionApplicableCount + " were satisfied",
                1, conditionApplicableCount);

        } else {
            Assert.assertEquals(
                "Expected 0 conditions to be satisfied for points:" + allowedOrRefused + " but "
                    + conditionApplicableCount + " were satisfied",
                0, conditionApplicableCount);
        }

        if ("allowed".equals(allowedOrRefused)) {
            Assert.assertEquals(GenAllowedOrRefusedCondition.ALLOWED_CONDITION, matchingCondition);
        } else if ("refused".equals(allowedOrRefused)) {
            Assert.assertEquals(GenAllowedOrRefusedCondition.REFUSED_CONDITION, matchingCondition);
        }
    }

    @Test
    public void testAllPointsConditionAttributesAreNotNull() {
        for (GenAllowedOrRefusedCondition condition : GenAllowedOrRefusedCondition.values()) {
            Assert.assertNotNull(condition.getAnswersExtractor());
            Assert.assertSame(condition.getAnswersExtractor(), GenAllowedOrRefusedCondition.getAllAnswersExtractor());
            Assert.assertNotNull(condition.getEnumClass());
            Assert.assertEquals(GenAllowedOrRefusedCondition.class, condition.getEnumClass());
            Assert.assertNotNull(condition.getPointsRequirementCondition());
        }
    }
}
