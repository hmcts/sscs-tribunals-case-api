package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

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
            sscsCaseData.getSscsEsaCaseData().setShowRegulation29Page(YesNo.YES);
            if (YesNo.YES.equals(sscsCaseData.getSscsEsaCaseData().getDoesRegulation29Apply())) {
                sscsCaseData.getSscsEsaCaseData().setShowSchedule3ActivitiesPage(YesNo.YES);
            } else if (YesNo.NO.equals(sscsCaseData.getSscsEsaCaseData().getDoesRegulation29Apply())) {
                sscsCaseData.getSscsEsaCaseData().setShowSchedule3ActivitiesPage(YesNo.NO);
            }
        } else {
            sscsCaseData.getSscsEsaCaseData().setShowRegulation29Page(YesNo.NO);
            sscsCaseData.getSscsEsaCaseData().setShowSchedule3ActivitiesPage(YesNo.YES);
        }
    }

    private boolean isWcaNotSupportGroupOnly(SscsCaseData sscsCaseData) {
        return sscsCaseData.getSscsEsaCaseData().isWcaAppeal() && !sscsCaseData.isSupportGroupOnlyAppeal();
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
        if (sscsCaseData.getSscsEsaCaseData().getWcaAppeal() != null && sscsCaseData.getSscsEsaCaseData().getWcaAppeal().equalsIgnoreCase(YesNo.NO.getValue())) {
            sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YesNo.YES);
            return;
        }
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YesNo.NO);
    }
}
