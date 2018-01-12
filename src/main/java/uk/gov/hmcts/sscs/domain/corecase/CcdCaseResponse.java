package uk.gov.hmcts.sscs.domain.corecase;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CcdCaseResponse {
    
    @JsonProperty(value = "after_submit_callback_response")
    private AfterSubmitCallbackResponse afterSubmitCallbackResponse;

    @JsonProperty(value = "callback_response_status")
    private String callbackResponseStatus;
    
    @JsonProperty(value = "callback_response_status_code")
    private String callbackResponseStatusCode;
    
    @JsonProperty(value = "case_data")
    private CcdCase caseData;
    
    @JsonProperty(value = "case_type_id")
    private String caseTypeId;
    
    @JsonProperty(value = "created_date")
    private String createdDate;
    
    private int id;
    
    private String jurisdiction;
    
    @JsonProperty(value = "last_modified")
    private String lastModified;
    
    private String state;

    public AfterSubmitCallbackResponse getAfterSubmitCallbackResponse() {
        return afterSubmitCallbackResponse;
    }

    public void setAfterSubmitCallbackResponse(AfterSubmitCallbackResponse afterSubmitCallbackResponse) {
        this.afterSubmitCallbackResponse = afterSubmitCallbackResponse;
    }

    public String getCallbackResponseStatus() {
        return callbackResponseStatus;
    }

    public void setCallbackResponseStatus(String callbackResponseStatus) {
        this.callbackResponseStatus = callbackResponseStatus;
    }

    public String getCallbackResponseStatusCode() {
        return callbackResponseStatusCode;
    }

    public void setCallbackResponseStatusCode(String callbackResponseStatusCode) {
        this.callbackResponseStatusCode = callbackResponseStatusCode;
    }

    public CcdCase getCaseData() {
        return caseData;
    }

    public void setCaseData(CcdCase caseData) {
        this.caseData = caseData;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

}
