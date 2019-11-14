package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class ResponseEventsAboutToSubmit {

    public void checkMandatoryFields(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, SscsCaseData sscsCaseData) {
        if (sscsCaseData.getBenefitCode() == null) {
            preSubmitCallbackResponse.addError("Benefit code cannot be empty");
        }
        if (sscsCaseData.getIssueCode() == null) {
            preSubmitCallbackResponse.addError("Issue code cannot be empty");
        }
    }

    public void setCaseCode(SscsCaseData sscsCaseData) {
        sscsCaseData.setCaseCode(buildCaseCode(sscsCaseData));
    }

    private String buildCaseCode(SscsCaseData sscsCaseData) {
        return sscsCaseData.getBenefitCode() + sscsCaseData.getIssueCode();
    }
}
