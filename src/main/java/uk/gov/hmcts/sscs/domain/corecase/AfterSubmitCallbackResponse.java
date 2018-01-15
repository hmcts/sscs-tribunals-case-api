package uk.gov.hmcts.sscs.domain.corecase;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

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

    @Override
    public String toString() {
        return "AfterSubmitCallbackResponse{"
            + "confirmationBody='" + confirmationBody + '\''
            + ", confirmationHeader='" + confirmationHeader + '\''
            + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AfterSubmitCallbackResponse)) {
            return false;
        }
        AfterSubmitCallbackResponse that = (AfterSubmitCallbackResponse) o;
        return Objects.equal(confirmationBody, that.confirmationBody)
            && Objects.equal(confirmationHeader, that.confirmationHeader);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(confirmationBody, confirmationHeader);
    }
}
