package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppellantDetails {

    private final AddressDetails addressDetails;
    private final String email;
    private final String phone;
    private final String mobile;

    public AppellantDetails(AddressDetails addressDetails, String email, String phone, String mobile) {
        this.addressDetails = addressDetails;
        this.email = email;
        this.phone = phone;
        this.mobile = mobile;
    }

    @JsonProperty(value = "address_details")
    public AddressDetails getAddressDetails() {
        return addressDetails;
    }

    @ApiModelProperty(example = "manish.sharma@gmail.com", required = true)
    @JsonProperty(value = "email")
    public String getEmail() {
        return email;
    }

    @ApiModelProperty(example = "07972438178")
    @JsonProperty(value = "phone")
    public String getPhone() {
        return phone;
    }

    @ApiModelProperty(example = "07972438178")
    @JsonProperty(value = "mobile")
    public String getMobile() {
        return mobile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AppellantDetails that = (AppellantDetails) o;
        return Objects.equals(addressDetails, that.addressDetails)
                && Objects.equals(email, that.email)
                && Objects.equals(phone, that.phone)
                && Objects.equals(mobile, that.mobile);
    }

    @Override
    public int hashCode() {

        return Objects.hash(addressDetails, email, phone, mobile);
    }

    @Override
    public String toString() {
        return "AppellantDetails{"
                + "addressDetails=" + addressDetails
                + ", email='" + email + '\''
                + ", phone='" + phone + '\''
                + ", mobile='" + mobile + '\''
                + '}';
    }
}
