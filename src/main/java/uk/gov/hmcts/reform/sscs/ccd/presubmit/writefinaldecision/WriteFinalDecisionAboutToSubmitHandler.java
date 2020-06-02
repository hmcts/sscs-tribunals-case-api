package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_IN_FAVOUR_OF_APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_UPHELD;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.domain.wrapper.ComparedRate;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

@Component
@Slf4j
public class WriteFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DecisionNoticeQuestionService decisionNoticeQuestionService;

    @Autowired
    public WriteFinalDecisionAboutToSubmitHandler(DecisionNoticeQuestionService decisionNoticeQuestionService) {
        this.decisionNoticeQuestionService = decisionNoticeQuestionService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.WRITE_FINAL_DECISION
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        getDecisionNoticePointsValidationErrorMessages(sscsCaseData).forEach(preSubmitCallbackResponse::addError);

        sscsCaseData.setDwpState(FINAL_DECISION_ISSUED.getId());

        calculateOutcomeCode(sscsCaseData, preSubmitCallbackResponse);

        return preSubmitCallbackResponse;
    }

    private void calculateOutcomeCode(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        String outcome = null;
        if (null != sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion()) {
            outcome = sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion().equals(ComparedRate.Higher.name())
                    ? DECISION_IN_FAVOUR_OF_APPELLANT.getId() : DECISION_UPHELD.getId();
        }

        if (DECISION_UPHELD.getId().equals(outcome) && null != sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion()) {
            outcome = sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion().equals(ComparedRate.Higher.name())
                    ? DECISION_IN_FAVOUR_OF_APPELLANT.getId() : DECISION_UPHELD.getId();
        }

        if (outcome != null) {
            sscsCaseData.setOutcome(outcome);
        } else {
            log.error("Outcome cannot be empty when generating final decision. Something has gone wrong for caseId: ", sscsCaseData.getCcdCaseId());
            preSubmitCallbackResponse.addError("Outcome cannot be empty. Please check case data. If problem continues please contact support");
        }

    }

    private List<String> getDecisionNoticePointsValidationErrorMessages(SscsCaseData sscsCaseData) {

        return Arrays.stream(PointsCondition.values())
            .filter(pointsCondition -> pointsCondition.isApplicable(sscsCaseData))
            .map(pointsCondition ->
                getOptionalErrorMessage(pointsCondition, sscsCaseData))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    /**
     * Obtain the points award for an activity question, given an SscsCaseData instance.
     *
     * @param sscsCaseData The SscsCaseData
     * @param activityQuestionKey The key of an activity question.
     * @return The points awarded for that question, given the SscsCaseData instance provided.
     */
    private int getPointsForActivityQuestionKey(SscsCaseData sscsCaseData, String activityQuestionKey) {

        Function<SscsCaseData, String> answerExtractor =
            ActivityQuestion.getByKey(activityQuestionKey).getAnswerExtractor();
        return decisionNoticeQuestionService
            .extractPointsFromSelectedValue(answerExtractor.apply(sscsCaseData));
    }

    /**
     * Given a points condition, and an SscsCaseData instance, obtain an error message for that condition if the condition has failed to be satified, or an empty optional if the condition is met.
     *
     * @param pointsCondition The condition to evaluate against the SscsCaseData
     * @param sscsCaseData The SscsCaseData to evaluate against the condition.
     * @return An optional error message if the condition has failed to be satified, or an empty optional if the condition is met.
     */
    private Optional<String> getOptionalErrorMessage(PointsCondition pointsCondition, SscsCaseData sscsCaseData) {

        int totalPoints = pointsCondition.getActivityType().getAnswersExtractor().apply(sscsCaseData)
            .stream().mapToInt(answerText -> getPointsForActivityQuestionKey(sscsCaseData,
                answerText)).sum();

        return pointsCondition.getPointsRequirementCondition().test(totalPoints) ? Optional.empty() :
            Optional.of(pointsCondition.getErrorMessage());

    }
}
