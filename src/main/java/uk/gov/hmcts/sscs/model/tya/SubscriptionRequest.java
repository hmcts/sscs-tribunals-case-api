package uk.gov.hmcts.sscs.model.tya;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.hibernate.validator.constraints.Email;

@JsonRootName(value = "subscription")
@JsonInclude(value = NON_EMPTY)
public class SubscriptionRequest {
    @Email(message = "Invalid email")
    private String email;

    @Size(min = 10, max = 13, message = "Mobile number length should be between 10 and 11 digits")
    @Pattern(regexp = "^(|[+][0-9]{10,13})$")
    private String mobileNumber;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }
}
