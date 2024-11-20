package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen;

import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

public class GenAllowedOrRefusedConditionTest {

    @Mock
    private DecisionNoticeQuestionService questionService;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }


    @SuppressWarnings("unused")
    private static Object[] allowedOrRefusedConditions() {
        return new Object[]{
            new String[]{"allowed"},
            new String[]{"refused"},
            new String[]{"somethingElse"},
            new String[]{null}
        };
    }

    private boolean isValidAllowedOrRefusedCombinationExpected(String allowedOrRefused) {
        return ("allowed".equals(allowedOrRefused) || "refused".equals(allowedOrRefused));
    }

    @ParameterizedTest
    @MethodSource("allowedOrRefusedConditions")
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

            Assertions.assertEquals(
                1, conditionApplicableCount, "Expected 1 condition to be satisfied for points:" + allowedOrRefused + " but "
                    + conditionApplicableCount + " were satisfied");

        } else {
            Assertions.assertEquals(
                0, conditionApplicableCount, "Expected 0 conditions to be satisfied for points:" + allowedOrRefused + " but "
                    + conditionApplicableCount + " were satisfied");
        }

        if ("allowed".equals(allowedOrRefused)) {
            Assertions.assertEquals(GenAllowedOrRefusedCondition.ALLOWED_CONDITION, matchingCondition);
        } else if ("refused".equals(allowedOrRefused)) {
            Assertions.assertEquals(GenAllowedOrRefusedCondition.REFUSED_CONDITION, matchingCondition);
        }
    }

    @ParameterizedTest
    public void testAllPointsConditionAttributesAreNotNull() {
        for (GenAllowedOrRefusedCondition condition : GenAllowedOrRefusedCondition.values()) {
            Assertions.assertNotNull(condition.getAnswersExtractor());
            Assertions.assertSame(condition.getAnswersExtractor(), GenAllowedOrRefusedCondition.getAllAnswersExtractor());
            Assertions.assertNotNull(condition.getEnumClass());
            Assertions.assertEquals(GenAllowedOrRefusedCondition.class, condition.getEnumClass());
            Assertions.assertNotNull(condition.getPointsRequirementCondition());
        }
    }
}
