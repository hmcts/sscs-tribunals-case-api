package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;

import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;

@Slf4j
@Component
public class UcWriteFinalDecisionMidEventValidationHandler extends WriteFinalDecisionMidEventValidationHandlerBase {

    public UcWriteFinalDecisionMidEventValidationHandler(Validator validator,
                                                         DecisionNoticeService decisionNoticeService,
                                                         @Value("${feature.postHearings.enabled}") boolean isPostHearingsEnabled) {
        super(validator, decisionNoticeService, isPostHearingsEnabled);
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
        return sscsCaseData.isWcaAppeal() && !sscsCaseData.isSupportGroupOnlyAppeal();
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
    protected void setShowSummaryOfOutcomePage(SscsCaseData sscsCaseData, String pageId) {
        // SSCS-9317 There is a strange 'feature' with CCD in that if you change your answers from the summary page, you receive any answers AFTER the flow in the callback as null.
        // If you don't change what you set originally then it remembers what was set before you got to summary page. If you change, then it loses any fields associated with your change if affected by a show condition.
        // In our scenario, if we changed the showFinalDecisionNoticeSummaryOfOutcomePage, the wcaAppeal field comes through as null so the showFinalDecisionNoticeSummaryOfOutcomePage was getting set to NO when
        // coming back from the summary page (instead of YES originally). This means the page show condition related to the showFinalDecisionNoticeSummaryOfOutcomePage is set to NO and any fields lost.
        // Therefore, we need to always skip setting this value, unless the pageId matches what we expect.
        if (pageId != null && pageId.equals("workCapabilityAssessment")) {
            if (sscsCaseData.getWcaAppeal() != null && NO.equals(sscsCaseData.getWcaAppeal())) {
                sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YES);
                return;
            }
            sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(NO);
        }
    }

    @Override
    protected void setShowWorkCapabilityAssessmentPage(SscsCaseData sscsCaseData) {
        sscsCaseData.setShowWorkCapabilityAssessmentPage(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice());
    }

    @Override
    protected void setDwpReassessAwardPage(SscsCaseData sscsCaseData, String pageId) {
        if (pageId != null && pageId.equals("workCapabilityAssessment")) {
            if (isYes(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())
                    && sscsCaseData.isWcaAppeal()
                    && "allowed".equalsIgnoreCase(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused())) {
                sscsCaseData.setShowDwpReassessAwardPage(YesNo.YES);
                return;
            }
            sscsCaseData.setShowDwpReassessAwardPage(YesNo.NO);
        }
    }
}
