package uk.gov.hmcts.reform.sscs.tyanotifications.personalisation;

import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.getSubscription;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;

@Component
@Slf4j
public class SubscriptionPersonalisation extends WithRepresentativePersonalisation {

    @Override
    protected Map<String, Object> create(NotificationSscsCaseDataWrapper responseWrapper, SubscriptionWithType subscriptionWithType) {
        Subscription newSubscription = subscriptionWithType.getSubscription();
        Subscription oldSubscription = getSubscription(responseWrapper.getOldSscsCaseData(), subscriptionWithType.getSubscriptionType());
        setSendSmsSubscriptionConfirmation(shouldSendSmsSubscriptionConfirmation(newSubscription, oldSubscription));
        unsetMobileAndEmailIfUnchanged(subscriptionWithType, oldSubscription);

        return super.create(responseWrapper, subscriptionWithType);
    }

    Boolean shouldSendSmsSubscriptionConfirmation(Subscription newSubscription, Subscription oldSubscription) {
        if (oldSubscription != null && newSubscription != null) {
            return ((!oldSubscription.isSmsSubscribed() && newSubscription.isSmsSubscribed())
                || !StringUtils.equalsIgnoreCase(newSubscription.getMobile(), oldSubscription.getMobile()));
        } else {
            return false;
        }
    }

    void unsetMobileAndEmailIfUnchanged(SubscriptionWithType subscriptionWithType, Subscription oldSubscription) {
        if (isSubscriptionUnchanged(subscriptionWithType.getSubscription(), oldSubscription)) {
            log.info("The subscription has not changed and so will not send any notification to " + subscriptionWithType.getSubscriptionType());
            subscriptionWithType.setSubscription(subscriptionWithType.getSubscription().toBuilder().mobile(null).email(null).build());
        } else if (isEmailSubscriptionUnchanged(subscriptionWithType.getSubscription(), oldSubscription)) {
            subscriptionWithType.setSubscription(subscriptionWithType.getSubscription().toBuilder().email(null).build());
        } else if (isSmsSubscriptionUnchanged(subscriptionWithType.getSubscription(), oldSubscription)) {
            subscriptionWithType.setSubscription(subscriptionWithType.getSubscription().toBuilder().mobile(null).build());
        }
    }

    private boolean isSmsSubscriptionUnchanged(Subscription newSubscription, Subscription oldSubscription) {
        return oldSubscription != null && newSubscription != null
            && newSubscription.isSmsSubscribed().equals(oldSubscription.isSmsSubscribed())
            && StringUtils.equalsIgnoreCase(newSubscription.getMobile(), oldSubscription.getMobile());
    }

    private boolean isEmailSubscriptionUnchanged(Subscription newSubscription, Subscription oldSubscription) {
        return oldSubscription != null && newSubscription != null
            && newSubscription.isEmailSubscribed().equals(oldSubscription.isEmailSubscribed())
            && StringUtils.equalsIgnoreCase(newSubscription.getEmail(), oldSubscription.getEmail());
    }

    private boolean isSubscriptionUnchanged(Subscription newSubscription, Subscription oldSubscription) {
        return isEmailSubscriptionUnchanged(newSubscription, oldSubscription)
            && isSmsSubscriptionUnchanged(newSubscription, oldSubscription);
    }
}
