package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDetails {

    private final String type;
    private final String name;
    private final AddressDetails addressDetails;
    private final String email;
    private final String phone;
    private final String mobile;
    private final List<Subscription> subscriptions;

    public UserDetails(String type, String name, AddressDetails addressDetails, String email, String phone, String mobile, List<Subscription> subscriptions) {
        this.type = type;
        this.name = name;
        this.addressDetails = addressDetails;
        this.email = email;
        this.phone = phone;
        this.mobile = mobile;
        this.subscriptions = subscriptions;
    }

    @Schema(example = "Appellant", required = true)
    @JsonProperty(value = "type")
    public String getType() {
        return type;
    }

    @Schema(example = "manish sharma", required = true)
    @JsonProperty(value = "name")
    public String getName() {
        return name;
    }

    @JsonProperty(value = "address_details")
    public AddressDetails getAddressDetails() {
        return addressDetails;
    }

    @Schema(example = "manish.sharma@gmail.com", required = true)
    @JsonProperty(value = "email")
    public String getEmail() {
        return email;
    }

    @Schema(example = "07972438178")
    @JsonProperty(value = "phone")
    public String getPhone() {
        return phone;
    }

    @Schema(example = "07972438178")
    @JsonProperty(value = "mobile")
    public String getMobile() {
        return mobile;
    }

    @JsonProperty(value = "subscriptions")
    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDetails that = (UserDetails) o;
        return Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && Objects.equals(addressDetails, that.addressDetails)
                && Objects.equals(email, that.email)
                && Objects.equals(phone, that.phone)
                && Objects.equals(mobile, that.mobile);
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, name, addressDetails, email, phone, mobile);
    }

    @Override
    public String toString() {
        return "AppellantDetails{"
                + "type=" + type
                + ", name=" + name
                + ", addressDetails=" + addressDetails + '\''
                + ", email='" + email + '\''
                + ", phone='" + phone + '\''
                + ", mobile='" + mobile + '\''
                + '}';
    }
}
