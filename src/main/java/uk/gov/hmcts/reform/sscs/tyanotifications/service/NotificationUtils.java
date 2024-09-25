package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.EVENT_TYPES_FOR_MANDATORY_LETTERS;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.*;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;

@Slf4j
public class NotificationUtils {

    private NotificationUtils() {
        // empty
    }

    /* Sometimes the data for the appointee comes in with null populated objects */
    public static boolean hasAppointee(NotificationSscsCaseDataWrapper wrapper) {
        return wrapper.getNewSscsCaseData().getAppeal() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant() != null
            && hasAppointee(wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee(),
            wrapper.getNewSscsCaseData().getAppeal().getAppellant().getIsAppointee());
    }

    public static boolean hasAppointee(Appointee appointee, String isAppointee) {
        return !equalsIgnoreCase(isAppointee, "No") && appointee != null && appointee.getName() != null && appointee.getName().getFirstName() != null
            && appointee.getName().getLastName() != null;
    }

    /* Sometimes the data for the appointee comes in with null populated objects */
    public static boolean hasRepresentative(NotificationSscsCaseDataWrapper wrapper) {
        return wrapper.getNewSscsCaseData().getAppeal() != null
            && hasRepresentative(wrapper.getNewSscsCaseData().getAppeal());
    }

    public static boolean hasRepresentative(Appeal appeal) {
        return appeal.getRep() != null
            && appeal.getRep().getHasRepresentative() != null
            && appeal.getRep().getHasRepresentative().equalsIgnoreCase("yes");
    }

    public static boolean hasRepresentative(OtherParty otherParty) {
        return otherParty.getRep() != null
            && otherParty.getRep().getHasRepresentative() != null
            && otherParty.getRep().getHasRepresentative().equalsIgnoreCase("yes");
    }

    public static boolean hasJointParty(SscsCaseData caseData) {
        return caseData.isThereAJointParty()
            && isNotBlank(trimToNull(caseData.getJointParty().getName().getFullName()));
    }

    public static boolean hasAppointeeSubscriptionOrIsMandatoryAppointeeLetter(NotificationSscsCaseDataWrapper wrapper) {
        Subscription subscription = getSubscription(wrapper.getNewSscsCaseData(), APPOINTEE);
        return hasAppointee(wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee(),
            wrapper.getNewSscsCaseData().getAppeal().getAppellant().getIsAppointee())
            && ((nonNull(subscription) && subscription.doesCaseHaveSubscriptions())
            || EVENT_TYPES_FOR_MANDATORY_LETTERS.contains(wrapper.getNotificationEventType()));
    }

    public static boolean hasRepSubscriptionOrIsMandatoryRepLetter(NotificationSscsCaseDataWrapper wrapper) {
        Subscription subscription = getSubscription(wrapper.getNewSscsCaseData(), REPRESENTATIVE);
        boolean hasRepresentative = hasRepresentative(wrapper.getNewSscsCaseData().getAppeal());
        boolean hasRepSubscription = hasRepresentative && (null != subscription && subscription.doesCaseHaveSubscriptions());
        boolean hasMandatoryLetter = (hasRepresentative(wrapper.getNewSscsCaseData().getAppeal())
            && EVENT_TYPES_FOR_MANDATORY_LETTERS.contains(wrapper.getNotificationEventType()));

        return (hasRepSubscription || hasMandatoryLetter);
    }

    public static boolean hasJointPartySubscription(NotificationSscsCaseDataWrapper wrapper) {
        Subscription subscription = getSubscription(wrapper.getNewSscsCaseData(), JOINT_PARTY);
        return ((null != subscription && subscription.doesCaseHaveSubscriptions() && hasJointParty(wrapper.getNewSscsCaseData()))
            || (hasJointParty(wrapper.getNewSscsCaseData())
            && EVENT_TYPES_FOR_MANDATORY_LETTERS.contains(wrapper.getNotificationEventType())));
    }

