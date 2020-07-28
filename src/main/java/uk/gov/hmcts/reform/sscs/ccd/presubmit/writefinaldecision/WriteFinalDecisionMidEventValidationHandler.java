package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.isFileAPdf;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class WriteFinalDecisionMidEventValidationHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private Validator validator;

    @Autowired
    WriteFinalDecisionMidEventValidationHandler(Validator validator) {
        this.validator = validator;
    }
    
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
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

        Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
        for (ConstraintViolation<SscsCaseData> violation : violations) {
            preSubmitCallbackResponse.addError(violation.getMessage());
        }

        if (isDecisionNoticeDatesInvalid(sscsCaseData)) {
            preSubmitCallbackResponse.addError("Decision notice end date must be after decision notice start date");
        }

        if (sscsCaseData.getWriteFinalDecisionPreviewDocument() != null && !isFileAPdf(sscsCaseData.getWriteFinalDecisionPreviewDocument())) {
            preSubmitCallbackResponse.addError("You need to upload PDF documents only");
            return preSubmitCallbackResponse;
        }

        validateAwardTypes(sscsCaseData, preSubmitCallbackResponse);

        return preSubmitCallbackResponse;
    }

    private void validateAwardTypes(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (AwardType.NO_AWARD.getKey().equals(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion())
            && "higher".equals(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion())) {
            preSubmitCallbackResponse.addError("Daily living decision of No Award cannot be higher than DWP decision");
        }

        if (AwardType.NO_AWARD.getKey().equals(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion())
            && "higher".equals(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion())) {
            preSubmitCallbackResponse.addError("Mobility decision of No Award cannot be higher than DWP decision");

        }
        if (AwardType.ENHANCED_RATE.getKey().equals(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion())
            && "lower".equals(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion())) {
            preSubmitCallbackResponse.addError("Daily living award at Enhanced Rate cannot be lower than DWP decision");
        }
        if (AwardType.ENHANCED_RATE.getKey().equals(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion())
            && "lower".equals(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion())) {
            preSubmitCallbackResponse.addError("Mobility award at Enhanced Rate cannot be lower than DWP decision");
        }

        if (AwardType.NOT_CONSIDERED.getKey().equals(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion())
            && AwardType.NOT_CONSIDERED.getKey().equals(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion())) {
            preSubmitCallbackResponse.addError("At least one of Mobility or Daily Living must be considered");
        }

    }

    private boolean isDecisionNoticeDatesInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getWriteFinalDecisionStartDate() != null && sscsCaseData.getWriteFinalDecisionEndDate() != null) {
            LocalDate decisionNoticeStartDate = LocalDate.parse(sscsCaseData.getWriteFinalDecisionStartDate());
            LocalDate decisionNoticeEndDate = LocalDate.parse(sscsCaseData.getWriteFinalDecisionEndDate());
            return !decisionNoticeStartDate.isBefore(decisionNoticeEndDate);
        }
        return false;
    }

    private boolean isDecisionNoticeDateOfDecisionInvalid(SscsCaseData sscsCaseData) {
        if (!isBlank(sscsCaseData.getWriteFinalDecisionDateOfDecision())) {
            LocalDate decisionNoticeDecisionDate = LocalDate.parse(sscsCaseData.getWriteFinalDecisionDateOfDecision());
            LocalDate today = LocalDate.now();
            return decisionNoticeDecisionDate.isAfter(today);
        }
        return false;
    }

}
