package uk.gov.hmcts.reform.sscs.tyanotifications.factory;

import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

public interface NotificationWrapper {
    NotificationEventType getNotificationType();

    void setNotificationType(NotificationEventType notificationEventType);

    SscsCaseData getNewSscsCaseData();

    String getCaseId();

    Subscription getAppellantSubscription();

    Subscription getRepresentativeSubscription();

    Subscription getAppointeeSubscription();

    Subscription getJointPartySubscription();

    List<SubscriptionWithType> getOtherPartySubscriptions(SscsCaseData sscsCaseData, NotificationEventType notificationEventType);

    SscsCaseDataWrapper getSscsCaseDataWrapper();

    AppealHearingType getHearingType();

    String getSchedulerPayload();

    SscsCaseData getOldSscsCaseData();

    List<SubscriptionWithType> getSubscriptionsBasedOnNotificationType();

    void setNotificationEventTypeOverridden(boolean notificationEventTypeOverridden);

    boolean hasNotificationEventBeenOverridden();

    void setSwitchLanguageType(boolean languageSwitched);

    boolean hasLanguageSwitched();
}
