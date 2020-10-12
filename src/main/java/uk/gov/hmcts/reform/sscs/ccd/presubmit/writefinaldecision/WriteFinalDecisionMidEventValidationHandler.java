package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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

    private final Validator validator;

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

        validateAwardTypes(sscsCaseData, preSubmitCallbackResponse);

        if (sscsCaseData.getWriteFinalDecisionEndDateType() == null && "yes".equalsIgnoreCase(sscsCaseData.getWriteFinalDecisionIsDescriptorFlow())) {

            boolean isNaEndDateTypeForBoth = sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion() != null
                && sscsCaseData.getPipWriteFinalDecisionMobilityQuestion() != null
                && isNoAwardOrNotConsideredForDailyLiving(sscsCaseData)
                && isNoAwardOrNotConsideredForMobility(sscsCaseData);

            boolean isNaEndDateTypeForDailyLivingOnly = sscsCaseData.getPipWriteFinalDecisionMobilityQuestion() == null
                && sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion() != null
                && isNoAwardOrNotConsideredForDailyLiving(sscsCaseData);

            boolean isNaEndDateTypeForMobilityOnly = sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion() == null
                && sscsCaseData.getPipWriteFinalDecisionMobilityQuestion() != null
                && isNoAwardOrNotConsideredForMobility(sscsCaseData);

            if (isNaEndDateTypeForBoth || isNaEndDateTypeForDailyLivingOnly || isNaEndDateTypeForMobilityOnly) {
                sscsCaseData.setWriteFinalDecisionEndDateType("na");
            }
        }

        return preSubmitCallbackResponse;
    }

    private boolean isNoAwardOrNotConsideredForDailyLiving(SscsCaseData sscsCaseData) {
        return sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion() != null
            && ("noAward".equals(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion())
            || "notConsidered".equals(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion()));
    }

    private boolean isNoAwardOrNotConsideredForMobility(SscsCaseData sscsCaseData) {
        return sscsCaseData.getPipWriteFinalDecisionMobilityQuestion() != null
            && ("noAward".equals(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion())
            || "notConsidered".equals(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()));
    }

    private void validateAwardTypes(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if ((!equalsIgnoreCase(AwardType.NO_AWARD.getKey(), sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion())
                || !equalsIgnoreCase(AwardType.NO_AWARD.getKey(), sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()))
                &&  sscsCaseData.getPipWriteFinalDecisionDailyLivingActivitiesQuestion() != null
                &&  sscsCaseData.getPipWriteFinalDecisionMobilityActivitiesQuestion() != null
                &&  sscsCaseData.getPipWriteFinalDecisionDailyLivingActivitiesQuestion().isEmpty()
                &&  sscsCaseData.getPipWriteFinalDecisionMobilityActivitiesQuestion().isEmpty()) {
            preSubmitCallbackResponse.addError("At least one activity must be selected unless there is no award");
        }

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

        if ("yes".equalsIgnoreCase(sscsCaseData.getWriteFinalDecisionIsDescriptorFlow())) {
            boolean endDateIsNotApplicable = false;
            if (sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion() != null && sscsCaseData.getPipWriteFinalDecisionMobilityQuestion() != null && isNoAwardOrNotConsideredForDailyLiving(sscsCaseData)
                && isNoAwardOrNotConsideredForMobility(sscsCaseData) && sscsCaseData.getWriteFinalDecisionEndDateType() != null && !"na".equals(sscsCaseData.getWriteFinalDecisionEndDateType())) {
                endDateIsNotApplicable = true;
            } else if (sscsCaseData.getPipWriteFinalDecisionMobilityQuestion() == null && sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion() != null && isNoAwardOrNotConsideredForDailyLiving(sscsCaseData)
                && sscsCaseData.getWriteFinalDecisionEndDateType() != null && !"na".equals(sscsCaseData.getWriteFinalDecisionEndDateType())) {
                endDateIsNotApplicable = true;
            } else if (sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion() == null && sscsCaseData.getPipWriteFinalDecisionMobilityQuestion() != null && isNoAwardOrNotConsideredForMobility(sscsCaseData)
                && sscsCaseData.getWriteFinalDecisionEndDateType() != null && !"na".equals(sscsCaseData.getWriteFinalDecisionEndDateType())) {
                endDateIsNotApplicable = true;
            } else if (((sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion() != null && !isNoAwardOrNotConsideredForDailyLiving(sscsCaseData))
                || (sscsCaseData.getPipWriteFinalDecisionMobilityQuestion() != null && !isNoAwardOrNotConsideredForMobility(sscsCaseData))) && "na".equals(sscsCaseData.getWriteFinalDecisionEndDateType())) {
                preSubmitCallbackResponse.addError("An end date must be provided set to indefinite for this decision");
            }

            if (endDateIsNotApplicable) {
                preSubmitCallbackResponse.addError("End date is not applicable for this decision");
            }
        }

    }

    private boolean isDecisionNoticeDatesInvalid(SscsCaseData sscsCaseData) {
        if (isNotBlank(sscsCaseData.getWriteFinalDecisionStartDate()) && isNotBlank(sscsCaseData.getWriteFinalDecisionEndDate())) {
            LocalDate decisionNoticeStartDate = LocalDate.parse(sscsCaseData.getWriteFinalDecisionStartDate());
            LocalDate decisionNoticeEndDate = LocalDate.parse(sscsCaseData.getWriteFinalDecisionEndDate());
            return !decisionNoticeStartDate.isBefore(decisionNoticeEndDate);
        }
        return false;
    }

}
