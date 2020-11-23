package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

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
public class UcWriteFinalDecisionMidEventValidationHandler extends WriteFinalDecisionMidEventValidationHandlerBase {

    public UcWriteFinalDecisionMidEventValidationHandler(Validator validator, DecisionNoticeService decisionNoticeService) {
        super(validator, decisionNoticeService);
    }

    @Override
    protected String getBenefitType() {
        return "UC";
    }

    @Override
    protected void setDefaultFields(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply() == null) {
            sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("Yes");
        }
    }

    @Override
    protected void setShowPageFlags(SscsCaseData sscsCaseData) {
        int totalPoints = decisionNoticeService.getQuestionService("UC").getTotalPoints(sscsCaseData, UcPointsRegulationsAndSchedule7ActivitiesCondition.getAllAnswersExtractor().apply(sscsCaseData));

        if (isWcaNotSupportGroupOnly(sscsCaseData) && UcPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(totalPoints)) {
            sscsCaseData.getSscsUcCaseData().setShowSchedule8Paragraph4Page(YesNo.YES);
            if (YesNo.YES.equals(sscsCaseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply())) {
                sscsCaseData.getSscsUcCaseData().setShowSchedule7ActivitiesPage(YesNo.YES);
            } else if (YesNo.NO.equals(sscsCaseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply())) {
                sscsCaseData.getSscsUcCaseData().setShowSchedule7ActivitiesPage(YesNo.NO);
            }
        } else {
            sscsCaseData.getSscsUcCaseData().setShowSchedule8Paragraph4Page(YesNo.NO);
            sscsCaseData.getSscsUcCaseData().setShowSchedule7ActivitiesPage(YesNo.YES);
        }
    }

    private boolean isWcaNotSupportGroupOnly(SscsCaseData sscsCaseData) {
        return sscsCaseData.isWcaAppeal() && !sscsCaseData.isSupportGroupOnlyAppeal();
    }

    @Override
    protected void validateAwardTypes(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        // No-op for UC
    }

    @Override
    protected void setShowSummaryOfOutcomePage(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getWcaAppeal() != null && sscsCaseData.getWcaAppeal().equalsIgnoreCase(YesNo.NO.getValue())) {
            sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YesNo.YES);
            return;
        }
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YesNo.NO);
    }
}
