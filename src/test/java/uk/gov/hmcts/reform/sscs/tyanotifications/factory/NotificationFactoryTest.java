package uk.gov.hmcts.reform.sscs.tyanotifications.factory;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.getSubscription;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Resource;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.properties.EvidenceProperties;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.tyanotifications.personalisation.NotificationDateConverterUtil;
import uk.gov.hmcts.reform.sscs.tyanotifications.personalisation.SubscriptionPersonalisation;
import uk.gov.hmcts.reform.sscs.tyanotifications.personalisation.WithRepresentativePersonalisation;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.utility.PhoneNumbersUtil;

@RunWith(JUnitParamsRunner.class)
public class NotificationFactoryTest {

    private static final String CASE_ID = "54321";
    private static final String DATE = "2018-01-01T14:01:18.243";

    private NotificationFactory factory;

    private NotificationSscsCaseDataWrapper wrapper;

    private SscsCaseData ccdResponse;

    @Mock
    private PersonalisationFactory personalisationFactory;

    @Mock
    private EvidenceProperties evidenceProperties;

    @Mock
    private EvidenceProperties.EvidenceAddress evidencePropertiesAddress;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Mock
    private NotificationConfig config;

    @Mock
    private NotificationDateConverterUtil notificationDateConverterUtil;

    @InjectMocks
    @Resource
    private SubscriptionPersonalisation subscriptionPersonalisation;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    private Subscription subscription;

    @Mock
    private WithRepresentativePersonalisation withRepresentativePersonalisation;

