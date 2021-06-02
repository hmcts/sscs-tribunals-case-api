package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;

import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
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

        if (isWcaNotSupportGroupOnly(sscsCaseData) && EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
            sscsCaseData.getSscsEsaCaseData().setShowRegulation29Page(YES);
            if (YES.equals(sscsCaseData.getSscsEsaCaseData().getDoesRegulation29Apply())) {
                sscsCaseData.getSscsEsaCaseData().setShowSchedule3ActivitiesPage(YES);
            } else if (NO.equals(sscsCaseData.getSscsEsaCaseData().getDoesRegulation29Apply())) {
                sscsCaseData.getSscsEsaCaseData().setShowSchedule3ActivitiesPage(NO);
            }
        } else {
            sscsCaseData.getSscsEsaCaseData().setShowRegulation29Page(NO);
            sscsCaseData.getSscsEsaCaseData().setShowSchedule3ActivitiesPage(YES);
        }
    }

    private boolean isWcaNotSupportGroupOnly(SscsCaseData sscsCaseData) {
        return sscsCaseData.isWcaAppeal() && !sscsCaseData.isSupportGroupOnlyAppeal();
    }

    @Override
    protected void validateAwardTypes(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if ("Yes".equalsIgnoreCase(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply())) {
            if (sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesQuestion() == null
                || sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesQuestion().isEmpty()) {
                preSubmitCallbackResponse.addError("Please select the Schedule 3 Activities that apply, or indicate that none apply");
            }
        }
    }

    @Override
    protected void setShowSummaryOfOutcomePage(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getWcaAppeal() != null && NO.equals(sscsCaseData.getWcaAppeal())) {
            sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YES);
            return;
        }
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(NO);
    }

    @Override
    protected void setShowWorkCapabilityAssessmentPage(SscsCaseData sscsCaseData) {
        if (YES.getValue().equals(sscsCaseData.getWriteFinalDecisionGenerateNotice())) {
            sscsCaseData.setShowWorkCapabilityAssessmentPage(YES);
        }
    }

    @Override
    protected void setDwpReassessAwardPage(SscsCaseData sscsCaseData) {

        if (YesNo.YES.getValue().equalsIgnoreCase(sscsCaseData.getWriteFinalDecisionGenerateNotice())
                && sscsCaseData.isWcaAppeal()
                && "allowed".equalsIgnoreCase(sscsCaseData.getWriteFinalDecisionAllowedOrRefused())) {
            sscsCaseData.setShowDwpReassessAwardPage(YesNo.YES);
            return;
        }
        sscsCaseData.setShowDwpReassessAwardPage(YesNo.NO);
    }
}
