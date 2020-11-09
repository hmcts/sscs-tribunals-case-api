package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

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
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@Component
@Slf4j
public class WriteFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DecisionNoticeService decisionNoticeService;
    private final PreviewDocumentService previewDocumentService;

    @Autowired
    public WriteFinalDecisionAboutToSubmitHandler(DecisionNoticeService decisionNoticeService,
                                                  PreviewDocumentService previewDocumentService) {
        this.decisionNoticeService = decisionNoticeService;
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

        if ("na".equals(sscsCaseData.getWriteFinalDecisionEndDateType())) {
            sscsCaseData.setWriteFinalDecisionEndDateType(null);
        }

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        String benefitType = sscsCaseData.getAppeal().getBenefitType() == null ? null : sscsCaseData.getAppeal().getBenefitType().getCode();

        if (benefitType == null) {
            preSubmitCallbackResponse.addError("Unexpected error - benefit type is null");
        } else {

            DecisionNoticeOutcomeService outcomeService = decisionNoticeService.getOutcomeService(benefitType);

            // Due to a bug with CCD related to hidden fields, hidden fields are not being unset
            // on the final submission from CCD, so we need to reset them here
            // See https://tools.hmcts.net/jira/browse/RDM-8200
            // This is a temporary workaround for this issue.
            outcomeService.performPreOutcomeIntegrityAdjustments(sscsCaseData);

            DecisionNoticeQuestionService questionService = decisionNoticeService.getQuestionService(benefitType);

            List<String> validationErrorMessages = new ArrayList<>();
            for (Class<? extends PointsCondition<?>> pointsConditionEnumClass : questionService.getPointsConditionEnumClasses()) {
                if (validationErrorMessages.isEmpty()) {
                    getDecisionNoticePointsValidationErrorMessages(pointsConditionEnumClass, questionService, sscsCaseData)
                        .forEach(validationErrorMessages::add);
                }
            }

            validationErrorMessages.stream().forEach(preSubmitCallbackResponse::addError);

            if (validationErrorMessages.isEmpty()) {


                // Validate that we can determine an outcome
                Outcome outcome = outcomeService.determineOutcomeWithValidation(preSubmitCallbackResponse.getData());
                if ("ESA".equals(benefitType) && outcome == null) {
                    throw new IllegalStateException("Unable to determine a validated outcome");
                }

            }

            previewDocumentService.writePreviewDocumentToSscsDocument(sscsCaseData, DRAFT_DECISION_NOTICE, sscsCaseData.getWriteFinalDecisionPreviewDocument());
        }
        return preSubmitCallbackResponse;
    }

    private <T extends PointsCondition<?>> List<String> getDecisionNoticePointsValidationErrorMessages(Class<T> enumType, DecisionNoticeQuestionService decisionNoticeQuestionService, SscsCaseData sscsCaseData) {

        return Arrays.stream(enumType.getEnumConstants())
            .filter(pointsCondition -> pointsCondition.isApplicable(
                decisionNoticeQuestionService, sscsCaseData))
            .map(pointsCondition ->
                pointsCondition.getOptionalErrorMessage(decisionNoticeQuestionService, sscsCaseData))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
}
