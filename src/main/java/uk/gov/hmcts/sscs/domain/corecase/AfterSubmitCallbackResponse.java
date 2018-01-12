package uk.gov.hmcts.sscs.domain.corecase;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AfterSubmitCallbackResponse {
    
    @JsonProperty(value = "confirmation_body")
    private String confirmationBody;

    @JsonProperty(value = "confirmation_header")
    private String confirmationHeader;

    public String getConfirmationBody() {
        return confirmationBody;
    }

    public void setConfirmationBody(String confirmationBody) {
        this.confirmationBody = confirmationBody;
    }

    public String getConfirmationHeader() {
        return confirmationHeader;
    }

    public void setConfirmationHeader(String confirmationHeader) {
        this.confirmationHeader = confirmationHeader;
    }

}
