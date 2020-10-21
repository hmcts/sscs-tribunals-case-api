package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Collection;
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

    /**
     * Given a points condition, and an SscsCaseData instance, obtain an error message for that condition if the condition has failed to be satified, or an empty optional if the condition is met.
     *
     * @param questionService The question service to use
     * @param sscsCaseData The SscsCaseData to evaluate against the condition.
     * @return An optional error message if the condition has failed to be satified, or an empty optional if the condition is met.
     */
    default Optional<String> getOptionalErrorMessage(DecisionNoticeQuestionService questionService, SscsCaseData sscsCaseData) {

        Collection<String> collection = emptyIfNull(getAnswersExtractor().apply(sscsCaseData));

        if (collection.isEmpty()) {
            return Optional.empty();
        }
        int totalPoints = collection.stream().map(answerText -> questionService.getAnswerForActivityQuestionKey(sscsCaseData,
            answerText)).filter(Optional::isPresent).map(Optional::get).mapToInt(ActivityAnswer::getActivityAnswerPoints).sum();

        return getPointsRequirementCondition().test(totalPoints) ? Optional.empty() :
            Optional.of(getErrorMessage());

    }
}