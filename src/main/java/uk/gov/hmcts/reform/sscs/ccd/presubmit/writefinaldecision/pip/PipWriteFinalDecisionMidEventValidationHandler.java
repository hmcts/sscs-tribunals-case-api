package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;

@Slf4j
@Component
public class PipWriteFinalDecisionMidEventValidationHandler extends WriteFinalDecisionMidEventValidationHandlerBase {

    public PipWriteFinalDecisionMidEventValidationHandler(Validator validator,
                                                          DecisionNoticeService decisionNoticeService,
                                                          @Value("${feature.postHearings.enabled}") boolean isPostHearingsEnabled) {
        super(validator, decisionNoticeService, isPostHearingsEnabled);
    }

    @Override
    protected String getBenefitType() {
        return "PIP";
    }

    @Override
    protected void setDefaultFields(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType() == null && "yes"
            .equalsIgnoreCase(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow())) {
            if (bothDailyLivingAndMobilityQuestionsAnswered(sscsCaseData)
                && isNoAwardOrNotConsideredForDailyLiving(sscsCaseData)
                && isNoAwardOrNotConsideredForMobility(sscsCaseData)) {
                sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType("na");
            }
        }
    }

    @Override
    protected void setShowPageFlags(SscsCaseData sscsCaseData) {
        // N/A for PIP
    }

    @Override
    protected void validateAwardTypes(SscsCaseData sscsCaseData,
                                      PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (((!equalsIgnoreCase(AwardType.NO_AWARD.getKey(),
            sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion())
            && !equalsIgnoreCase(AwardType.NOT_CONSIDERED.getKey(),
            sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion()))
            || ((!equalsIgnoreCase(AwardType.NO_AWARD.getKey(),
            sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion())
            && !equalsIgnoreCase(AwardType.NOT_CONSIDERED.getKey(),
            sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion()))))
            && sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingActivitiesQuestion() != null
            && sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityActivitiesQuestion() != null
            && sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingActivitiesQuestion().isEmpty()
            && sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityActivitiesQuestion().isEmpty()) {
            preSubmitCallbackResponse.addError("At least one activity must be selected unless there is no award");
        }

        if (AwardType.NO_AWARD.getKey()
            .equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion())
            && "higher"
            .equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion())) {
            preSubmitCallbackResponse.addError("Daily living decision of No Award cannot be higher than DWP decision");
        }

        if (AwardType.NO_AWARD.getKey()
            .equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion())
            && "higher"
            .equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion())) {
            preSubmitCallbackResponse.addError("Mobility decision of No Award cannot be higher than DWP decision");

        }
        if (AwardType.ENHANCED_RATE.getKey()
            .equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion())
            && "lower"
            .equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion())) {
            preSubmitCallbackResponse.addError("Daily living award at Enhanced Rate cannot be lower than DWP decision");
        }
        if (AwardType.ENHANCED_RATE.getKey()
            .equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion())
            &&
            "lower".equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion())) {
            preSubmitCallbackResponse.addError("Mobility award at Enhanced Rate cannot be lower than DWP decision");
        }

        if (AwardType.NOT_CONSIDERED.getKey()
            .equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion())
            && AwardType.NOT_CONSIDERED.getKey()
            .equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion())) {
            preSubmitCallbackResponse.addError("At least one of Mobility or Daily Living must be considered");
        }

        if ("yes"
            .equalsIgnoreCase(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow())
            && bothDailyLivingAndMobilityQuestionsAnswered(sscsCaseData)) {
            if (isNoAwardOrNotConsideredForDailyLiving(sscsCaseData)
                && isNoAwardOrNotConsideredForMobility(sscsCaseData)) {
                if (sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType() != null
                    && !"na".equals(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType())) {
                    preSubmitCallbackResponse
                        .addError("End date is not applicable for this decision - please specify 'N/A - No Award'.");
                }
            } else {
                if ("na".equals(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType())) {
                    preSubmitCallbackResponse
                        .addError("An end date must be provided or set to Indefinite for this decision.");
                }
            }
        }
    }

    @Override
    protected void setShowSummaryOfOutcomePage(SscsCaseData sscsCaseData, String pageId) {
        if (sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow() != null
            && sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow()
                .equalsIgnoreCase(YesNo.NO.getValue())
            && isYes(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
            sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YesNo.YES);
            return;
        }
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YesNo.NO);
    }

    @Override
    protected void setShowWorkCapabilityAssessmentPage(SscsCaseData sscsCaseData) {
        // N/A for PIP
    }

    @Override
    protected void setDwpReassessAwardPage(SscsCaseData sscsCaseData, String pageId) {
        sscsCaseData.setShowDwpReassessAwardPage(YesNo.NO);
    }

    private boolean isNoAwardOrNotConsideredForMobility(SscsCaseData sscsCaseData) {
        return sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion() != null
            && ("noAward".equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion())
            || "notConsidered".equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion()));
    }

    private boolean isNoAwardOrNotConsideredForDailyLiving(SscsCaseData sscsCaseData) {
        return sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion() != null
            && ("noAward".equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion())
            || "notConsidered".equals(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion()));
    }

    private boolean bothDailyLivingAndMobilityQuestionsAnswered(SscsCaseData caseData) {
        return caseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion() != null
            && caseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion() != null;
    }
}
