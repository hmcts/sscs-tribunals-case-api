package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla;

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
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class DlaAllowedOrRefusedConditionTest {

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
            .writeFinalDecisionGenerateNotice("Yes")
            .writeFinalDecisionAllowedOrRefused(allowedOrRefused)
            .dwpReassessTheAward(null).build();

        DlaAllowedOrRefusedCondition matchingCondition = null;

        for (DlaAllowedOrRefusedCondition dlaAllowedOrRefusedCondition : DlaAllowedOrRefusedCondition.values()) {

            if (dlaAllowedOrRefusedCondition.isApplicable(questionService, caseData)) {
                conditionApplicableCount++;
                matchingCondition = dlaAllowedOrRefusedCondition;
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
            Assert.assertEquals(DlaAllowedOrRefusedCondition.ALLOWED_CONDITION, matchingCondition);
        } else if ("refused".equals(allowedOrRefused)) {
            Assert.assertEquals(DlaAllowedOrRefusedCondition.REFUSED_CONDITION, matchingCondition);
        }
    }

    @Test
    public void testAllPointsConditionAttributesAreNotNull() {
        for (DlaAllowedOrRefusedCondition condition : DlaAllowedOrRefusedCondition.values()) {
            Assert.assertNotNull(condition.getAnswersExtractor());
            Assert.assertSame(condition.getAnswersExtractor(), DlaAllowedOrRefusedCondition.getAllAnswersExtractor());
            Assert.assertNotNull(condition.getEnumClass());
            Assert.assertEquals(DlaAllowedOrRefusedCondition.class, condition.getEnumClass());
            Assert.assertNotNull(condition.getPointsRequirementCondition());
        }
    }
}
