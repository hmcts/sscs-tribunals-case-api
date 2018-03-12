package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Subscriptions {
    Subscription appellantSubscription;
    Subscription supporterSubscription;

    @JsonCreator
    public Subscriptions(@JsonProperty("appellantSubscription") Subscription appellantSubscription,
                        @JsonProperty("supporterSubscription") Subscription supporterSubscription) {
        this.appellantSubscription = appellantSubscription;
        this.supporterSubscription = supporterSubscription;
    }
}
