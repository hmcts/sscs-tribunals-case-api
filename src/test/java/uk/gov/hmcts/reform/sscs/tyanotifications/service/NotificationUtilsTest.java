package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.tyanotifications.SscsCaseDataUtils.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationServiceTest.APPELLANT_WITH_ADDRESS;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationServiceTest.getSscsCaseDataBuilder;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.SendNotificationServiceTest.APPELLANT_WITH_ADDRESS_AND_APPOINTEE;

import java.util.Arrays;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;

public class NotificationUtilsTest {
    @Mock
    private NotificationValidService notificationValidService;

    @BeforeEach
    public void setup() {
        openMocks(this);
    }

    @Test
    public void trueWhenHasPopulatedAppointee() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertTrue(hasAppointee(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasNullPopulatedAppointee() {
        Appellant appellant = Appellant.builder()
            .name(Name.builder().firstName("Ap").lastName("pellant").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .appointee(Appointee.builder().build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            appellant,
            null,
            null
        );

        assertFalse(hasAppointee(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasNullAppointee() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertFalse(hasAppointee(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenNoFirstName() {
        assertFalse(hasAppointee(Appointee.builder().name(Name.builder().lastName("Last").build()).build(), "Yes"));
    }

    @Test
    public void falseWhenNoLastName() {
        assertFalse(hasAppointee(Appointee.builder().name(Name.builder().firstName("First").build()).build(), "Yes"));
    }

    @Test
    public void trueWhenHasFirstAndLastName() {
        assertTrue(hasAppointee(Appointee.builder().name(Name.builder().firstName("First").lastName("Last").build()).build(), "Yes"));
    }

    @Test
    public void falseWhenIsAppointeeIsNo() {
        assertFalse(hasAppointee(Appointee.builder().name(Name.builder().firstName("First").lastName("Last").build()).build(), "No"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Yes", "", "null"})
    public void trueWhenIsAppointeeIs(@Nullable String value) {
        assertTrue(hasAppointee(Appointee.builder().name(Name.builder().firstName("First").lastName("Last").build()).build(), value));
    }

    @Test
    public void trueWhenHasPopulatedRep() {
        Representative rep = Representative.builder()
            .hasRepresentative("Yes")
            .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS,
            rep,
            null
        );

        assertTrue(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasPopulatedRepButHasRepSetToNo() {
        Representative rep = Representative.builder()
            .hasRepresentative("No")
            .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS,
            rep,
            null
        );

        assertFalse(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasPopulatedRepButHasRepNotSet() {
        Representative rep = Representative.builder()
            .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
            .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS,
            rep,
            null
        );

        assertFalse(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasNullPopulatedRep() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS,
            Representative.builder().build(),
            null
        );

        assertFalse(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasNullRep() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertFalse(hasRepresentative(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void falseWhenHasNullPopulatedOtherPartyRep() {
        OtherParty otherParty = OtherParty.builder().build();
        assertFalse(hasRepresentative(otherParty));
    }

    @Test
    public void falseWhenHasNullOtherPartyRep() {
        OtherParty otherParty = OtherParty.builder().rep(Representative.builder().build()).build();
        assertFalse(hasRepresentative(otherParty));
    }

    @Test
    public void falseWhenHasNoOtherPartyRep() {
        OtherParty otherParty = OtherParty.builder().rep(Representative.builder().hasRepresentative("No").build()).build();
        assertFalse(hasRepresentative(otherParty));
    }

    @Test
    public void trueWhenHasYesOtherPartyRep() {
        OtherParty otherParty = OtherParty.builder().rep(Representative.builder().hasRepresentative("Yes").build()).build();
        assertTrue(hasRepresentative(otherParty));
    }

    @Test
    public void shouldBeOkToSendNotificationForValidFutureNotification() {
        NotificationEventType eventType = HEARING_BOOKED;
        NotificationWrapper wrapper = buildNotificationWrapper(eventType);

        Subscription subscription = Subscription.builder().subscribeSms("Yes").subscribeEmail("Yes").build();

        when(notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), eventType)).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), eventType)).thenReturn(true);

        assertTrue(isOkToSendNotification(wrapper, eventType, subscription, notificationValidService));
    }

    @Test
    public void shouldNotBeOkToSendNotificationValidPastNotification() {
        NotificationEventType eventType = HEARING_BOOKED;
        NotificationWrapper wrapper = buildNotificationWrapper(eventType);

        Subscription subscription = Subscription.builder().subscribeSms("Yes").subscribeEmail("Yes").build();

        when(notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), eventType)).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), eventType)).thenReturn(false);

        assertFalse(isOkToSendNotification(wrapper, eventType, subscription, notificationValidService));
    }

    @Test
    public void shouldNotBeOkToSendNotificationInvalidNotification() {
        NotificationEventType eventType = HEARING_BOOKED;
        NotificationWrapper wrapper = buildNotificationWrapper(eventType);

        Subscription subscription = Subscription.builder().subscribeSms("Yes").subscribeEmail("Yes").build();

        when(notificationValidService.isNotificationStillValidToSend(wrapper.getNewSscsCaseData().getHearings(), eventType)).thenReturn(false);
        when(notificationValidService.isHearingTypeValidToSendNotification(wrapper.getNewSscsCaseData(), eventType)).thenReturn(true);

        assertFalse(isOkToSendNotification(wrapper, eventType, subscription, notificationValidService));
    }

    private NotificationWrapper buildNotificationWrapper(NotificationEventType eventType) {
        SscsCaseData sscsCaseData = getSscsCaseDataBuilder(
            APPELLANT_WITH_ADDRESS,
            null,
            null
        ).build();
        addHearing(sscsCaseData, 1);
        addHearingOptions(sscsCaseData, "Yes");

        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseData)
            .oldSscsCaseData(sscsCaseData)
            .notificationEventType(eventType)
            .build();
        return new CcdNotificationWrapper(caseDataWrapper);
    }

    @Test
    public void itIsOkToSendNotification() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        Subscription subscription = Subscription.builder().subscribeEmail("test@test.com").subscribeSms("07800000000").build();

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        assertTrue(NotificationUtils.isOkToSendNotification(wrapper, HEARING_BOOKED, subscription, notificationValidService));
    }

    @ParameterizedTest
    @MethodSource("isNotOkToSendNotificationResponses")
    public void isNotOkToSendNotification(boolean isNotificationStillValidToSendResponse, boolean isHearingTypeValidToSendNotificationResponse) {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        Subscription subscription = Subscription.builder().subscribeEmail("test@test.com").subscribeSms("07800000000").build();

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(isNotificationStillValidToSendResponse);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(isHearingTypeValidToSendNotificationResponse);

        assertFalse(NotificationUtils.isOkToSendNotification(wrapper, HEARING_BOOKED, subscription, notificationValidService));
    }

    @Test
    public void okToSendSmsNotificationisValid() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(null, null);

        Subscription subscription = Subscription.builder().subscribeSms("Yes").wantSmsNotifications("Yes").build();
        Notification notification = Notification.builder()
            .reference(new Reference("someref"))
            .destination(Destination.builder().sms("07800123456").build())
            .template(Template.builder().smsTemplateId(Arrays.asList("some.template")).build())
            .build();

        assertTrue(isOkToSendSmsNotification(wrapper, subscription, notification, STRUCK_OUT, notificationValidService));
    }

    @ParameterizedTest
    @MethodSource("isNotOkToSendSmsNotificationScenarios")
    public void okToSendSmsNotificationisNotValid(Subscription subscription, Notification notification) {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(null, null);

        assertFalse(isOkToSendSmsNotification(wrapper, subscription, notification, STRUCK_OUT, notificationValidService));
    }

    @Test
    public void okToSendEmailNotificationisValid() {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(null, null);

        Subscription subscription = Subscription.builder().subscribeEmail("Yes").build();
        Notification notification = Notification.builder()
            .reference(new Reference("someref"))
            .destination(Destination.builder().email("test@test.com").build())
            .template(Template.builder().emailTemplateId("some.template").build())
            .build();

        assertTrue(isOkToSendEmailNotification(wrapper, subscription, notification, notificationValidService));
    }

    @ParameterizedTest
    @MethodSource("isNotOkToSendEmailNotificationScenarios")
    public void okToSendEmailNotificationisNotValid(Subscription subscription, Notification notification) {
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        CcdNotificationWrapper wrapper = buildBaseWrapper(null, null);

        assertFalse(isOkToSendEmailNotification(wrapper, subscription, notification, notificationValidService));
    }

    @Test
    public void hasNoAppointeeSubscriptionsIfAppointeeIsNotSubscribed() {
        Subscription subscription = Subscription.builder().wantSmsNotifications("No").subscribeSms("No").subscribeEmail("No").build();
        CcdNotificationWrapper wrapper = buildBaseWrapper(subscription, null);
        wrapper.getSscsCaseDataWrapper().getNewSscsCaseData().setSubscriptions(wrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getSubscriptions().toBuilder().appointeeSubscription(subscription).build());
        assertFalse(hasAppointeeSubscriptionOrIsMandatoryAppointeeLetter(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void hasNoRepresentativeSubscriptionsIfRepresentativeIsNotSubscribed() {
        Subscription subscription = Subscription.builder().wantSmsNotifications("No").subscribeSms("No").subscribeEmail("No").build();
        CcdNotificationWrapper wrapper = buildBaseWrapper(subscription, null);
        wrapper.getSscsCaseDataWrapper().getNewSscsCaseData().setSubscriptions(wrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getSubscriptions().toBuilder().appointeeSubscription(subscription).build());
        assertFalse(hasRepSubscriptionOrIsMandatoryRepLetter(wrapper.getSscsCaseDataWrapper()));
    }

    @Test
    public void shouldReturnFalseWhenThereIsNoJointParty() {
        assertFalse(hasJointParty(buildBaseWrapper(null, null).getNewSscsCaseData()));
    }

    @Test
    public void shouldReturnTrueWhenThereIsAJointParty() {
        assertTrue(hasJointParty(buildJointPartyWrapper(null, null, YES).getNewSscsCaseData()));
    }

    @Test
    public void shouldReturnTrueWhenThereIsAJointPartySubscriptionAndJointPartyIsYes() {
        Subscription subscription = Subscription.builder().subscribeSms(YES).subscribeEmail(YES).build();
        assertTrue(hasJointPartySubscription(buildJointPartyWrapper(subscription, null, YES).getSscsCaseDataWrapper()));
    }

    @Test
    public void shouldReturnFalseWhenThereIsAJointPartySubscriptionAndJointPartyIsNo() {
        Subscription subscription = Subscription.builder().subscribeSms(YES).subscribeEmail(YES).build();
        assertFalse(hasJointPartySubscription(buildJointPartyWrapper(subscription, null, "No").getSscsCaseDataWrapper()));
    }

    @Test
    public void shouldReturnFalseWhenThereIsANoJointPartySubscription() {
        assertFalse(hasJointPartySubscription(buildJointPartyWrapper(null, null, YES).getSscsCaseDataWrapper()));
    }

    @Test
    public void shouldReturntrueWhenThereIsANoJointPartySubscriptionButALetterIsSent() {
        assertTrue(hasJointPartySubscription(buildJointPartyWrapper(null, ISSUE_FINAL_DECISION, YES).getSscsCaseDataWrapper()));
    }

    @Test
    public void shouldReturnFalseWhenTheSubscriptionIsNull() {
        assertFalse(isValidSubscriptionOrIsMandatoryLetter(null, VALID_APPEAL_CREATED));
    }

    @Test
    public void shouldReturnFalseWhenTheCaseHasNotSubscribed() {
        Subscription subscription = Subscription.builder().subscribeSms(YesNo.NO.getValue()).subscribeEmail(YesNo.NO.getValue()).build();
        assertFalse(isValidSubscriptionOrIsMandatoryLetter(subscription, VALID_APPEAL_CREATED));
    }

    @Test
    public void shouldReturnTrueWhenTheCaseHasSubscribed() {
        Subscription subscription = Subscription.builder().subscribeSms(YES).subscribeEmail(YES).build();
        assertTrue(isValidSubscriptionOrIsMandatoryLetter(subscription, VALID_APPEAL_CREATED));
    }

    @Test
    public void shouldReturntrueWhenThereIsSubscriptionButALetterIsSent() {
        assertTrue(isValidSubscriptionOrIsMandatoryLetter(null, UPDATE_OTHER_PARTY_DATA));
    }

    private Object[] mandatoryNotificationTypes() {
        return new Object[]{
            STRUCK_OUT,
            HEARING_BOOKED,
            DWP_UPLOAD_RESPONSE
        };
    }

    private Object[] nonMandatoryNotificationTypes() {
        return new Object[]{
            ADJOURNED,
            SYA_APPEAL_CREATED,
            RESEND_APPEAL_CREATED,
            APPEAL_DORMANT,
            EVIDENCE_RECEIVED,
            DWP_RESPONSE_RECEIVED,
            POSTPONEMENT,
            SUBSCRIPTION_CREATED,
            SUBSCRIPTION_UPDATED,
            SUBSCRIPTION_OLD,
            EVIDENCE_REMINDER,
            HEARING_REMINDER,
            CASE_UPDATED,
            DO_NOT_SEND
        };
    }

    private static Object[] isNotOkToSendNotificationResponses() {
        return new Object[]{
            new Object[]{
                false, false
            },
            new Object[]{
                false, true
            },
            new Object[]{
                true, false
            }
        };
    }

    private static CcdNotificationWrapper buildJointPartyWrapper(Subscription subscription, NotificationEventType eventType, String jointParty) {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(subscription, eventType);
        final SscsCaseData sscsCaseData = ccdNotificationWrapper.getNewSscsCaseData().toBuilder()
            .jointParty(JointParty.builder()
                .hasJointParty(isYes(jointParty) ? YesNo.YES : YesNo.NO)
                .jointPartyAddressSameAsAppellant(YesNo.YES)
                .name(Name.builder()
                    .firstName("Joint")
                    .lastName("Party")
                    .build())
                .build())
            .subscriptions(Subscriptions.builder().appellantSubscription(subscription).jointPartySubscription(subscription).build())
            .build();
        return new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseData)
            .oldSscsCaseData(sscsCaseData)
            .notificationEventType(eventType)
            .build());
    }

    private static CcdNotificationWrapper buildBaseWrapper(Subscription subscription, NotificationEventType eventType) {
        Subscriptions subscriptions = null;
        if (null != subscription) {
            subscriptions = Subscriptions.builder().appellantSubscription(subscription).build();
        }

        SscsCaseData sscsCaseDataWithDocuments = SscsCaseData.builder()
            .appeal(
                Appeal
                    .builder()
                    .appellant(Appellant.builder().build())
                    .hearingType(AppealHearingType.ORAL.name())
                    .hearingOptions(HearingOptions.builder().wantsToAttend(YES).build())
                    .build())
            .subscriptions(subscriptions)
            .ccdCaseId(CASE_ID)
            .build();

        NotificationSscsCaseDataWrapper notificationSscsCaseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseDataWithDocuments)
            .oldSscsCaseData(sscsCaseDataWithDocuments)
            .notificationEventType(eventType)
            .build();
        return new CcdNotificationWrapper(notificationSscsCaseDataWrapper);
    }

    public static Object[] isNotOkToSendSmsNotificationScenarios() {
        return new Object[]{
            new Object[]{
                null,
                null
            },
            new Object[]{
                null,
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().sms("07800123456").build())
                    .template(Template.builder().smsTemplateId(Arrays.asList("some.template")).build())
                    .build()
            },
            new Object[]{
                Subscription.builder().subscribeSms("Yes").build(),
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().build())
                    .template(Template.builder().smsTemplateId(Arrays.asList("some.template")).build())
                    .build()
            },
            new Object[]{
                Subscription.builder().subscribeSms("Yes").build(),
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().sms("07800123456").build())
                    .template(Template.builder().build())
                    .build()
            }
        };
    }

