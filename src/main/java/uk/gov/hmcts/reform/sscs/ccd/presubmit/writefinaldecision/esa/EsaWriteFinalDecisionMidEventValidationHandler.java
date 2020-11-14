package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;

@Slf4j
@Component
public class EsaWriteFinalDecisionMidEventValidationHandler extends WriteFinalDecisionMidEventValidationHandlerBase {

    public EsaWriteFinalDecisionMidEventValidationHandler(Validator validator, DecisionNoticeService decisionNoticeService) {
        super(validator, decisionNoticeService);
    }

    @Override
    protected String getBenefitType() {
        return "ESA";
    }

    @Override
    protected void setDefaultFields(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply() == null) {
            sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply("Yes");
        }
    }

    @Override
    protected void setShowPageFlags(SscsCaseData sscsCaseData) {
        int totalPoints = decisionNoticeService.getQuestionService("ESA").getTotalPoints(sscsCaseData, EsaPointsRegulationsAndSchedule3ActivitiesCondition.getAllAnswersExtractor().apply(sscsCaseData));

        if (EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
            sscsCaseData.setShowRegulation29Page(YesNo.YES);
            if (YesNo.YES.equals(sscsCaseData.getDoesRegulation29Apply())) {
                sscsCaseData.setShowSchedule3ActivitiesPage(YesNo.YES);
            } else if (YesNo.NO.equals(sscsCaseData.getDoesRegulation29Apply())) {
                sscsCaseData.setShowSchedule3ActivitiesPage(YesNo.NO);
            }
        } else {
            sscsCaseData.setShowRegulation29Page(YesNo.NO);
            sscsCaseData.setShowSchedule3ActivitiesPage(YesNo.YES);
        }
    }

    @Override
    protected void validateAwardTypes(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        SscsEsaCaseData esaCaseData = sscsCaseData.getSscsEsaCaseData();
        if (esaCaseData.getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion() != null
            || esaCaseData.getEsaWriteFinalDecisionMentalAssessmentQuestion() != null) {

            if ((esaCaseData.getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion() == null
                || esaCaseData.getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion().isEmpty())
                && (esaCaseData.getEsaWriteFinalDecisionMentalAssessmentQuestion() == null
                || esaCaseData.getEsaWriteFinalDecisionMentalAssessmentQuestion().isEmpty())
                && "yes".equalsIgnoreCase(sscsCaseData.getWriteFinalDecisionIsDescriptorFlow())
                && bothDailyLivingAndMobilityQuestionsAnswered(sscsCaseData)) {
                if (isNoAwardOrNotConsideredForDailyLiving(sscsCaseData)
                    && isNoAwardOrNotConsideredForMobility(sscsCaseData)) {
                    if (sscsCaseData.getWriteFinalDecisionEndDateType() != null && !"na".equals(sscsCaseData.getWriteFinalDecisionEndDateType())) {
                        preSubmitCallbackResponse.addError("End date is not applicable for this decision - please specify 'N/A - No Award'.");
                    }
                } else {
                    if ("na".equals(sscsCaseData.getWriteFinalDecisionEndDateType())) {
                        preSubmitCallbackResponse.addError("An end date must be provided or set to Indefinite for this decision.");
                    }
                }
            }
        }
    }

    @Override
    protected void setShowSummaryOfOutcomePage(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getWcaAppeal() != null && sscsCaseData.getWcaAppeal().equalsIgnoreCase(YesNo.NO.getValue())) {
            sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YesNo.YES);
            return;
        }
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YesNo.NO);
    }

    private boolean isNoAwardOrNotConsideredForMobility(SscsCaseData sscsCaseData) {
        return sscsCaseData.getPipWriteFinalDecisionMobilityQuestion() != null
            && ("noAward".equals(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion())
            || "notConsidered".equals(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()));
    }

    private boolean isNoAwardOrNotConsideredForDailyLiving(SscsCaseData sscsCaseData) {
        return sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion() != null
            && ("noAward".equals(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion())
            || "notConsidered".equals(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion()));
    }

    private boolean bothDailyLivingAndMobilityQuestionsAnswered(SscsCaseData caseData) {
        return caseData.getPipWriteFinalDecisionDailyLivingQuestion() != null
            && caseData.getPipWriteFinalDecisionMobilityQuestion() != null;
    }
}
