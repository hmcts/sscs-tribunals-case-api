package uk.gov.hmcts.reform.sscs.tyanotifications.factory;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCodeOrThrowException;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.personalisation.Personalisation;
import uk.gov.hmcts.reform.sscs.utility.PhoneNumbersUtil;

@Component
@Slf4j
public class NotificationFactory {

    private final PersonalisationFactory personalisationFactory;
    private final Map<NotificationEventType, Personalisation> map = newHashMap();

    NotificationFactory(PersonalisationFactory personalisationFactory) {
        this.personalisationFactory = personalisationFactory;
    }

    public <E extends NotificationWrapper> Notification create(E notificationWrapper,
                                                               SubscriptionWithType subscriptionWithType) {
        Personalisation<E> personalisation = getPersonalisation(notificationWrapper);
        if (personalisation == null) {
            return null;
        }

        Map<String, Object> placeholders = personalisation.create(notificationWrapper, subscriptionWithType);
        if (null == placeholders) {
            return null;
        }

        Benefit benefit = null;
        if (notificationWrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getAppeal().getBenefitType() != null
            && !StringUtils.isEmpty(notificationWrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getAppeal().getBenefitType().getCode())) {
            benefit = getBenefitByCodeOrThrowException(notificationWrapper
                .getSscsCaseDataWrapper().getNewSscsCaseData().getAppeal().getBenefitType().getCode());
        }
        Template template = personalisation.getTemplate(notificationWrapper, benefit, subscriptionWithType.getSubscriptionType());

        SscsCaseData ccdResponse = notificationWrapper.getSscsCaseDataWrapper().getNewSscsCaseData();

        Subscription subscription = subscriptionWithType.getSubscription();
        Destination destination = getDestination(subscription);
        Reference reference = new Reference(ccdResponse.getCaseReference());
        String appealNumber = tya(subscription);

        return new Notification(template, destination, placeholders, reference, appealNumber);
    }

    private static String tya(Subscription subscription) {
        if (subscription != null) {
            return StringUtils.defaultIfBlank(subscription.getTya(), StringUtils.EMPTY);
        } else {
            return StringUtils.EMPTY;
        }
    }

    private <E extends NotificationWrapper> Personalisation<E> getPersonalisation(E notificationWrapper) {
        //noinspection unchecked
        return map.computeIfAbsent(notificationWrapper.getNotificationType(), personalisationFactory);
    }

    private Destination getDestination(Subscription subscription) {
        if (subscription != null) {
            return Destination.builder()
                .email(subscription.getEmail())
                .sms(PhoneNumbersUtil.cleanPhoneNumber(subscription.getMobile()).orElse(subscription.getMobile()))
                .build();
        } else {
            return Destination.builder().build();
        }
    }
}
