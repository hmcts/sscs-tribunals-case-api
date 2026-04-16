package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.function.IntPredicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExtendedSscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

@ExtendWith(MockitoExtension.class)
class EsaAllowedOrRefusedConditionTest {
    @Mock
    private DecisionNoticeQuestionService questionService;

    @Test
    void shouldReturnCorrectEnumClass() {
        assertEquals(
                EsaAllowedOrRefusedCondition.class,
                EsaAllowedOrRefusedCondition.REFUSED_NON_SUPPORT_GROUP_ONLY.getEnumClass()
        );
    }

    @Test
    void shouldApplyPointsCondition_whenPresent() {
        IntPredicate predicate = EsaAllowedOrRefusedCondition.REFUSED_SUPPORT_GROUP_ONLY_LOW_POINTS
                .getPointsRequirementCondition();

        assertTrue(predicate.test(10));
        assertFalse(predicate.test(20));
    }

    @Test
    void shouldAlwaysReturnTrue_whenNoPointsCondition() {
        IntPredicate predicate = EsaAllowedOrRefusedCondition.ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_SELECTED
                .getPointsRequirementCondition();

        assertTrue(predicate.test(0));
        assertTrue(predicate.test(100));
    }

    @Test
    void shouldReturnMatchingCondition_whenValidCaseDataProvided() {
        SscsCaseData caseData = buildValidCaseData();
        when(questionService.getTotalPoints(any(), any())).thenReturn(10);

        EsaAllowedOrRefusedCondition result = EsaAllowedOrRefusedCondition
                .getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(questionService, caseData);

        assertNotNull(result);
    }

    @Test
    void shouldThrowException_whenNoConditionMatches() {
        SscsCaseData caseData = buildInvalidCaseData();
        when(questionService.getTotalPoints(any(), any())).thenReturn(999);

        assertThrows(IllegalStateException.class, () -> EsaAllowedOrRefusedCondition
                .getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(questionService, caseData)
        );
    }

    private SscsCaseData buildValidCaseData() {

        SscsFinalDecisionCaseData finalDecision =
                SscsFinalDecisionCaseData.builder()
                        .writeFinalDecisionGenerateNotice(YesNo.YES)
                        .writeFinalDecisionAllowedOrRefused("refused")
                        .build();

        SscsEsaCaseData esa =
                SscsEsaCaseData.builder()
                        .doesRegulation29Apply(YesNo.NO)
                        .doesRegulation35Apply(YesNo.NO)
                        .esaWriteFinalDecisionSchedule3ActivitiesQuestion(Collections.emptyList())
                        .build();

        ExtendedSscsCaseData extended =
                ExtendedSscsCaseData.builder()
                        .writeFinalDecisionSevereYesNo(YesNo.NO)
                        .esaWriteFinalDecisionSevereCriteriaApply(YesNo.NO)
                        .build();

        return SscsCaseData.builder()
                .finalDecisionCaseData(finalDecision)
                .sscsEsaCaseData(esa)
                .extendedSscsCaseData(extended)
                .wcaAppeal(YesNo.YES)
                .supportGroupOnlyAppeal("NO")
                .build();
    }

    private SscsCaseData buildInvalidCaseData() {

        SscsFinalDecisionCaseData finalDecision =
                SscsFinalDecisionCaseData.builder()
                        .writeFinalDecisionGenerateNotice(YesNo.YES)
                        .writeFinalDecisionAllowedOrRefused("allowed")
                        .build();

        SscsEsaCaseData esa =
                SscsEsaCaseData.builder()
                        .doesRegulation29Apply(YesNo.YES)
                        .doesRegulation35Apply(YesNo.YES)
                        .esaWriteFinalDecisionSchedule3ActivitiesQuestion(Collections.emptyList())
                        .build();

        ExtendedSscsCaseData extended =
                ExtendedSscsCaseData.builder()
                        .writeFinalDecisionSevereYesNo(YesNo.YES)
                        .esaWriteFinalDecisionSevereCriteriaApply(YesNo.YES)
                        .build();

        return SscsCaseData.builder()
                .finalDecisionCaseData(finalDecision)
                .sscsEsaCaseData(esa)
                .extendedSscsCaseData(extended)
                .wcaAppeal(YesNo.NO)
                .supportGroupOnlyAppeal("YES")
                .build();
    }
}