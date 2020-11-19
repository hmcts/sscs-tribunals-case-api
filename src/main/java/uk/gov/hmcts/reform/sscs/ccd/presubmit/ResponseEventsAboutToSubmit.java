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
        } else if (sscsCaseData.getIssueCode().equals("DD")) {
            preSubmitCallbackResponse.addError("Issue code cannot be set to the default value of DD");
        }
    }

    public void setCaseCode(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAppeal().getBenefitType() != null && "uc".equalsIgnoreCase(sscsCaseData.getAppeal().getBenefitType().getCode())) {
            setUcCaseCode(sscsCaseData);
        } else if (sscsCaseData.getBenefitCode() != null && sscsCaseData.getIssueCode() != null) {
            sscsCaseData.setCaseCode(buildCaseCode(sscsCaseData));
        }
    }

    private String buildCaseCode(SscsCaseData sscsCaseData) {
        return sscsCaseData.getBenefitCode() + sscsCaseData.getIssueCode();
    }

    private void setUcCaseCode(SscsCaseData sscsCaseData) {
        boolean multiElementAppeal = null != sscsCaseData.getElementsDisputedList() && sscsCaseData.getElementsDisputedList().size() > 1;
        String issueCode = multiElementAppeal ? "UM" : "US";

        sscsCaseData.setIssueCode(issueCode);
        sscsCaseData.setBenefitCode("001");
        sscsCaseData.setCaseCode("001" + issueCode);
    }
}
