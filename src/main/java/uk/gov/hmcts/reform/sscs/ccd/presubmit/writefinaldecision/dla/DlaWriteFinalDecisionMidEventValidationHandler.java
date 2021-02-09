package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla;

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
public class DlaWriteFinalDecisionMidEventValidationHandler extends WriteFinalDecisionMidEventValidationHandlerBase {

    public DlaWriteFinalDecisionMidEventValidationHandler(Validator validator, DecisionNoticeService decisionNoticeService) {
        super(validator, decisionNoticeService);
    }

    @Override
    protected String getBenefitType() {
        return "DLA";
    }

    @Override
    protected void setDefaultFields(SscsCaseData sscsCaseData) {
        // N/A for DLA
    }

    @Override
    protected void setShowPageFlags(SscsCaseData sscsCaseData) {
        // N/A for DLA
    }

    @Override
    protected void validateAwardTypes(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        // N/A for DLA
    }

    @Override
    protected void setShowSummaryOfOutcomePage(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getWriteFinalDecisionIsDescriptorFlow() != null && sscsCaseData.getWriteFinalDecisionIsDescriptorFlow().equalsIgnoreCase(YesNo.NO.getValue())
            && sscsCaseData.getWriteFinalDecisionGenerateNotice() != null && sscsCaseData.getWriteFinalDecisionGenerateNotice().equalsIgnoreCase(YesNo.YES.getValue())) {
            sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YesNo.YES);
            return;
        }
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(YesNo.NO);
    }

    @Override
    protected void setDwpReassessAwardPage(SscsCaseData sscsCaseData) {
        sscsCaseData.setShowDwpReassessAwardPage(YesNo.NO);
    }
}