    @Before
    public void setup() {
        openMocks(this);
        factory = new NotificationFactory(personalisationFactory);

        subscription = Subscription.builder()
            .tya("ABC").email("test@testing.com")
            .mobile("07985858594").subscribeEmail("Yes").subscribeSms("No").build();

        ccdResponse = SscsCaseData.builder().ccdCaseId(CASE_ID).caseReference("SC/1234/5").appeal(Appeal.builder()
                .appellant(Appellant.builder().name(Name.builder().firstName("Ronnie").lastName("Scott").title("Mr").build()).build())
                .benefitType(BenefitType.builder().code("PIP").build()).build())
            .subscriptions(Subscriptions.builder().appellantSubscription(subscription).build()).build();

        wrapper = NotificationSscsCaseDataWrapper.builder().newSscsCaseData(ccdResponse).notificationEventType(APPEAL_RECEIVED).build();

        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://manageemails.com/mac").build());
        when(config.getTrackAppealLink()).thenReturn(Link.builder().linkUrl("http://tyalink.com/appeal_id").build());
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/appeal_id").build());
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://link.com/manage-email-notifications/mac").build());
        when(config.getClaimingExpensesLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/expenses").build());
        when(config.getHearingInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/abouthearing").build());
        when(config.getOnlineHearingLinkWithEmail()).thenReturn(Link.builder().linkUrl("http://link.com/onlineHearing?email={email}").build());
        when(macService.generateToken("ABC", PIP.name())).thenReturn("ZYX");

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("Venue").address1("HMCTS").address2("The Road").address3("Town").address4("City").city("Birmingham").postcode("B23 1EH").build();
        when(regionalProcessingCenterService.getByScReferenceCode("SC/1234/5")).thenReturn(rpc);
        when(hearingContactDateExtractor.extract(any())).thenReturn(Optional.empty());
        when(notificationDateConverterUtil.toEmailDate(any(LocalDate.class))).thenReturn("1 January 2018");

        when(evidenceProperties.getAddress()).thenReturn(evidencePropertiesAddress);
    }

    @Test
    @Parameters({"APPELLANT, appellantEmail", "REPRESENTATIVE, repsEmail"})
    public void givenAppealLapsedEventAndSubscriptionType_shouldInferRightSubscriptionToCreateNotification(
        SubscriptionType subscriptionType, String expectedEmail) {
        NotificationSscsCaseDataWrapper notificationSscsCaseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(SscsCaseData.builder()
                .appeal(Appeal.builder()
                    .benefitType(BenefitType.builder()
                        .code("PIP")
                        .build())
                    .build())
                .subscriptions(Subscriptions.builder()
                    .appellantSubscription(Subscription.builder()
                        .email("appellantEmail")
                        .build())
                    .representativeSubscription(Subscription.builder()
                        .email("repsEmail")
                        .build())
                    .build())
                .build())
            .notificationEventType(APPEAL_LAPSED)
            .build();
        CcdNotificationWrapper notificationWrapper = new CcdNotificationWrapper(notificationSscsCaseDataWrapper);

        given(personalisationFactory.apply(any(NotificationEventType.class)))
            .willReturn(withRepresentativePersonalisation);

        Notification notification = factory.create(notificationWrapper, getSubscriptionWithType(notificationSscsCaseDataWrapper, subscriptionType,
            wrapper.getNewSscsCaseData().getAppeal().getAppellant(), wrapper.getNewSscsCaseData().getAppeal().getAppellant()));
        assertEquals(expectedEmail, notification.getEmail());

        then(withRepresentativePersonalisation).should()
            .getTemplate(eq(notificationWrapper), eq(PIP), eq(subscriptionType));

    }

    @Test
    @Parameters({"APPELLANT, appellantEmail", "REPRESENTATIVE, repsEmail"})
    public void givenAppealDwpLapsedEventAndSubscriptionType_shouldInferRightSubscriptionToCreateNotification(
        SubscriptionType subscriptionType, String expectedEmail) {
        NotificationSscsCaseDataWrapper notificationSscsCaseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(SscsCaseData.builder()
                .appeal(Appeal.builder()
                    .benefitType(BenefitType.builder()
                        .code("PIP")
                        .build())
                    .build())
                .subscriptions(Subscriptions.builder()
                    .appellantSubscription(Subscription.builder()
                        .email("appellantEmail")
                        .build())
                    .representativeSubscription(Subscription.builder()
                        .email("repsEmail")
                        .build())
                    .build())
                .build())
            .notificationEventType(DWP_APPEAL_LAPSED)
            .build();
        CcdNotificationWrapper notificationWrapper = new CcdNotificationWrapper(notificationSscsCaseDataWrapper);

        given(personalisationFactory.apply(any(NotificationEventType.class)))
            .willReturn(withRepresentativePersonalisation);

        Notification notification = factory.create(notificationWrapper, getSubscriptionWithType(notificationSscsCaseDataWrapper, subscriptionType,
            wrapper.getNewSscsCaseData().getAppeal().getAppellant(), wrapper.getNewSscsCaseData().getAppeal().getAppellant()));
        assertEquals(expectedEmail, notification.getEmail());

        then(withRepresentativePersonalisation).should()
            .getTemplate(eq(notificationWrapper), eq(PIP), eq(subscriptionType));

    }

    @Test
    @Parameters({"APPOINTEE, appointeeEmail", "REPRESENTATIVE, repsEmail"})
    public void givenAppealCreatedEventAndSubscriptionType_shouldInferRightSubscriptionToCreateNotification(
        SubscriptionType subscriptionType, String expectedEmail) {
        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(SscsCaseData.builder()
                .appeal(Appeal.builder().appellant(Appellant.builder().appointee(Appointee.builder().build()).build())
                    .benefitType(BenefitType.builder()
                        .code("PIP")
                        .build())
                    .build())
                .subscriptions(Subscriptions.builder()
                    .appellantSubscription(Subscription.builder()
                        .email("appellantEmail")
                        .build())
                    .appointeeSubscription(Subscription.builder()
                        .email("appointeeEmail")
                        .build())
                    .representativeSubscription(Subscription.builder()
                        .email("repsEmail")
                        .build())
                    .build())
                .build())
            .notificationEventType(SYA_APPEAL_CREATED)
            .build();
        CcdNotificationWrapper notificationWrapper = new CcdNotificationWrapper(wrapper);

        given(personalisationFactory.apply(any(NotificationEventType.class)))
            .willReturn(withRepresentativePersonalisation);

        Notification notification = factory.create(notificationWrapper, getSubscriptionWithType(wrapper, subscriptionType,
            wrapper.getNewSscsCaseData().getAppeal().getAppellant(), wrapper.getNewSscsCaseData().getAppeal().getAppellant()));
        assertEquals(expectedEmail, notification.getEmail());

        then(withRepresentativePersonalisation).should()
            .getTemplate(eq(notificationWrapper), eq(PIP), eq(subscriptionType));

    }

    @Test
    @Parameters({"APPOINTEE, appointeeEmail", "REPRESENTATIVE, repsEmail"})
    public void givenValidAppealCreatedEventAndSubscriptionType_shouldInferRightSubscriptionToCreateNotification(
        SubscriptionType subscriptionType, String expectedEmail) {
        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(SscsCaseData.builder()
                .appeal(Appeal.builder().appellant(Appellant.builder().appointee(Appointee.builder().build()).build())
                    .benefitType(BenefitType.builder()
                        .code("PIP")
                        .build())
                    .build())
                .subscriptions(Subscriptions.builder()
                    .appellantSubscription(Subscription.builder()
                        .email("appellantEmail")
                        .build())
                    .appointeeSubscription(Subscription.builder()
                        .email("appointeeEmail")
                        .build())
                    .representativeSubscription(Subscription.builder()
                        .email("repsEmail")
                        .build())
                    .build())
                .build())
            .notificationEventType(VALID_APPEAL_CREATED)
            .build();
        CcdNotificationWrapper notificationWrapper = new CcdNotificationWrapper(wrapper);

        given(personalisationFactory.apply(any(NotificationEventType.class)))
            .willReturn(withRepresentativePersonalisation);

        Notification notification = factory.create(notificationWrapper, getSubscriptionWithType(wrapper, subscriptionType,
            wrapper.getNewSscsCaseData().getAppeal().getAppellant(), wrapper.getNewSscsCaseData().getAppeal().getAppellant()));
        assertEquals(expectedEmail, notification.getEmail());

        then(withRepresentativePersonalisation).should()
            .getTemplate(eq(notificationWrapper), eq(PIP), eq(subscriptionType));

    }

    @Test
    public void buildSubscriptionCreatedSmsNotificationFromSscsCaseDataWithSubscriptionUpdatedNotificationAndSmsFirstSubscribed() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(eq(SUBSCRIPTION_UPDATED.getId()), eq(SUBSCRIPTION_CREATED.getId() + ".appellant"), eq(SUBSCRIPTION_UPDATED.getId()),
            eq(SUBSCRIPTION_UPDATED.getId()), eq(PIP), any(NotificationWrapper.class), any())).thenReturn(Template.builder().emailTemplateId(null).smsTemplateId(Arrays.asList("123")).build());

        wrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").wantSmsNotifications("Yes").subscribeEmail("No").build()).build())
                    .build())
            .oldSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("No").wantSmsNotifications("No").subscribeEmail("No").build()).build())
                    .build())
            .notificationEventType(SUBSCRIPTION_UPDATED)
            .build();

        Notification result = factory.create(new CcdNotificationWrapper(wrapper), getSubscriptionWithType(wrapper, APPELLANT,
            wrapper.getNewSscsCaseData().getAppeal().getAppellant(), wrapper.getNewSscsCaseData().getAppeal().getAppellant()));

        assertEquals("123", result.getSmsTemplate().get(0));
    }

    @Test
    public void buildSubscriptionUpdatedSmsNotificationFromSscsCaseDataWithSubscriptionUpdatedNotificationAndSmsAlreadySubscribed() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(eq(SUBSCRIPTION_UPDATED.getId()), eq(SUBSCRIPTION_UPDATED.getId()), eq(SUBSCRIPTION_UPDATED.getId()), eq(SUBSCRIPTION_UPDATED.getId()),
            eq(PIP), any(NotificationWrapper.class), any())).thenReturn(Template.builder().emailTemplateId("123").smsTemplateId(Arrays.asList("123")).build());

        wrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").wantSmsNotifications("Yes").subscribeEmail("No").build()).build())
                    .build())
            .oldSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").wantSmsNotifications("Yes").subscribeEmail("Yes").build()).build())
                    .build())
            .notificationEventType(SUBSCRIPTION_UPDATED)
            .build();

        Notification result = factory.create(new CcdNotificationWrapper(wrapper), getSubscriptionWithType(wrapper, APPELLANT,
            wrapper.getNewSscsCaseData().getAppeal().getAppellant(), wrapper.getNewSscsCaseData().getAppeal().getAppellant()));

        assertEquals("123", result.getSmsTemplate().get(0));
    }

    @Test
    public void buildLastNotificationFromSscsCaseDataEventWhenSmsFirstSubscribed() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(eq(SUBSCRIPTION_UPDATED.getId()), eq(SUBSCRIPTION_CREATED.getId() + ".appellant"), eq(SUBSCRIPTION_UPDATED.getId()),
            eq(SUBSCRIPTION_UPDATED.getId()), eq(PIP), any(NotificationWrapper.class), any())).thenReturn(Template.builder().emailTemplateId("123").smsTemplateId(Arrays.asList("123")).build());

        List<Event> event = new ArrayList<>();
        event.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getId()).build()).build());

        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").wantSmsNotifications("Yes").subscribeEmail("Yes").build()).build())
                    .events(event)
                    .build())
            .oldSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("No").wantSmsNotifications("No").subscribeEmail("Yes").build()).build())
                    .build())
            .notificationEventType(SUBSCRIPTION_UPDATED)
            .build();

        Notification result = factory.create(new CcdNotificationWrapper(wrapper), getSubscriptionWithType(wrapper, APPELLANT,
            wrapper.getNewSscsCaseData().getAppeal().getAppellant(), wrapper.getNewSscsCaseData().getAppeal().getAppellant()));

        assertNull(result.getDestination().email);
        assertNotNull(subscription.getMobile());
        assertEquals(PhoneNumbersUtil.cleanPhoneNumber(subscription.getMobile()).orElse(subscription.getMobile()),
            result.getDestination().sms);
        assertEquals("123", result.getSmsTemplate().get(0));
    }

    @Test
    public void buildNoNotificationFromSscsCaseDataWhenSubscriptionUpdateReceivedWithNoChangeInSubscription() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(eq(SUBSCRIPTION_UPDATED.getId()), eq(SUBSCRIPTION_UPDATED.getId()), eq(SUBSCRIPTION_UPDATED.getId()),
            eq(SUBSCRIPTION_UPDATED.getId()), eq(PIP), any(NotificationWrapper.class), any())).thenReturn(Template.builder().emailTemplateId("123").smsTemplateId(Arrays.asList("123")).build());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getId()).build()).build());

        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").wantSmsNotifications("Yes").subscribeEmail("Yes").build()).build())
                    .events(events)
                    .build())
            .oldSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").wantSmsNotifications("Yes").subscribeEmail("Yes").build()).build())
                    .build())
            .notificationEventType(SUBSCRIPTION_UPDATED)
            .build();

        Notification result = factory.create(new CcdNotificationWrapper(wrapper), getSubscriptionWithType(wrapper, APPELLANT,
            wrapper.getNewSscsCaseData().getAppeal().getAppellant(), wrapper.getNewSscsCaseData().getAppeal().getAppellant()));

        assertEquals("123", result.getEmailTemplate());
        assertEquals("123", result.getSmsTemplate().get(0));
        assertNull(result.getDestination().email);
        assertNull(result.getDestination().sms);
    }

    @Test
    public void buildSubscriptionUpdatedNotificationFromSscsCaseDataWhenEmailIsChanged() {
        when(personalisationFactory.apply(SUBSCRIPTION_UPDATED)).thenReturn(subscriptionPersonalisation);
        when(config.getTemplate(eq(SUBSCRIPTION_UPDATED.getId()), eq(SUBSCRIPTION_UPDATED.getId()), eq(SUBSCRIPTION_UPDATED.getId()),
            eq(SUBSCRIPTION_UPDATED.getId()), eq(PIP), any(NotificationWrapper.class), any())).thenReturn(Template.builder().emailTemplateId("123").smsTemplateId(Arrays.asList("123")).build());

        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getId()).build()).build());

        wrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().email("changed@testing.com").subscribeSms("Yes").wantSmsNotifications("Yes").subscribeEmail("Yes").build()).build())
                    .events(events)
                    .build())
            .oldSscsCaseData(
                ccdResponse.toBuilder()
                    .subscriptions(Subscriptions.builder().appellantSubscription(subscription.toBuilder().subscribeSms("Yes").wantSmsNotifications("Yes").subscribeEmail("Yes").build()).build())
                    .build())
            .notificationEventType(SUBSCRIPTION_UPDATED)
            .build();

        Notification result = factory.create(new CcdNotificationWrapper(wrapper), getSubscriptionWithType(wrapper, APPELLANT,
            wrapper.getNewSscsCaseData().getAppeal().getAppellant(), wrapper.getNewSscsCaseData().getAppeal().getAppellant()));

        assertEquals("123", result.getEmailTemplate());
        assertEquals("123", result.getSmsTemplate().get(0));
        assertEquals("changed@testing.com", result.getDestination().email);
        assertNull(result.getDestination().sms);
    }

    @Test
    public void returnNullIfPersonalisationNotFound() {
        when(personalisationFactory.apply(APPEAL_RECEIVED)).thenReturn(null);
        Notification result = factory.create(new CcdNotificationWrapper(wrapper), getSubscriptionWithType(wrapper, APPELLANT,
            wrapper.getNewSscsCaseData().getAppeal().getAppellant(), wrapper.getNewSscsCaseData().getAppeal().getAppellant()));

        assertNull(result);
    }

    @Test
    @Parameters({"null", ""})
    public void givenAnEmptyBenefitType_shouldNotThrowExceptionAndGenerateTemplate(@Nullable String benefitType) {

        NotificationSscsCaseDataWrapper notificationSscsCaseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(SscsCaseData.builder()
                .appeal(Appeal.builder()
                    .benefitType(BenefitType.builder()
                        .code(benefitType)
                        .build())
                    .build())
                .build())
            .notificationEventType(APPEAL_RECEIVED)
            .build();

        given(personalisationFactory.apply(any(NotificationEventType.class)))
            .willReturn(withRepresentativePersonalisation);

        CcdNotificationWrapper notificationWrapper = new CcdNotificationWrapper(notificationSscsCaseDataWrapper);

        factory.create(notificationWrapper, new SubscriptionWithType(null, APPELLANT, null, null));

        then(withRepresentativePersonalisation).should()
            .getTemplate(eq(notificationWrapper), eq(null), eq(APPELLANT));
    }

    @Test
    public void shouldHandleNoSubscription() {
        NotificationSscsCaseDataWrapper notificationSscsCaseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(SscsCaseData.builder()
                .appeal(Appeal.builder()
                    .benefitType(BenefitType.builder()
                        .code("PIP")
                        .build())
                    .build())
                .build())
            .notificationEventType(APPEAL_RECEIVED)
            .build();
        CcdNotificationWrapper notificationWrapper = new CcdNotificationWrapper(notificationSscsCaseDataWrapper);

        given(personalisationFactory.apply(any(NotificationEventType.class)))
            .willReturn(withRepresentativePersonalisation);

        Notification notification = factory.create(notificationWrapper, new SubscriptionWithType(null, APPELLANT,
            null, null));

        assertEquals(StringUtils.EMPTY, notification.getAppealNumber());
        assertEquals(Destination.builder().build(), notification.getDestination());
    }

    private SubscriptionWithType getSubscriptionWithType(NotificationSscsCaseDataWrapper notificationSscsCaseDataWrapper, SubscriptionType subscriptionType,
                                                         Party party, Entity entity) {
        return new SubscriptionWithType(getSubscription(notificationSscsCaseDataWrapper.getNewSscsCaseData(), subscriptionType),
            subscriptionType, party, entity);
    }
}
