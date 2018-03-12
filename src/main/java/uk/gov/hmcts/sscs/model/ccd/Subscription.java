package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Subscription {
    String tya;
    String email;
    String mobile;
    String subscribeEmail;
    String subscribeSms;
    String reason;

    @JsonCreator
    public Subscription(@JsonProperty("tya") String tya,
                @JsonProperty("email") String email,
                @JsonProperty("mobile") String mobile,
                @JsonProperty("subscribeEmail") String subscribeEmail,
                @JsonProperty("subscribeSms") String subscribeSms,
                @JsonProperty("reason") String reason) {
        this.tya = tya;
        this.email = email;
        this.mobile = mobile;
        this.subscribeEmail = subscribeEmail;
        this.subscribeSms = subscribeSms;
        this.reason = reason;
    }
}
