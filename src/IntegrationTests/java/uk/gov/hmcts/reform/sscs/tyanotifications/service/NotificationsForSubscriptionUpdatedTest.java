package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.service.notify.NotificationClientException;

public class NotificationsForSubscriptionUpdatedTest extends NotificationProcessingServiceBaseTest {

    @Value("${notification.english.appealReceived.appellant.emailId}")
    private String appealReceivedAppellantEmailId;

    @Value("${notification.english.appealReceived.appellant.smsId}")
    private String appealReceivedAppellantSmsId;

    @Value("${notification.english.appealReceived.representative.emailId}")
    private String appealReceivedRepresentativeEmailId;

    @Value("${notification.english.appealReceived.representative.smsId}")
    private String appealReceivedRepresentativeSmsId;

    @Value("${notification.english.appealReceived.appointee.emailId}")
    private String appealReceivedAppointeeEmailId;

    @Value("${notification.english.appealReceived.appointee.smsId}")
    private String appealReceivedAppointeeSmsId;

    @Value("${notification.english.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailId;

    @Value("${notification.english.subscriptionUpdated.smsId}")
    private String subscriptionUpdatedSmsId;

    @Value("${notification.english.subscriptionCreated.appellant.smsId}")
    private String subscriptionCreatedAppellantSmsId;

    @Value("${notification.english.subscriptionCreated.appointee.smsId}")
    private String subscriptionCreatedAppointeeSmsId;

    @Value("${notification.english.subscriptionCreated.representative.smsId}")
    private String subscriptionCreatedRepresentativeSmsId;

    @Value("${notification.english.subscriptionOld.emailId}")
    private String subscriptionOldEmailId;

    @Value("${notification.english.subscriptionOld.smsId}")
    private String subscriptionOldSmsId;

    @ParameterizedTest
    @ValueSource(strings = {"appellant", "representative", "appointee"})
    public void unsubscribeFromSmsAndEmail_doesNotSendAnyEmailsOrSms(String who) {
        Subscription newSubscription = getSubscription().toBuilder()
            .subscribeEmail(NotificationProcessingServiceBaseTest.NO)
            .subscribeSms(NotificationProcessingServiceBaseTest.NO)
            .build();
        doUnsubscribeWithAssertions(newSubscription, who);
    }

    @ParameterizedTest
    @ValueSource(strings = {"appellant", "representative", "appointee"})
    public void unsubscribeFromEmail_doesNotSendAnyEmailsOrSms(String who) {
        Subscription newSubscription = getSubscription().toBuilder().subscribeEmail(NotificationProcessingServiceBaseTest.NO).subscribeSms(NotificationProcessingServiceBaseTest.YES).build();
        doUnsubscribeWithAssertions(newSubscription, who);
    }

    @ParameterizedTest
    @ValueSource(strings = {"appellant", "representative", "appointee"})
    public void unsubscribeFromSms_doesNotSendAnyEmailsOrSms(String who) {
        Subscription newSubscription = getSubscription().toBuilder().subscribeEmail(NotificationProcessingServiceBaseTest.YES).subscribeSms(NotificationProcessingServiceBaseTest.NO).build();
        doUnsubscribeWithAssertions(newSubscription, who);
    }

    @ParameterizedTest
    @ValueSource(strings = {"appellant", "representative", "appointee"})
    public void subscribeEmail_willSendSubscriptionEmail(String who) throws NotificationClientException {
        Subscription newSubscription = getSubscription().toBuilder().email(NotificationProcessingServiceBaseTest.EMAIL_TEST_2).subscribeEmail(NotificationProcessingServiceBaseTest.YES).subscribeSms(NotificationProcessingServiceBaseTest.NO).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationProcessingServiceBaseTest.NO).subscribeSms(NotificationProcessingServiceBaseTest.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, who);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, who);
        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);
        verify(getNotificationGateway()).sendEmail(eq(subscriptionUpdatedEmailId), eq(newSubscription.getEmail()), any(), any(), any(), any());
        verifyNoMoreInteractions(getNotificationGateway());
    }

    @Test
    public void subscribeMobileForAppellant_willSendSubscriptionSms() throws NotificationClientException {
        String appellant = "appellant";
        Subscription newSubscription = getSubscription().toBuilder().mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).subscribeEmail(NotificationProcessingServiceBaseTest.NO).subscribeSms(NotificationProcessingServiceBaseTest.YES).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationProcessingServiceBaseTest.NO).subscribeSms(NotificationProcessingServiceBaseTest.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, appellant);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appellant);
        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);
        verify(getNotificationGateway()).sendSms(eq(subscriptionUpdatedSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(getNotificationGateway());
    }

    @Test
    public void subscribeMobileForAppointee_willSendSubscriptionSms() throws NotificationClientException {
        String appointee = "appointee";
        Subscription newSubscription = getSubscription().toBuilder().mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).subscribeEmail(NotificationProcessingServiceBaseTest.NO).subscribeSms(NotificationProcessingServiceBaseTest.YES).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationProcessingServiceBaseTest.NO).subscribeSms(NotificationProcessingServiceBaseTest.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, appointee);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appointee);
        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);
        verify(getNotificationGateway()).sendSms(eq(subscriptionUpdatedSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(getNotificationGateway());
    }

    @Test
    public void subscribeMobileForRepresentative_willSendSubscriptionSms() throws NotificationClientException {
        String representative = "representative";
        Subscription newSubscription = getSubscription().toBuilder().mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).subscribeEmail(NotificationProcessingServiceBaseTest.NO).subscribeSms(NotificationProcessingServiceBaseTest.YES).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationProcessingServiceBaseTest.NO).subscribeSms(NotificationProcessingServiceBaseTest.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, representative);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, representative);
        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);
        verify(getNotificationGateway()).sendSms(eq(subscriptionCreatedRepresentativeSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(getNotificationGateway());
    }

    @Test
    public void subscribeMobileAndEmailForAppellant_willSendSubscriptionEmailAndSms() throws NotificationClientException {
        String appellant = "appellant";
        Subscription newSubscription = getSubscription().toBuilder().email(NotificationProcessingServiceBaseTest.EMAIL_TEST_2).mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).subscribeEmail(NotificationProcessingServiceBaseTest.YES).subscribeSms(NotificationProcessingServiceBaseTest.YES).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationProcessingServiceBaseTest.NO).subscribeSms(NotificationProcessingServiceBaseTest.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, appellant);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appellant);
        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);
        verify(getNotificationGateway()).sendEmail(eq(subscriptionUpdatedEmailId), eq(newSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationGateway()).sendSms(eq(subscriptionCreatedAppellantSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationGateway());
    }

    @Test
    public void subscribeMobileAndEmailForAppointee_willSendSubscriptionEmailAndSms() throws NotificationClientException {
        String appointee = "appointee";
        Subscription newSubscription = getSubscription().toBuilder().email(NotificationProcessingServiceBaseTest.EMAIL_TEST_2).mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).subscribeEmail(NotificationProcessingServiceBaseTest.YES).subscribeSms(NotificationProcessingServiceBaseTest.YES).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationProcessingServiceBaseTest.NO).subscribeSms(NotificationProcessingServiceBaseTest.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, appointee);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appointee);
        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);
        verify(getNotificationGateway()).sendEmail(eq(subscriptionUpdatedEmailId), eq(newSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationGateway()).sendSms(eq(subscriptionCreatedAppointeeSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationGateway());
    }

    @Test
    public void subscribeMobileAndEmailForRepresentative_willSendSubscriptionEmailAndSms() throws NotificationClientException {
        String representative = "representative";
        Subscription newSubscription = getSubscription().toBuilder().email(NotificationProcessingServiceBaseTest.EMAIL_TEST_2).mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).subscribeEmail(NotificationProcessingServiceBaseTest.YES).subscribeSms(NotificationProcessingServiceBaseTest.YES).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationProcessingServiceBaseTest.NO).subscribeSms(NotificationProcessingServiceBaseTest.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, representative);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, representative);
        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);
        verify(getNotificationGateway()).sendEmail(eq(subscriptionUpdatedEmailId), eq(newSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationGateway()).sendSms(eq(subscriptionCreatedRepresentativeSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationGateway());
    }

    @ParameterizedTest
    @ValueSource(strings = {"appellant", "representative", "appointee"})
    public void changeEmail_willSendChangeEmailToOldAndNewEmail(String who) throws NotificationClientException {
        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), who);

        Subscription oldSubscription = getSubscription().toBuilder().email(NotificationProcessingServiceBaseTest.EMAIL_TEST_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, who);

        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);

        verify(getNotificationGateway()).sendEmail(eq(subscriptionUpdatedEmailId), eq(getSubscription().getEmail()), any(), any(), any(), any());
        verify(getNotificationGateway()).sendEmail(eq(subscriptionOldEmailId), eq(oldSubscription.getEmail()), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationGateway());
    }

    @Test
    public void changeMobileForAppellant_willSendChangeSmsToOldAndNewMobile() throws NotificationClientException {
        String appellant = "appellant";
        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), appellant);

        Subscription oldSubscription = getSubscription().toBuilder().mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appellant);

        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);

        verify(getNotificationGateway()).sendSms(eq(subscriptionCreatedAppellantSmsId), eq(getSubscription().getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationGateway()).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationGateway());
    }

    @Test
    public void changeMobileForAppointee_willSendChangeSmsToOldAndNewMobile() throws NotificationClientException {
        String appointee = "appointee";
        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), appointee);

        Subscription oldSubscription = getSubscription().toBuilder().mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appointee);

        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);

        verify(getNotificationGateway()).sendSms(eq(subscriptionCreatedAppointeeSmsId), eq(getSubscription().getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationGateway()).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationGateway());
    }

    @Test
    public void changeMobileForRepresentative_willSendChangeSmsToOldAndNewMobile() throws NotificationClientException {
        String representative = "representative";

        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), representative);

        Subscription oldSubscription = getSubscription().toBuilder().mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, representative);

        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);

        verify(getNotificationGateway()).sendSms(eq(subscriptionCreatedRepresentativeSmsId), eq(getSubscription().getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationGateway()).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationGateway());
    }

    @Test
    public void changeMobileAndEmailForAppellant_willSendChangeSmsToOldAndNewMobileAndEmail() throws NotificationClientException {
        String appellant = "appellant";
        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), appellant);

        Subscription oldSubscription = getSubscription().toBuilder().mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).email(NotificationProcessingServiceBaseTest.EMAIL_TEST_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appellant);

        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);

        verify(getNotificationGateway()).sendEmail(eq(subscriptionUpdatedEmailId), eq(getSubscription().getEmail()), any(), any(), any(), any());
        verify(getNotificationGateway()).sendEmail(eq(subscriptionOldEmailId), eq(oldSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationGateway()).sendSms(eq(subscriptionCreatedAppellantSmsId), eq(getSubscription().getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationGateway()).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationGateway());
    }

    @Test
    public void changeMobileAndEmailForAppointee_willSendChangeSmsToOldAndNewMobileAndEmail() throws NotificationClientException {
        String appointee = "appointee";
        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), appointee);

        Subscription oldSubscription = getSubscription().toBuilder().mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).email(NotificationProcessingServiceBaseTest.EMAIL_TEST_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appointee);

        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);

        verify(getNotificationGateway()).sendEmail(eq(subscriptionUpdatedEmailId), eq(getSubscription().getEmail()), any(), any(), any(), any());
        verify(getNotificationGateway()).sendEmail(eq(subscriptionOldEmailId), eq(oldSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationGateway()).sendSms(eq(subscriptionCreatedAppointeeSmsId), eq(getSubscription().getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationGateway()).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationGateway());
    }

    @Test
    public void changeMobileAndEmailForRepresentative_willSendChangeSmsToOldAndNewMobileAndEmail() throws NotificationClientException {
        String representative = "representative";
        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), representative);

        Subscription oldSubscription = getSubscription().toBuilder().mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).email(NotificationProcessingServiceBaseTest.EMAIL_TEST_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, representative);

        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);

        verify(getNotificationGateway()).sendEmail(eq(subscriptionUpdatedEmailId), eq(getSubscription().getEmail()), any(), any(), any(), any());
        verify(getNotificationGateway()).sendEmail(eq(subscriptionOldEmailId), eq(oldSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationGateway()).sendSms(eq(subscriptionCreatedRepresentativeSmsId), eq(getSubscription().getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationGateway()).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationGateway());
    }

    private void doUnsubscribeWithAssertions(Subscription newSubscription, String who) {
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, who);
        SscsCaseData oldSscsCaseData = getSscsCaseData(getSubscription(), who);

        NotificationSscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED);

        getNotificationProcessingService().processNotification(new CcdNotificationWrapper(wrapper), false);

        verifyNoMoreInteractions(getNotificationGateway());
    }
}
