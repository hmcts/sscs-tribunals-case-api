package uk.gov.hmcts.reform.sscs.tyanotifications.personalisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.getSubscription;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationConfiguration;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.properties.EvidenceProperties;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.tyanotifications.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.MessageAuthenticationServiceImpl;

public class SubscriptionPersonalisationTest {

    private NotificationSscsCaseDataWrapper wrapper;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Mock
    private NotificationConfig config;

    @Mock
    private EvidenceProperties evidenceProperties;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    @Mock
    private EvidenceProperties.EvidenceAddress evidencePropertiesAddress;

    @Mock
    private NotificationDateConverterUtil notificationDateConverterUtil;

    @InjectMocks
    @Resource
    SubscriptionPersonalisation personalisation;

    @Spy
    private PersonalisationConfiguration personalisationConfiguration;

    private static final Subscription NEW_SUBSCRIPTION = Subscription.builder()
        .tya("GLSCRR").email("test@email.com")
        .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").wantSmsNotifications("Yes").build();

    @BeforeEach
    public void setup() {
        openMocks(this);
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://manageemails.com/mac").build());
        when(config.getTrackAppealLink()).thenReturn(Link.builder().linkUrl("http://tyalink.com/appeal_id").build());
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/appeal_id").build());
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://link.com/manage-email-notifications/mac").build());
        when(config.getClaimingExpensesLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/expenses").build());
        when(config.getHearingInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/abouthearing").build());
        when(config.getOnlineHearingLinkWithEmail()).thenReturn(Link.builder().linkUrl("http://link.com/onlineHearing?email={email}").build());
        when(notificationDateConverterUtil.toEmailDate(any(LocalDate.class))).thenReturn("1 January 2018");
        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");
        when(hearingContactDateExtractor.extract(any())).thenReturn(Optional.empty());
        when(evidenceProperties.getAddress()).thenReturn(evidencePropertiesAddress);

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("Venue").address1("HMCTS").address2("The Road").address3("Town").address4("City").city("Birmingham").postcode("B23 1EH").phoneNumber("01234543225").build();

        when(regionalProcessingCenterService.getByScReferenceCode("1234")).thenReturn(rpc);

        Map<String, String> englishMap = new HashMap<>();
        Map<String, String> welshMap = new HashMap<>();

        Map<LanguagePreference, Map<String, String>> personalisations = new HashMap<>();
        personalisations.put(LanguagePreference.ENGLISH, englishMap);
        personalisations.put(LanguagePreference.WELSH, welshMap);

        personalisationConfiguration.setPersonalisation(personalisations);
    }

    @Test
    public void customisePersonalisation() {
        buildNewAndOldCaseData(NEW_SUBSCRIPTION, buildSubscriptionWithNothingSubscribed());
        Map<String, Object> result = personalisation.create(wrapper, getSubscriptionWithType(new CcdNotificationWrapper(wrapper)));

        assertEquals("PIP", result.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals("Personal Independence Payment", result.get(BENEFIT_FULL_NAME_LITERAL));
        assertEquals("1234", result.get(APPEAL_REF));
        assertEquals("GLSCRR", result.get(APPEAL_ID_LITERAL));
        assertEquals("Harry Kane", result.get(NAME));
        assertEquals("01234543225", result.get(PHONE_NUMBER));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals(AppConstants.DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals(AppConstants.DWP_FULL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
    }

    @Test
    public void customisePersonalisationShouldLeaveNotificationTypeAsSubscriptionUpdatedWhenEmailHasChanged() {
        Subscription newAppellantSubscription = Subscription.builder()
            .tya("GLSCRR").email("changed@test.com")
            .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        Subscription oldSubscription = Subscription.builder()
            .tya("GLSCRR").email("test@email.com")
            .mobile("07983495065").subscribeEmail("Yes").subscribeSms("No").build();

        buildNewAndOldCaseData(newAppellantSubscription, oldSubscription);

        personalisation.create(wrapper, getSubscriptionWithType(new CcdNotificationWrapper(wrapper)));

        assertEquals(SUBSCRIPTION_UPDATED, wrapper.getNotificationEventType());
    }

    @Test
    public void customisePersonalisationShouldSetSmsConfirmationFlagWhenNumberHasChanged() {
        Subscription newAppellantSubscription = Subscription.builder()
            .tya("GLSCRR").email("test@email.com")
            .mobile("07900000000").subscribeEmail("Yes").subscribeSms("Yes").build();

        Subscription oldSubscription = Subscription.builder()
            .tya("GLSCRR").email("test@email.com")
            .mobile("07983495065").subscribeEmail("Yes").subscribeSms("Yes").build();

        buildNewAndOldCaseData(newAppellantSubscription, oldSubscription);

        personalisation.create(wrapper, getSubscriptionWithType(new CcdNotificationWrapper(wrapper)));

        assertTrue(personalisation.isSendSmsSubscriptionConfirmation());
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeWhenSmsSubscribedIsFirstSet() {
        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(NEW_SUBSCRIPTION, buildSubscriptionWithNothingSubscribed());

        assertTrue(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsAlreadySet() {
        Subscription oldSubscription = NEW_SUBSCRIPTION.toBuilder()
            .subscribeEmail("No").subscribeSms("Yes").build();

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(NEW_SUBSCRIPTION, oldSubscription);

        assertFalse(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeWhenSmsAlreadySubscribedAndNumberIsChanged() {
        Subscription oldSubscription = NEW_SUBSCRIPTION.toBuilder()
            .subscribeEmail("No").subscribeSms("Yes").mobile("07900000000").build();

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(NEW_SUBSCRIPTION, oldSubscription);

        assertTrue(result);
    }

    @Test
    public void checkSubscriptionCreatedNotificationTypeNotChangedWhenSmsSubscribedIsNotSet() {

        Subscription newSubscription = NEW_SUBSCRIPTION.toBuilder().subscribeSms("No").build();
        Subscription oldSubscription = newSubscription.toBuilder()
            .subscribeEmail("No").subscribeSms("No").build();

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(newSubscription, oldSubscription);

        assertFalse(result);
    }

    @Test
    public void emptyOldAppellantSubscriptionReturnsFalseForSubscriptionCreatedNotificationType() {
        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(null, buildSubscriptionWithNothingSubscribed());
        assertFalse(result);
    }

    @Test
    public void emptyNewAppellantSubscriptionReturnsFalseForSubscriptionCreatedNotificationType() {

        Boolean result = personalisation.shouldSendSmsSubscriptionConfirmation(null, buildSubscriptionWithNothingSubscribed());

        assertFalse(result);
    }

    @Test
    public void willUnsetMobileAndSmsIfSubscriptionIsUnchanged() {
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(NEW_SUBSCRIPTION, APPELLANT, null, null);
        personalisation.unsetMobileAndEmailIfUnchanged(subscriptionWithType, NEW_SUBSCRIPTION);
        assertEquals(NEW_SUBSCRIPTION.toBuilder().mobile(null).email(null).build(),
            subscriptionWithType.getSubscription());
    }

    @Test
    public void willUnsetEmailIfSubscriptionIfSmsIsSubscribed() {
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(NEW_SUBSCRIPTION, APPELLANT, null, null);
        Subscription oldSubscription = NEW_SUBSCRIPTION.toBuilder().subscribeSms("No").build();
        personalisation.unsetMobileAndEmailIfUnchanged(subscriptionWithType, oldSubscription);
        assertEquals(NEW_SUBSCRIPTION.toBuilder().email(null).build(), subscriptionWithType.getSubscription());
    }

    @Test
    public void willUnsetMobileIfSubscriptionIfEmailIsSubscribed() {
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(NEW_SUBSCRIPTION, APPELLANT, null, null);
        Subscription oldSubscription = NEW_SUBSCRIPTION.toBuilder().subscribeEmail("No").build();
        personalisation.unsetMobileAndEmailIfUnchanged(subscriptionWithType, oldSubscription);
        assertEquals(NEW_SUBSCRIPTION.toBuilder().mobile(null).build(), subscriptionWithType.getSubscription());
    }

    @Test
    public void willUnsetMobileIfSubscriptionIfEmailIsChanged() {
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(NEW_SUBSCRIPTION, APPELLANT, null, null);
        Subscription oldSubscription = NEW_SUBSCRIPTION.toBuilder().email("test2@email.com").build();
        personalisation.unsetMobileAndEmailIfUnchanged(subscriptionWithType, oldSubscription);
        assertEquals(NEW_SUBSCRIPTION.toBuilder().mobile(null).build(), subscriptionWithType.getSubscription());
    }

    @Test
    public void willUnsetEmailIfSubscriptionIfMobileIsChanged() {
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(NEW_SUBSCRIPTION, APPELLANT, null, null);
        Subscription oldSubscription = NEW_SUBSCRIPTION.toBuilder().mobile("07983495060").build();
        personalisation.unsetMobileAndEmailIfUnchanged(subscriptionWithType, oldSubscription);
        assertEquals(NEW_SUBSCRIPTION.toBuilder().email(null).build(), subscriptionWithType.getSubscription());
    }


    private Subscription buildSubscriptionWithNothingSubscribed() {
        return NEW_SUBSCRIPTION.toBuilder().subscribeEmail("No").subscribeSms("No").wantSmsNotifications("No").build();
    }

    private SubscriptionWithType getSubscriptionWithType(CcdNotificationWrapper ccdNotificationWrapper) {
        return new SubscriptionWithType(getSubscription(ccdNotificationWrapper.getNewSscsCaseData(), APPELLANT),
            APPELLANT, ccdNotificationWrapper.getNewSscsCaseData().getAppeal().getAppellant(),
            ccdNotificationWrapper.getNewSscsCaseData().getAppeal().getAppellant());
    }

    private void buildNewAndOldCaseData(Subscription newAppellantSubscription, Subscription oldSubscription) {
        SscsCaseData newSscsCaseData = SscsCaseData.builder().ccdCaseId("54321")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(Name.builder().firstName("Harry").lastName("Kane").title("Mr").build()).build()).build())
            .caseReference("1234")
            .subscriptions(Subscriptions.builder().appellantSubscription(newAppellantSubscription).build()).build();

        SscsCaseData oldSscsCaseData = SscsCaseData.builder().ccdCaseId("54321")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(Name.builder().firstName("Harry").lastName("Kane").title("Mr").build()).build()).build())
            .caseReference("5432")
            .subscriptions(Subscriptions.builder().appellantSubscription(oldSubscription).build()).build();

        wrapper = NotificationSscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).notificationEventType(SUBSCRIPTION_UPDATED).build();
    }
}
