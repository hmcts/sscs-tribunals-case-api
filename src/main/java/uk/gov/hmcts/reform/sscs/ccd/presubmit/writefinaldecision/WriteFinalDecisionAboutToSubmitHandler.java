package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

@Component
@Slf4j
public class WriteFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DecisionNoticeQuestionService decisionNoticeQuestionService;
    private final DecisionNoticeOutcomeService decisionNoticeOutcomeService;

    @Autowired
    public WriteFinalDecisionAboutToSubmitHandler(DecisionNoticeQuestionService decisionNoticeQuestionService,
        DecisionNoticeOutcomeService decisionNoticeOutcomeService) {
        this.decisionNoticeQuestionService = decisionNoticeQuestionService;
        this.decisionNoticeOutcomeService = decisionNoticeOutcomeService;
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

        calculateOutcomeCode(sscsCaseData, preSubmitCallbackResponse);

        writePreviewDocumentToSscsDocument(sscsCaseData);

        return preSubmitCallbackResponse;
    }

    private void calculateOutcomeCode(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        Outcome outcome = decisionNoticeOutcomeService.determineOutcome(sscsCaseData);

        if (outcome != null) {
            sscsCaseData.setOutcome(outcome.getId());
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
     * Given a points condition, and an SscsCaseData instance, obtain an error message for that condition if the condition has failed to be satified, or an empty optional if the condition is met.
     *
     * @param pointsCondition The condition to evaluate against the SscsCaseData
     * @param sscsCaseData The SscsCaseData to evaluate against the condition.
     * @return An optional error message if the condition has failed to be satified, or an empty optional if the condition is met.
     */
    private Optional<String> getOptionalErrorMessage(PointsCondition pointsCondition, SscsCaseData sscsCaseData) {

        int totalPoints = pointsCondition.getActivityType().getAnswersExtractor().apply(sscsCaseData)
            .stream().map(answerText -> decisionNoticeQuestionService.getAnswerForActivityQuestionKey(sscsCaseData,
                answerText)).filter(Optional::isPresent).map(Optional::get).mapToInt(ActivityAnswer::getActivityAnswerPoints).sum();

        return pointsCondition.getPointsRequirementCondition().test(totalPoints) ? Optional.empty() :
            Optional.of(pointsCondition.getErrorMessage());

    }

    private void writePreviewDocumentToSscsDocument(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getSscsDocument() != null) {
            sscsCaseData.getSscsDocument()
                    .removeIf(doc -> DRAFT_DECISION_NOTICE.getValue().equals(doc.getValue().getDocumentType()));
        }

        SscsDocument draftDecisionNotice = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(createFileName(sscsCaseData))
                .documentLink(sscsCaseData.getWriteFinalDecisionPreviewDocument())
                .documentDateAdded(setDocumentDateAdded(sscsCaseData))
                .documentType(DRAFT_DECISION_NOTICE.getValue())
                .build()).build();

        List<SscsDocument> documents = new ArrayList<>();

        documents.add(draftDecisionNotice);

        if (sscsCaseData.getSscsDocument() != null) {
            documents.addAll(sscsCaseData.getSscsDocument());
        }
        sscsCaseData.setSscsDocument(documents);
    }

    private String createFileName(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getWriteFinalDecisionDocumentFileName() != null) {
            return sscsCaseData.getWriteFinalDecisionDocumentFileName();
        } else {
            return String.format("%s generated on %s.pdf", DRAFT_DECISION_NOTICE.getLabel(), LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        }
    }

    private String setDocumentDateAdded(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getWriteFinalDecisionDocumentDateAdded() != null) {
            return sscsCaseData.getWriteFinalDecisionDocumentDateAdded();
        } else {
            return LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        }
    }
}
