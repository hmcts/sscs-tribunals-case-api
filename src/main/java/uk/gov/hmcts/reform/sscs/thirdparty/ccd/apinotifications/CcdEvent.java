package uk.gov.hmcts.reform.sscs.thirdparty.ccd.apinotifications;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CcdEvent {
    @JsonProperty(value = "case_details")
    private CaseDetails caseDetails;
    @JsonProperty(value = "case_details_before")
    private CaseDetails caseDetailsBefore;

    public CcdEvent(
            @JsonProperty(value = "case_details") CaseDetails caseDetails,
            @JsonProperty(value = "case_details_before") CaseDetails caseDetailsBefore) {
        this.caseDetails = caseDetails;
        this.caseDetailsBefore = caseDetailsBefore;
    }

    public CaseDetails getCaseDetails() {
        return caseDetails;
    }

    public CaseDetails getCaseDetailsBefore() {
        return caseDetailsBefore;
    }
}
