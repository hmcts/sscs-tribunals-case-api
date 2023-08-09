package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_IN_FAVOUR_OF_APPELLANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Outcome.DECISION_UPHELD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.PointsCondition;

@Slf4j
public abstract class DecisionNoticeOutcomeService {

    private String benefitType;
    protected DecisionNoticeQuestionService questionService;

    protected DecisionNoticeOutcomeService(String benefitType, DecisionNoticeQuestionService questionService) {
        this.benefitType = benefitType;
        this.questionService = questionService;
    }

    public String getBenefitType() {
        return benefitType;
    }

    public abstract Outcome determineOutcome(SscsCaseData sscsCaseData);

    /**
     * Due to a bug with CCD related to hidden fields, hidden fields are not being unset
     * on the final submission from CCD, so we need to reset them here
     * See https://tools.hmcts.net/jira/browse/RDM-8200
     * This method provides a hook to temporarily workaround this issue, and allow
     * hidden fields to be unset.
     *
     */
    public abstract void performPreOutcomeIntegrityAdjustments(SscsCaseData sscsCaseData);

    public abstract Outcome determineOutcomeWithValidation(SscsCaseData sscsCaseData);

    protected Outcome useExplicitySetOutcome(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused() == null) {
            return null;
        } else {
            if ("allowed".equalsIgnoreCase(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused())) {
                return DECISION_IN_FAVOUR_OF_APPELLANT;
            } else {
                return DECISION_UPHELD;
            }
        }
    }

    public void validate(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, SscsCaseData sscsCaseData) {

        // Due to a bug with CCD related to hidden fields, hidden fields are not being unset
        // on the final submission from CCD, so we need to reset them here
        // See https://tools.hmcts.net/jira/browse/RDM-8200
        // This is a temporary workaround for this issue.
        performPreOutcomeIntegrityAdjustments(sscsCaseData);

        List<String> validationErrorMessages = new ArrayList<>();
        if (questionService.getPointsConditionEnumClasses() != null) {
            for (Class<? extends PointsCondition<?>> pointsConditionEnumClass : questionService.getPointsConditionEnumClasses()) {
                if (validationErrorMessages.isEmpty()) {
                    validationErrorMessages.addAll(getDecisionNoticePointsValidationErrorMessages(pointsConditionEnumClass, questionService, sscsCaseData));
                }
            }
        }

        validationErrorMessages.stream().forEach(preSubmitCallbackResponse::addError);

        if (validationErrorMessages.isEmpty()) {

            // Validate that we can determine an outcome
            Outcome outcome = determineOutcomeWithValidation(preSubmitCallbackResponse.getData());
            if (("ESA".equals(getBenefitType()) || "UC".equals(getBenefitType())) && outcome == null) {
                throw new IllegalStateException("Unable to determine a validated outcome");
            }
        }
    }

    private <T extends PointsCondition<?>> List<String> getDecisionNoticePointsValidationErrorMessages(Class<T> enumType, DecisionNoticeQuestionService decisionNoticeQuestionService, SscsCaseData sscsCaseData) {
        return Arrays.stream(enumType.getEnumConstants())
            .filter(pointsCondition -> pointsCondition.isApplicable(
                decisionNoticeQuestionService, sscsCaseData))
            .map(pointsCondition ->
                pointsCondition.getOptionalErrorMessage(decisionNoticeQuestionService, sscsCaseData))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }
}