    public static boolean isValidSubscriptionOrIsMandatoryLetter(Subscription subscription, NotificationEventType eventType) {
        Subscription nullCheckedSubscription = getPopulatedSubscriptionOrNull(subscription);
        return ((null != nullCheckedSubscription && nullCheckedSubscription.doesCaseHaveSubscriptions())
            || EVENT_TYPES_FOR_MANDATORY_LETTERS.contains(eventType));
    }

    public static Subscription getSubscription(SscsCaseData sscsCaseData, SubscriptionType subscriptionType) {
        if (REPRESENTATIVE.equals(subscriptionType)) {
            return getPopulatedSubscriptionOrNull(sscsCaseData.getSubscriptions().getRepresentativeSubscription());
        } else if (APPELLANT.equals(subscriptionType)) {
            return getPopulatedSubscriptionOrNull(sscsCaseData.getSubscriptions().getAppellantSubscription());
        } else if (JOINT_PARTY.equals(subscriptionType)) {
            return getPopulatedSubscriptionOrNull(sscsCaseData.getSubscriptions().getJointPartySubscription());
        } else {
            return getPopulatedSubscriptionOrNull(sscsCaseData.getSubscriptions().getAppointeeSubscription());
        }
    }

    private static Subscription getPopulatedSubscriptionOrNull(Subscription subscription) {
        if (null == subscription
            || (null == subscription.getWantSmsNotifications()
            && null == subscription.getTya()
            && null == subscription.getEmail()
            && null == subscription.getMobile()
            && null == subscription.getSubscribeEmail()
            && null == subscription.getSubscribeSms()
            && null == subscription.getReason())
        ) {
            return null;
        }

        return subscription;
    }

    static boolean isOkToSendNotification(NotificationWrapper wrapper, NotificationEventType notificationType,
                                          Subscription subscription,
                                          NotificationValidService notificationValidService) {
        return ((subscription != null
            && subscription.doesCaseHaveSubscriptions()))
            && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
            && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType);
    }


    protected static boolean isOkToSendSmsNotification(NotificationWrapper wrapper, Subscription subscription,
                                                       Notification notification, NotificationEventType notificationType,
                                                       NotificationValidService notificationValidService) {
        return subscription != null
            && subscription.isSmsSubscribed()
            && notification.isSms()
            && subscription.doesCaseHaveSubscriptions()
            && notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), notificationType)
            && notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), notificationType);
    }

    protected static boolean isOkToSendEmailNotification(NotificationWrapper wrapper, Subscription subscription,
                                                         Notification notification,
                                                         NotificationValidService notificationValidService) {
        return subscription != null
            && subscription.isEmailSubscribed()
            && notification.isEmail()
            && notification.getEmailTemplate() != null
            && isOkToSendNotification(wrapper, wrapper.getNotificationType(), subscription, notificationValidService);
    }

    static boolean hasLetterTemplate(Notification notification) {
        return notification.getLetterTemplate() != null;
    }

    static boolean hasNoSubscriptions(Subscription subscription) {
        return subscription == null || (!subscription.isSmsSubscribed() && !subscription.isEmailSubscribed());
    }

    static boolean hasSubscription(NotificationWrapper wrapper, SubscriptionType subscriptionType) {
        return APPELLANT.equals(subscriptionType)
            || APPOINTEE.equals(subscriptionType)
            || JOINT_PARTY.equals(subscriptionType)
            || (REPRESENTATIVE.equals(subscriptionType) && null != wrapper.getNewSscsCaseData().getAppeal().getRep());
    }

    public static NotificationSscsCaseDataWrapper buildSscsCaseDataWrapper(SscsCaseData caseData, SscsCaseData caseDataBefore, NotificationEventType event, State state) {
        return NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(caseData)
            .oldSscsCaseData(caseDataBefore)
            .notificationEventType(event)
            .state(state).build();
    }
}