    public static Object[] isNotOkToSendEmailNotificationScenarios() {
        return new Object[]{
            new Object[]{
                null,
                null
            },
            new Object[]{
                null,
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().email("test@test.com").build())
                    .template(Template.builder().emailTemplateId("some.template").build())
                    .build()
            },
            new Object[]{
                Subscription.builder().subscribeSms("Yes").build(),
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().build())
                    .template(Template.builder().emailTemplateId("some.template").build())
                    .build()
            },
            new Object[]{
                Subscription.builder().subscribeSms("Yes").build(),
                Notification.builder()
                    .reference(new Reference("someref"))
                    .destination(Destination.builder().email("test@test.com").build())
                    .template(Template.builder().build())
                    .build()
            }
        };
    }

    @ParameterizedTest
    @MethodSource("getLatestHearingScenarios")
    public void getLatestHearingTest(
        SscsCaseData sscsCaseData, String expectedHearingId, String expectedDate, String expectedTime) {
        Hearing hearing = sscsCaseData.getLatestHearing();
        assertEquals(expectedHearingId, hearing.getValue().getHearingId());
        assertEquals(expectedDate, hearing.getValue().getHearingDate());
        assertEquals(expectedTime, hearing.getValue().getTime());
    }

    @Test
    public void whenGettingLatestHearing_shouldReturnNullIfNoHearings() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        Hearing hearing = sscsCaseData.getLatestHearing();
        assertNull(hearing);
    }

    private static Hearing createHearing(String hearingId, String hearingDate, String hearingTime) {
        return Hearing.builder().value(HearingDetails.builder()
            .hearingDate(hearingDate)
            .hearingId(hearingId)
            .time(hearingTime)
            .build()).build();
    }

    public static Object[] getLatestHearingScenarios() {
        return new Object[]{
            new Object[]{
                SscsCaseData.builder()
                    .hearings(Arrays.asList(
                        createHearing("1", "2019-06-01", "14:00")))
                    .build(),
                "1", "2019-06-01", "14:00"
            },
            new Object[]{
                SscsCaseData.builder()
                    .hearings(Arrays.asList(
                        createHearing("1", "2019-06-01", "14:00"),
                        createHearing("1", "2019-06-01", "14:01"),
                        createHearing("2", "2019-06-01", "10:00")))
                    .build(),
                "2", "2019-06-01", "10:00"
            },
            new Object[]{
                SscsCaseData.builder()
                    .hearings(Arrays.asList(
                        createHearing("1", "2019-06-01", "10:00"),
                        createHearing("1", "2019-06-01", "14:01"),
                        createHearing("2", "2019-06-02", "14:00")))
                    .build(),
                "2", "2019-06-02", "14:00"
            },
            new Object[]{
                SscsCaseData.builder()
                    .hearings(Arrays.asList(
                        createHearing("3", "2019-06-01", "14:00"),
                        createHearing("1", "2019-06-02", "14:01"),
                        createHearing("2", "2019-06-01", "10:00")))
                    .build(),
                "3", "2019-06-01", "14:00"
            },
            new Object[]{
                SscsCaseData.builder()
                    .hearings(Arrays.asList(
                        createHearing("1", "2019-06-01", "14:00"),
                        createHearing("4", "2019-06-01", "14:01"),
                        createHearing("1", "2019-06-01", "10:00")))
                    .build(),
                "4", "2019-06-01", "14:01"
            },
            new Object[]{
                SscsCaseData.builder()
                    .hearings(Arrays.asList(
                        createHearing("1", "2019-06-01", "14:00"),
                        createHearing("4", "2019-06-01", "14:01"),
                        createHearing("4", "2019-06-01", "13:00"),
                        createHearing("1", "2019-06-01", "10:00")))
                    .build(),
                "4", "2019-06-01", "14:01"
            },
            new Object[]{
                SscsCaseData.builder()
                    .hearings(Arrays.asList(
                        createHearing("1", "2019-06-01", "14:00"),
                        createHearing("4", "2019-06-01", "13:00"),
                        createHearing("4", "2019-06-01", "14:01"),
                        createHearing("1", "2019-06-01", "10:00")))
                    .build(),
                "4", "2019-06-01", "14:01"
            },
            new Object[]{
                SscsCaseData.builder()
                    .hearings(Arrays.asList(
                        createHearing("1", "2019-05-28", "14:01"),
                        createHearing(null, "2018-05-28", "14:01")))
                    .build(),
                "1", "2019-05-28", "14:01"
            },
            new Object[]{
                SscsCaseData.builder()
                    .hearings(Arrays.asList(
                        createHearing(null, "2019-06-01", "14:00"),
                        createHearing(null, "2019-06-01", "13:00"),
                        createHearing(null, "2019-06-01", "14:01"),
                        createHearing(null, "2019-06-01", "10:00")))
                    .build(),
                null, "2019-06-01", "14:01"
            }
        };
    }
}
