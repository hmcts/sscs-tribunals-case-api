package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;

import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
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
            sscsCaseData.getSscsUcCaseData().setShowSchedule8Paragraph4Page(YES);
            if (YES.equals(sscsCaseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply())) {
                sscsCaseData.getSscsUcCaseData().setShowSchedule7ActivitiesPage(YES);
            } else if (NO.equals(sscsCaseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply())) {
                sscsCaseData.getSscsUcCaseData().setShowSchedule7ActivitiesPage(NO);
            }
        } else {
            sscsCaseData.getSscsUcCaseData().setShowSchedule8Paragraph4Page(NO);
            sscsCaseData.getSscsUcCaseData().setShowSchedule7ActivitiesPage(YES);
        }
    }

    private boolean isWcaNotSupportGroupOnly(SscsCaseData sscsCaseData) {
        return sscsCaseData.getSscsUcCaseData().isLcwaAppeal() && !sscsCaseData.isSupportGroupOnlyAppeal();
    }

    @Override
    protected void validateAwardTypes(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if ("Yes".equalsIgnoreCase(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply())) {
            if (sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesQuestion() == null
                || sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesQuestion().isEmpty()) {
                preSubmitCallbackResponse.addError("Please select the Schedule 7 Activities that apply, or indicate that none apply");
            }
        }
    }

    @Override
    protected void setShowSummaryOfOutcomePage(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getSscsUcCaseData().getLcwaAppeal() != null && NO.equals(sscsCaseData.getSscsUcCaseData().getLcwaAppeal())) {
            sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YES);
            return;
        }
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(NO);
    }
}
