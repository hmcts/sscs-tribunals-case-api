package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@Component
@Slf4j
public class WriteFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DecisionNoticeQuestionService decisionNoticeQuestionService;
    private final PreviewDocumentService previewDocumentService;

    @Autowired
    public WriteFinalDecisionAboutToSubmitHandler(DecisionNoticeQuestionService decisionNoticeQuestionService,
                                                  PreviewDocumentService previewDocumentService) {
        this.decisionNoticeQuestionService = decisionNoticeQuestionService;
        this.previewDocumentService = previewDocumentService;
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

        // Due to a bug with CCD related to hidden fields, this field is not being set
        // on the final submission from CCD, so we need to reset it here
        // See https://tools.hmcts.net/jira/browse/RDM-8200
        // This is a temporary workaround for this issue.
        sscsCaseData.setWriteFinalDecisionGeneratedDate(LocalDate.now().toString());

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        getDecisionNoticePointsValidationErrorMessages(sscsCaseData).forEach(preSubmitCallbackResponse::addError);

        previewDocumentService.writePreviewDocumentToSscsDocument(sscsCaseData, DRAFT_DECISION_NOTICE, sscsCaseData.getWriteFinalDecisionPreviewDocument());

        return preSubmitCallbackResponse;
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
     * Given a points condition, and an SscsCaseData instance, obtain an error message for that condition if the condition has failed to be satified, or an empty optional if the condition is met.
     *
     * @param pointsCondition The condition to evaluate against the SscsCaseData
     * @param sscsCaseData The SscsCaseData to evaluate against the condition.
     * @return An optional error message if the condition has failed to be satified, or an empty optional if the condition is met.
     */
    private Optional<String> getOptionalErrorMessage(PointsCondition pointsCondition, SscsCaseData sscsCaseData) {

        Collection<String> collection = emptyIfNull(pointsCondition.getActivityType().getAnswersExtractor().apply(sscsCaseData));

        if (collection.isEmpty()) {
            return Optional.empty();
        }
        int totalPoints = collection.stream().map(answerText -> decisionNoticeQuestionService.getAnswerForActivityQuestionKey(sscsCaseData,
                answerText)).filter(Optional::isPresent).map(Optional::get).mapToInt(ActivityAnswer::getActivityAnswerPoints).sum();

        return pointsCondition.getPointsRequirementCondition().test(totalPoints) ? Optional.empty() :
            Optional.of(pointsCondition.getErrorMessage());

    }
}
