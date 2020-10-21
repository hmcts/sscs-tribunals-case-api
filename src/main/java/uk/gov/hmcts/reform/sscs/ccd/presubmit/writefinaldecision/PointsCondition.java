package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntPredicate;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

public interface PointsCondition<E extends Enum<E> & PointsCondition> {

    boolean isApplicable(SscsCaseData caseData);

    IntPredicate getPointsRequirementCondition();

    String getErrorMessage();

    Class<E> getEnumClass();

    Function<SscsCaseData, List<String>> getAnswersExtractor();

    Optional<String> getOptionalErrorMessage(DecisionNoticeQuestionService questionService, SscsCaseData sscsCaseData);
}