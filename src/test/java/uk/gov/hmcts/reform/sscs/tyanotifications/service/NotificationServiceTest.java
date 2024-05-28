package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.EVENTS_FOR_ACTION_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.EVENT_TYPES_FOR_BUNDLED_LETTER;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.getSubscription;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.docmosis.PdfLetterService;

@RunWith(JUnitParamsRunner.class)
public class NotificationServiceTest {

    static Appellant APPELLANT_WITH_ADDRESS = Appellant.builder()
        .name(Name.builder().firstName("Ap").lastName("pellant").build())
        .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
        .build();

    private static final String APPEAL_NUMBER = "GLSCRR";
    private static final String YES = "Yes";
    private static final String CASE_REFERENCE = "ABC123";
    private static final String CASE_ID = "1000001";
    private static final String EMAIL_TEMPLATE_ID = "email-template-id";
    private static final String SMS_TEMPLATE_ID = "sms-template-id";
    private static final String WELSH_SMS_TEMPLATE_ID = "welsh-template-id";
    private static final String LETTER_TEMPLATE_ID = "letter-template-id";
    private static final String SAME_TEST_EMAIL_COM = "sametest@email.com";
    private static final String NEW_TEST_EMAIL_COM = "newtest@email.com";
    private static final String NO = "No";
    private static final String PIP = "PIP";
    private static final String EMAIL = "Email";
    private static final String SMS = "SMS";
    private static final String SMS_MOBILE = "07123456789";
    private static final String LETTER = "Letter";
    private static final String MOBILE_NUMBER_1 = "07983495065";
    private static final String MOBILE_NUMBER_2 = "07983495067";

    private NotificationService notificationService;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private NotificationFactory factory;

    @Mock
    private ReminderService reminderService;

    @Mock
    private NotificationValidService notificationValidService;

    @Mock
    private NotificationHandler notificationHandler;

    @Mock
    private OutOfHoursCalculator outOfHoursCalculator;

    @Mock
    private NotificationConfig notificationConfig;

    @Mock
    private PdfStoreService pdfStoreService;

    @Mock
    private IdamService idamService;

    @Mock
    private PdfLetterService pdfLetterService;

    private SscsCaseData sscsCaseData;
    private CcdNotificationWrapper ccdNotificationWrapper;
    private NotificationSscsCaseDataWrapper notificationSscsCaseDataWrapper;
    private final Subscription subscription = Subscription.builder()
        .tya(APPEAL_NUMBER)
        .email(EMAIL)
        .mobile(MOBILE_NUMBER_1)
        .subscribeEmail(YES)
        .subscribeSms(YES).wantSmsNotifications(YES)
        .build();

    @Mock
    private Appender<ILoggingEvent> mockAppender;
    @Captor
    private ArgumentCaptor captorLoggingEvent;

    @Captor
    private ArgumentCaptor<CcdNotificationWrapper> ccdNotificationWrapperCaptor;

    @Before
    public void setup() {
        openMocks(this);

        notificationService = getNotificationService();

        sscsCaseData = SscsCaseData.builder()
            .appeal(
                Appeal.builder()
                    .hearingType(AppealHearingType.ORAL.name())
                    .hearingOptions(HearingOptions.builder().wantsToAttend(YES).build())
                    .appellant(APPELLANT_WITH_ADDRESS)
                    .build()
            )
            .dwpState(DwpState.RESPONSE_SUBMITTED_DWP)
            .subscriptions(Subscriptions.builder().appellantSubscription(subscription).build())
            .caseReference(CASE_REFERENCE)
            .createdInGapsFrom(READY_TO_LIST.getId())
            .build();
        notificationSscsCaseDataWrapper = NotificationSscsCaseDataWrapper.builder().newSscsCaseData(sscsCaseData).oldSscsCaseData(sscsCaseData).notificationEventType(APPEAL_WITHDRAWN).build();
        ccdNotificationWrapper = new CcdNotificationWrapper(notificationSscsCaseDataWrapper);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);

        String authHeader = "authHeader";
        String serviceAuthHeader = "serviceAuthHeader";
        IdamTokens idamTokens = IdamTokens.builder().idamOauth2Token(authHeader).serviceAuthorization(serviceAuthHeader).build();

        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        Logger logger = (Logger) LoggerFactory.getLogger(NotificationService.class.getName());
        logger.addAppender(mockAppender);
    }

    @Test
    @Parameters(method = "generateNotificationTypeAndSubscriptionsScenarios")
    public void givenNotificationEventTypeAndDifferentSubscriptionCombinations_shouldManageNotificationAndSubscriptionAccordingly(
        NotificationEventType notificationEventType,
        int wantedNumberOfEmailNotificationsSent,
        int wantedNumberOfSmsNotificationsSent,
        int wantedNumberOfLetterNotificationsSent,
        int wantedNumberOfFactoryCreateCalls,
        Subscription appellantSubscription,
        Subscription repsSubscription,
        Subscription appointeeSubscription,
        SubscriptionType[] expectedSubscriptionTypes, List<CcdValue<OtherParty>> otherParties) {

        ccdNotificationWrapper = buildNotificationWrapperGivenNotificationTypeAndSubscriptions(
            notificationEventType, appellantSubscription, repsSubscription, appointeeSubscription, otherParties);

        if (notificationEventType == DRAFT_TO_VALID_APPEAL_CREATED) {
            //override
            notificationEventType = VALID_APPEAL_CREATED;
        }

        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), eq(notificationEventType))).willReturn(true);

        given(notificationValidService.isNotificationStillValidToSend(anyList(), eq(notificationEventType)))
            .willReturn(true);


        given(factory.create(any(NotificationWrapper.class), any(SubscriptionWithType.class)))
            .willReturn(new Notification(
                Template.builder()
                    .emailTemplateId(EMAIL_TEMPLATE_ID)
                    .smsTemplateId(Arrays.asList(SMS_TEMPLATE_ID, WELSH_SMS_TEMPLATE_ID))
                    .letterTemplateId(LETTER_TEMPLATE_ID)
                    .build(),
                Destination.builder()
                    .email(EMAIL)
                    .sms(SMS_MOBILE)
                    .build(),
                new HashMap<>(),
                new Reference(),
                null));

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, true);

        ArgumentCaptor<SubscriptionWithType> subscriptionWithTypeCaptor = ArgumentCaptor.forClass(SubscriptionWithType.class);
        then(factory).should(times(wantedNumberOfFactoryCreateCalls))
            .create(any(NotificationWrapper.class), subscriptionWithTypeCaptor.capture());
        List<SubscriptionType> actualSubscriptionTypes = subscriptionWithTypeCaptor.getAllValues().stream()
            .map(SubscriptionWithType::getSubscriptionType).collect(Collectors.toList());
        if (expectedSubscriptionTypes != null) {
            assertTrue(actualSubscriptionTypes.stream().allMatch(actualSubscriptionType ->
                Arrays.asList(expectedSubscriptionTypes).contains(actualSubscriptionType)));
        }

        then(notificationHandler).should(times(wantedNumberOfEmailNotificationsSent)).sendNotification(
            eq(ccdNotificationWrapper), eq(EMAIL_TEMPLATE_ID), eq("Email"),
            any(NotificationHandler.SendNotification.class));
        then(notificationHandler).should(times(wantedNumberOfSmsNotificationsSent)).sendNotification(
            eq(ccdNotificationWrapper), eq(SMS_TEMPLATE_ID), eq("SMS"),
            any(NotificationHandler.SendNotification.class));
        then(notificationHandler).should(times(wantedNumberOfLetterNotificationsSent)).sendNotification(
            eq(ccdNotificationWrapper), eq(LETTER_TEMPLATE_ID), eq("Letter"),
            any(NotificationHandler.SendNotification.class));

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }


    @Test
    @Parameters(method = "generateNotificationTypeAndSubscriptionsAppointeeScenarios")
    public void givenNotificationEventTypeAndAppointeeSubscriptionCombinations_shouldManageNotificationAndSubscriptionAccordingly(
        NotificationEventType notificationEventType, int wantedNumberOfEmailNotificationsSent,
        int wantedNumberOfSmsNotificationsSent, Subscription appointeeSubscription, Subscription repsSubscription,
        SubscriptionType[] expectedSubscriptionTypes) {

        ccdNotificationWrapper = buildNotificationWrapperGivenNotificationTypeAndAppointeeSubscriptions(
            notificationEventType, appointeeSubscription, repsSubscription);

        if (notificationEventType == DRAFT_TO_VALID_APPEAL_CREATED) {
            //override
            notificationEventType = VALID_APPEAL_CREATED;
        }

        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), eq(notificationEventType))).willReturn(true);

        given(notificationValidService.isNotificationStillValidToSend(anyList(), eq(notificationEventType)))
            .willReturn(true);


        given(factory.create(any(NotificationWrapper.class), any(SubscriptionWithType.class)))
            .willReturn(new Notification(
                Template.builder()
                    .emailTemplateId(EMAIL_TEMPLATE_ID)
                    .smsTemplateId(Arrays.asList(SMS_TEMPLATE_ID))
                    .build(),
                Destination.builder()
                    .email(EMAIL)
                    .sms(SMS_MOBILE)
                    .build(),
                new HashMap<>(),
                new Reference(),
                null));

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, true);

        ArgumentCaptor<SubscriptionWithType> subscriptionWithTypeCaptor = ArgumentCaptor.forClass(SubscriptionWithType.class);
        then(factory).should(times(expectedSubscriptionTypes.length))
            .create(any(NotificationWrapper.class), subscriptionWithTypeCaptor.capture());
        assertArrayEquals(expectedSubscriptionTypes, subscriptionWithTypeCaptor.getAllValues().stream().map(SubscriptionWithType::getSubscriptionType).toArray());

        then(notificationHandler).should(times(wantedNumberOfEmailNotificationsSent)).sendNotification(
            eq(ccdNotificationWrapper), eq(EMAIL_TEMPLATE_ID), eq("Email"),
            any(NotificationHandler.SendNotification.class));
        then(notificationHandler).should(times(wantedNumberOfSmsNotificationsSent)).sendNotification(
            eq(ccdNotificationWrapper), eq(SMS_TEMPLATE_ID), eq("SMS"),
            any(NotificationHandler.SendNotification.class));

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }


    @SuppressWarnings({"Indentation", "UnusedPrivateMethod"})
    private Object[] generateNotificationTypeAndSubscriptionsScenarios() {
        return new Object[]{
            new Object[]{
                APPEAL_LAPSED,
                1,
                1,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT}, null
            },
            new Object[]{
                DWP_UPLOAD_RESPONSE,
                2,
                2,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT, OTHER_PARTY},
                buildOtherParties(null, Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build())
            },
            new Object[]{
                POSTPONEMENT,
                2,
                1,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT, OTHER_PARTY},
                buildOtherParties(null, Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build())
            },
            new Object[]{
                APPEAL_LAPSED,
                2,
                2,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT, OTHER_PARTY},
                buildOtherParties(null, Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build())
            },
            new Object[]{
                ADMIN_APPEAL_WITHDRAWN,
                1,
                1,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
                null
            },
            new Object[]{
                ADMIN_APPEAL_WITHDRAWN,
                0,
                0,
                1,
                1,
                null,
                null,
                null,
                new SubscriptionType[]{APPELLANT},
                null
            },
            new Object[]{
                ADMIN_APPEAL_WITHDRAWN,
                2,
                1,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
                null
            },
            new Object[]{
                APPEAL_LAPSED,
                2,
                1,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
                null
            },
            new Object[]{
                APPEAL_LAPSED,
                2,
                2,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
                null
            },
            new Object[]{
                APPEAL_RECEIVED,
                1,
                1,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
                null
            },
            new Object[]{
                APPEAL_RECEIVED,
                2,
                1,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
                null
            },
            new Object[]{
                APPEAL_RECEIVED,
                2,
                2,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPELLANT, REPRESENTATIVE},
                null
            },
            new Object[]{
                SYA_APPEAL_CREATED,
                1,
                0,
                0,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
                null
            },
            new Object[]{
                SYA_APPEAL_CREATED,
                1,
                1,
                0,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
                null
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                1,
                0,
                0,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
                null
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                1,
                1,
                0,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
                null
            },
            new Object[]{
                DRAFT_TO_VALID_APPEAL_CREATED,
                1,
                0,
                0,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
                null
            },
            new Object[]{
                DRAFT_TO_VALID_APPEAL_CREATED,
                1,
                1,
                0,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                null,
                null,
                new SubscriptionType[]{APPELLANT},
                null
            },
            new Object[]{
                UPDATE_OTHER_PARTY_DATA,
                0,
                0,
                1,
                1,
                null,
                null,
                null,
                new SubscriptionType[]{OTHER_PARTY},
                buildOtherParties(YesNo.YES, null)
            }
        };
    }

    @SuppressWarnings("Indentation")
    private Object[] generateNotificationTypeAndSubscriptionsAppointeeScenarios() {
        return new Object[]{
            new Object[]{
                SYA_APPEAL_CREATED,
                1,
                0,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                APPEAL_RECEIVED,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                APPEAL_RECEIVED,
                2,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
            },
            new Object[]{
                APPEAL_RECEIVED,
                2,
                2,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build(),
                new SubscriptionType[]{APPOINTEE, REPRESENTATIVE},
            },
            new Object[]{
                SYA_APPEAL_CREATED,
                1,
                0,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                SYA_APPEAL_CREATED,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                HEARING_REMINDER,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                HEARING_REMINDER,
                1,
                0,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                HEARING_REMINDER,
                0,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                1,
                0,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                DRAFT_TO_VALID_APPEAL_CREATED,
                1,
                0,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            },
            new Object[]{
                DRAFT_TO_VALID_APPEAL_CREATED,
                1,
                1,
                Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .subscribeEmail(YES)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .mobile(MOBILE_NUMBER_1)
                    .build(),
                null,
                new SubscriptionType[]{APPOINTEE},
            }
        };
    }

    private CcdNotificationWrapper buildNotificationWrapperGivenNotificationTypeAndSubscriptions(
        NotificationEventType notificationEventType, Subscription appellantSubscription,
        Subscription repsSubscription, Subscription appointeeSubscription, List<CcdValue<OtherParty>> otherParties) {
        return buildNotificationWrapperGivenNotificationTypeAndSubscriptions(notificationEventType,
            appellantSubscription, repsSubscription, appointeeSubscription, new SscsCaseData(), otherParties);
    }

    private CcdNotificationWrapper buildNotificationWrapperGivenNotificationTypeAndSubscriptions(
        NotificationEventType notificationEventType, Subscription appellantSubscription,
        Subscription repsSubscription, Subscription appointeeSubscription, SscsCaseData oldCaseData,
        List<CcdValue<OtherParty>> otherParties) {

        Representative rep = null;
        if (repsSubscription != null) {
            rep = Representative.builder()
                .hasRepresentative("Yes")
                .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                .build();
        }

        Appellant appellant = Appellant.builder()
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 7LL").build())
            .build();
        if (appointeeSubscription != null) {
            appellant.setAppointee(Appointee.builder()
                .name(Name.builder().firstName("Jack").lastName("Smith").build())
                .build());
        }

        sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(appellant)
                .rep(rep)
                .hearingType(AppealHearingType.ORAL.name())
                .hearingOptions(HearingOptions.builder()
                    .wantsToAttend(YES)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(appellantSubscription)
                .representativeSubscription(repsSubscription)
                .appointeeSubscription(appointeeSubscription)
                .build())
            .caseReference(CASE_REFERENCE)
            .otherParties(otherParties)
            .hearings(singletonList(Hearing.builder().build()))
            .createdInGapsFrom(READY_TO_LIST.getId())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .build())
            .build();

        notificationSscsCaseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .oldSscsCaseData(oldCaseData)
            .newSscsCaseData(sscsCaseData)
            .notificationEventType(notificationEventType)
            .build();

        return new CcdNotificationWrapper(notificationSscsCaseDataWrapper);
    }

    @NotNull
    private List<CcdValue<OtherParty>> buildOtherParties(YesNo newOtherPartyNotification, Subscription otherPartySubscription) {
        return List.of(CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .sendNewOtherPartyNotification(newOtherPartyNotification)
            .id("1")
            .otherPartySubscription(otherPartySubscription)
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 7LL").build())
            .build()).build());
    }

    private CcdNotificationWrapper buildNotificationWrapperGivenNotificationTypeAndAppointeeSubscriptions(
        NotificationEventType notificationEventType, Subscription appointeeSubscription,
        Subscription repsSubscription) {
        return buildNotificationWrapperGivenNotificationTypeAndAppointeeSubscriptions(notificationEventType, appointeeSubscription, repsSubscription, new SscsCaseData());
    }

    private CcdNotificationWrapper buildNotificationWrapperGivenNotificationTypeAndAppointeeSubscriptions(
        NotificationEventType notificationEventType, Subscription appointeeSubscription,
        Subscription repsSubscription, SscsCaseData oldCaseData) {

        Representative rep = null;
        if (repsSubscription != null) {
            rep = Representative.builder()
                .hasRepresentative("Yes")
                .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                .build();
        }

        Appointee appointee = null;
        if (appointeeSubscription != null) {
            appointee = Appointee.builder()
                .name(Name.builder().firstName("Jack").lastName("Johnson").build())
                .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 7LL").build())
                .build();
        }

        sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().appointee(appointee).build())
                .rep(rep)
                .hearingType(AppealHearingType.ORAL.name())
                .hearingOptions(HearingOptions.builder()
                    .wantsToAttend(YES)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder().build())
                .appointeeSubscription(appointeeSubscription)
                .representativeSubscription(repsSubscription)
                .build())
            .caseReference(CASE_REFERENCE)
            .hearings(singletonList(Hearing.builder().build()))
            .build();

        notificationSscsCaseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .oldSscsCaseData(oldCaseData)
            .newSscsCaseData(sscsCaseData)
            .notificationEventType(notificationEventType)
            .build();

        return new CcdNotificationWrapper(notificationSscsCaseDataWrapper);
    }

    @Test
    public void sendEmailToGovNotifyWhenNotificationIsAnEmailAndTemplateNotBlank() {
        String emailTemplateId = "abc";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), new HashMap<>(), new Reference(), null);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(emailTemplateId), eq(EMAIL), any(NotificationHandler.SendNotification.class));

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void sendSmsToGovNotifyWhenNotificationIsAnSmsAndTemplateNotBlank() {
        String smsTemplateId = "123";
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId(Arrays.asList(smsTemplateId)).build(), Destination.builder().email(null).sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(smsTemplateId), eq(SMS), any(NotificationHandler.SendNotification.class));

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void sendSmsAndEmailToGovNotifyWhenNotificationIsAnSmsAndEmailAndTemplateNotBlank() {
        String emailTemplateId = "abc";
        String smsTemplateId = "123";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(Arrays.asList(smsTemplateId)).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(emailTemplateId), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(smsTemplateId), eq(SMS), any(NotificationHandler.SendNotification.class));

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenNotificationIsNotAnEmail() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(Arrays.asList("123")).build(), Destination.builder().email(null).sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), notificationSscsCaseDataWrapper.getNotificationEventType(), ccdNotificationWrapper.getNewSscsCaseData());

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenNotificationIsNotAnSms() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(Arrays.asList("123")).build(), Destination.builder().email("test@testing.com").sms(null).build(), new HashMap<>(), new Reference(), null);
        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationSender, never()).sendSms(notification.getSmsTemplate().get(0), notification.getMobile(), notification.getPlaceholders(), notification.getReference(), notification.getSmsSenderTemplate(), notificationSscsCaseDataWrapper.getNotificationEventType(), ccdNotificationWrapper.getNewSscsCaseData());

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void doNotSendEmailToGovNotifyWhenEmailTemplateIsBlank() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId(Arrays.asList("123")).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), notificationSscsCaseDataWrapper.getNotificationEventType(), ccdNotificationWrapper.getNewSscsCaseData());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void doNotSendSmsToGovNotifyWhenSmsTemplateIsBlank() throws Exception {
        String smsTemplateId = null;
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationSender, never()).sendSms(anyString(), eq(notification.getMobile()), eq(notification.getPlaceholders()), eq(notification.getReference()), eq(notification.getSmsSenderTemplate()), eq(notificationSscsCaseDataWrapper.getNotificationEventType()), eq(ccdNotificationWrapper.getNewSscsCaseData()));

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void doNotSendEmailOrSmsWhenNoActiveSubscription() throws Exception {
        Appeal appeal = Appeal.builder().appellant(Appellant.builder().build()).build();
        Subscription appellantSubscription = Subscription.builder().tya(APPEAL_NUMBER).email("test@email.com")
            .mobile(MOBILE_NUMBER_1).subscribeEmail("No").subscribeSms("No").build();

        sscsCaseData = SscsCaseData.builder().appeal(appeal).subscriptions(Subscriptions.builder().appellantSubscription(appellantSubscription).build()).caseReference(CASE_REFERENCE).build();
        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder().newSscsCaseData(sscsCaseData).oldSscsCaseData(sscsCaseData).notificationEventType(APPEAL_WITHDRAWN).build();
        ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId(Arrays.asList("123")).build(), Destination.builder().email(null).sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationSender, never()).sendSms(anyString(), eq(notification.getMobile()), eq(notification.getPlaceholders()), eq(notification.getReference()), eq(notification.getSmsSenderTemplate()), eq(notificationSscsCaseDataWrapper.getNotificationEventType()), eq(ccdNotificationWrapper.getNewSscsCaseData()));
        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), notificationSscsCaseDataWrapper.getNotificationEventType(), ccdNotificationWrapper.getNewSscsCaseData());

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void createsReminders() {

        Notification notification = new Notification(Template.builder().emailTemplateId(null).smsTemplateId(Arrays.asList("123")).build(), Destination.builder().email(null).sms("07823456746").build(), new HashMap<>(), new Reference(), null);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(reminderService).createReminders(ccdNotificationWrapper);

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void doNotSendNotificationWhenNotificationNotValidToSend() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), new HashMap<>(), new Reference(), null);
        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(false);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), notificationSscsCaseDataWrapper.getNotificationEventType(), ccdNotificationWrapper.getNewSscsCaseData());

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void doNotSendNotificationWhenHearingTypeIsNotValidToSend() throws Exception {
        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), new HashMap<>(), new Reference(), null);
        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(false);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationSender, never()).sendEmail(notification.getEmailTemplate(), notification.getEmail(), notification.getPlaceholders(), notification.getReference(), notificationSscsCaseDataWrapper.getNotificationEventType(), ccdNotificationWrapper.getNewSscsCaseData());

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void doNotSendNotificationsOutOfHours() {
        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder().newSscsCaseData(sscsCaseData).oldSscsCaseData(sscsCaseData).notificationEventType(HEARING_REMINDER).build();
        ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(Arrays.asList("123")).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(true);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationHandler, never()).sendNotification(any(), any(), any(), any());
        verify(notificationHandler).scheduleNotification(ccdNotificationWrapper);
        verifyNoMoreInteractions(reminderService);

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    @Parameters({"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "APPEAL_RECEIVED",
        "DWP_UPLOAD_RESPONSE"})
    public void delayScheduleOfEvents(NotificationEventType eventType) {
        sscsCaseData.setCaseCreated(LocalDate.now().toString());
        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseData).oldSscsCaseData(sscsCaseData)
            .notificationEventType(eventType)
            .build();
        ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        Notification notification = new Notification(Template.builder().emailTemplateId("abc").smsTemplateId(Arrays.asList("123")).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationHandler, never()).sendNotification(any(), any(), any(), any());
        ArgumentCaptor<ZonedDateTime> argument = ArgumentCaptor.forClass(ZonedDateTime.class);
        verify(notificationHandler).scheduleNotification(eq(ccdNotificationWrapper), argument.capture());
        assertThat(argument.getValue().isBefore(ZonedDateTime.now().plusMinutes(6)), is(true));
        verifyNoMoreInteractions(reminderService);

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void shouldSendEmailAndSmsToOldEmailAddressForEmailSubscriptionUpdateForPaperCase() {
        Subscription appellantNewSubscription = Subscription.builder().tya(APPEAL_NUMBER).email(NEW_TEST_EMAIL_COM)
            .mobile(MOBILE_NUMBER_1).subscribeEmail(YES).subscribeSms(YES).wantSmsNotifications(YES).build();
        Subscription appellantOldSubscription = Subscription.builder().tya(APPEAL_NUMBER).email("oldtest@email.com")
            .mobile(MOBILE_NUMBER_2).subscribeEmail(YES).subscribeSms(YES).wantSmsNotifications(YES).build();

        SscsCaseData newSscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().build())
                .hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
            .subscriptions(Subscriptions.builder().appellantSubscription(appellantNewSubscription).build())
            .caseReference(CASE_REFERENCE).build();

        SscsCaseData oldSscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().build())
                .hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
            .subscriptions(Subscriptions.builder().appellantSubscription(appellantOldSubscription).build())
            .caseReference(CASE_REFERENCE).build();

        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).oldSscsCaseData(oldSscsCaseData).notificationEventType(SUBSCRIPTION_UPDATED).build();
        ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(
            Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(Arrays.asList(SMS_TEMPLATE_ID)).build(),
            Destination.builder().email(NEW_TEST_EMAIL_COM).sms(MOBILE_NUMBER_2).build(), new HashMap<>(), new Reference(), null);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        when(notificationConfig.getTemplate(any(), any(), any(), any(), any(), any(), any())).thenReturn(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(Arrays.asList(SMS_TEMPLATE_ID)).build());

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationHandler, times(2)).sendNotification(eq(ccdNotificationWrapper), any(), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(2)).sendNotification(eq(ccdNotificationWrapper), any(), eq(SMS), any(NotificationHandler.SendNotification.class));

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }


    @Test
    public void shouldNotSendEmailOrSmsToOldEmailAddressIfOldAndNewEmailAndSmsAreSame() {
        Subscription appellantNewSubscription = Subscription.builder().tya(APPEAL_NUMBER).email(SAME_TEST_EMAIL_COM)
            .mobile(MOBILE_NUMBER_1).subscribeEmail(YES).subscribeSms(YES).wantSmsNotifications(YES).build();
        Subscription appellantOldSubscription = Subscription.builder().tya(APPEAL_NUMBER).email(SAME_TEST_EMAIL_COM)
            .mobile(MOBILE_NUMBER_1).subscribeEmail(YES).subscribeSms(YES).wantSmsNotifications(YES).build();

        SscsCaseData newSscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().build())
                .hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
            .subscriptions(Subscriptions.builder().appellantSubscription(appellantNewSubscription).build())
            .caseReference(CASE_REFERENCE).build();

        SscsCaseData oldSscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().build())
                .hearingType(AppealHearingType.PAPER.name()).benefitType(BenefitType.builder().code(PIP).build()).build())
            .subscriptions(Subscriptions.builder().appellantSubscription(appellantOldSubscription).build())
            .caseReference(CASE_REFERENCE).build();

        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData)
            .oldSscsCaseData(oldSscsCaseData).notificationEventType(SUBSCRIPTION_UPDATED).build();
        ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(
            Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(Arrays.asList(SMS_TEMPLATE_ID)).build(),
            Destination.builder().email(NEW_TEST_EMAIL_COM).sms(MOBILE_NUMBER_2).build(), new HashMap<>(), new Reference(), null);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        when(notificationConfig.getTemplate(any(), any(), any(), any(), any(), any(), any())).thenReturn(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(Arrays.asList(SMS_TEMPLATE_ID)).build());

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), any(), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), any(), eq(SMS), any(NotificationHandler.SendNotification.class));

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void shouldNotSendEmailAndSmsToOldEmailAddressIfOldEmailAddressAndSmsNotPresent() {
        Subscription appellantNewSubscription = Subscription.builder()
            .tya(APPEAL_NUMBER)
            .email(SAME_TEST_EMAIL_COM)
            .mobile(MOBILE_NUMBER_1)
            .subscribeEmail(YES)
            .subscribeSms(YES).wantSmsNotifications(YES)
            .build();
        Subscription appellantOldSubscription = Subscription.builder()
            .tya(APPEAL_NUMBER)
            .build();

        SscsCaseData newSscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(Appellant.builder().build())
                .hearingType(AppealHearingType.PAPER.name())
                .benefitType(BenefitType.builder()
                    .code(PIP)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(appellantNewSubscription)
                .build())
            .caseReference(CASE_REFERENCE).build();

        SscsCaseData oldSscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .appellant(Appellant.builder().build())
                .hearingType(AppealHearingType.PAPER.name())
                .benefitType(BenefitType.builder()
                    .code(PIP)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(appellantOldSubscription).build())
            .caseReference(CASE_REFERENCE).build();

        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(newSscsCaseData)
            .oldSscsCaseData(oldSscsCaseData)
            .notificationEventType(SUBSCRIPTION_UPDATED)
            .build();
        ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);

        Notification notification = new Notification(
            Template.builder()
                .emailTemplateId(EMAIL_TEMPLATE_ID)
                .smsTemplateId(Arrays.asList(SMS_TEMPLATE_ID))
                .build(),
            Destination.builder()
                .email(NEW_TEST_EMAIL_COM)
                .sms(MOBILE_NUMBER_2)
                .build(),
            new HashMap<>(),
            new Reference(),
            null);

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        when(notificationConfig.getTemplate(any(), any(), any(), any(), any(), any(), any())).thenReturn(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(Arrays.asList(SMS_TEMPLATE_ID)).build());

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), any(), eq(EMAIL), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), any(), eq(SMS), any(NotificationHandler.SendNotification.class));

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void willNotSendHearingNotifications_whenHearingBookedAndDwpStateIsFinalDecisionIssued() {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(HEARING_BOOKED, APPELLANT_WITH_ADDRESS, null, null);
        ccdNotificationWrapper.getNewSscsCaseData().setDwpState(DwpState.FINAL_DECISION_ISSUED);

        SendNotificationService sendNotificationService = new SendNotificationService(notificationSender, notificationHandler, notificationValidService, pdfLetterService, pdfStoreService);

        final NotificationService notificationService = new NotificationService(factory, reminderService,
            notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService, false
        );

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        then(notificationHandler).shouldHaveNoMoreInteractions();
    }

    @Test
    public void sendAppellantLetterOnAppealReceived() throws IOException {
        String fileUrl = "http://dm-store:4506/documents/1e1eb3d2-5b6c-430d-8dad-ebcea1ad7ecf";
        String docmosisId = "docmosis-id.doc";
        CcdNotificationWrapper ccdNotificationWrapper = buildWrapperWithDocuments(APPEAL_RECEIVED, fileUrl, APPELLANT_WITH_ADDRESS, null, "");
        Notification notification = new Notification(Template.builder().docmosisTemplateId(docmosisId).build(), Destination.builder().build(), new HashMap<>(), new Reference(), null);

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));

        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);
        when(pdfLetterService.generateLetter(any(), any(), any())).thenReturn(sampleDirectionCoversheet);

        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);

        getNotificationService().manageNotificationAndSubscription(ccdNotificationWrapper, true);

        verify(notificationHandler, times(1)).sendNotification(eq(ccdNotificationWrapper), eq(docmosisId), eq(LETTER), any(NotificationHandler.SendNotification.class));

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void shouldLogErrorWhenRequestForInformationWithEmptyInfoFromAppellant() {
        CcdNotificationWrapper wrapper = buildBaseWrapperWithCaseData(
            getSscsCaseDataBuilderSettingInformationFromAppellant(APPELLANT_WITH_ADDRESS, null, null, null).build(),
            REQUEST_FOR_INFORMATION
        );

        getNotificationService().manageNotificationAndSubscription(wrapper, false);

        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Request for Information", Level.INFO);
    }

    @Test
    public void shouldLogErrorWhenRequestForInformationWithNoInfoFromAppellant() {
        CcdNotificationWrapper wrapper = buildBaseWrapperWithCaseData(
            getSscsCaseDataBuilderSettingInformationFromAppellant(APPELLANT_WITH_ADDRESS, null, null, "no").build(),
            REQUEST_FOR_INFORMATION
        );

        getNotificationService().manageNotificationAndSubscription(wrapper, false);

        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Request for Information", Level.INFO);
    }

    @Test
    public void shouldNotLogErrorWhenRequestForInformationWithInfoFromAppellant() {
        CcdNotificationWrapper wrapper = buildBaseWrapperWithCaseData(
            getSscsCaseDataBuilderSettingInformationFromAppellant(APPELLANT_WITH_ADDRESS, null, null, "yes").build(),
            REQUEST_FOR_INFORMATION
        );

        given(factory.create(any(NotificationWrapper.class), any(SubscriptionWithType.class)))
            .willReturn(new Notification(
                Template.builder()
                    .emailTemplateId(EMAIL_TEMPLATE_ID)
                    .smsTemplateId(Arrays.asList(SMS_TEMPLATE_ID))
                    .build(),
                Destination.builder()
                    .email(EMAIL)
                    .sms(SMS_MOBILE)
                    .build(),
                new HashMap<>(),
                new Reference(),
                null));

        getNotificationService().manageNotificationAndSubscription(wrapper, false);

        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    @Parameters(method = "allEventTypesExceptRequestForInformationAndProcessingHearingRequest")
    public void shouldNotLogErrorWhenNotRequestForInformation(NotificationEventType eventType) {
        CcdNotificationWrapper wrapper = buildBaseWrapperWithCaseData(
            getSscsCaseDataBuilderSettingInformationFromAppellant(APPELLANT_WITH_ADDRESS, null, null, "yes").build(),
            eventType
        );

        given(factory.create(any(NotificationWrapper.class), any(SubscriptionWithType.class)))
            .willReturn(new Notification(
                Template.builder()
                    .emailTemplateId(EMAIL_TEMPLATE_ID)
                    .smsTemplateId(Arrays.asList(SMS_TEMPLATE_ID))
                    .build(),
                Destination.builder()
                    .email(EMAIL)
                    .sms(SMS_MOBILE)
                    .build(),
                new HashMap<>(),
                new Reference(),
                null));

        getNotificationService().manageNotificationAndSubscription(wrapper, false);

        verifyErrorLogMessageNotLogged(mockAppender, captorLoggingEvent, "Request For Information");
    }

    @Test
    public void shouldLogErrorWhenNotValidationNotification() {
        CcdNotificationWrapper wrapper = buildBaseWrapperWithCaseData(
            getSscsCaseDataBuilderSettingInformationFromAppellant(APPELLANT_WITH_ADDRESS, null, null, "yes").build(),
            ADJOURNED
        );

        given(factory.create(any(NotificationWrapper.class), any(SubscriptionWithType.class)))
            .willReturn(new Notification(
                Template.builder()
                    .emailTemplateId(EMAIL_TEMPLATE_ID)
                    .smsTemplateId(Arrays.asList(SMS_TEMPLATE_ID))
                    .build(),
                Destination.builder()
                    .email(EMAIL)
                    .sms(SMS_MOBILE)
                    .build(),
                new HashMap<>(),
                new Reference(),
                null));

        getNotificationService().manageNotificationAndSubscription(wrapper, false);

        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Is not a valid notification event", Level.ERROR);
    }

    @Test
    public void willNotSendDwpUpload_whenCreatedInGapsFromIsValidAppeal() {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(DWP_UPLOAD_RESPONSE, APPELLANT_WITH_ADDRESS, null, null);
        ccdNotificationWrapper.getNewSscsCaseData().setCreatedInGapsFrom(State.VALID_APPEAL.getId());

        Notification notification = new Notification(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        given(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).willReturn(notification);
        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), eq(DWP_UPLOAD_RESPONSE))).willReturn(true);
        given(notificationValidService.isNotificationStillValidToSend(anyList(), eq(DWP_UPLOAD_RESPONSE)))
            .willReturn(true);

        getNotificationService().manageNotificationAndSubscription(ccdNotificationWrapper, false);

        then(notificationHandler).shouldHaveNoMoreInteractions();
    }

    @Test
    @Parameters({"HEARING_BOOKED", "HEARING_REMINDER"})
    public void willNotSendHearingNotifications_whenCovid19FeatureTrue(NotificationEventType notificationEventType) {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(notificationEventType, APPELLANT_WITH_ADDRESS, null, null);
        ccdNotificationWrapper.getNewSscsCaseData().setCreatedInGapsFrom(State.VALID_APPEAL.getId());

        Notification notification = new Notification(Template.builder().emailTemplateId(EMAIL_TEMPLATE_ID).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        given(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).willReturn(notification);
        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), eq(notificationEventType))).willReturn(true);
        given(notificationValidService.isNotificationStillValidToSend(anyList(), eq(notificationEventType)))
            .willReturn(true);

        SendNotificationService sendNotificationService = new SendNotificationService(notificationSender, notificationHandler, notificationValidService, pdfLetterService, pdfStoreService);

        final NotificationService notificationService = new NotificationService(factory, reminderService,
            notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService, true
        );

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        then(notificationHandler).shouldHaveNoMoreInteractions();
    }

    @Test
    public void willSendHearingNotifications_whenHearingBookedAndHearingIdIsDifferent() {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(HEARING_BOOKED, APPELLANT_WITH_ADDRESS, Representative.builder().hasRepresentative("no").build(), SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        addHearings(ccdNotificationWrapper);
        ccdNotificationWrapper.getOldSscsCaseData().getLatestHearing().getValue().setHearingId("1");
        ccdNotificationWrapper.getNewSscsCaseData().setState(State.WITH_DWP);
        ccdNotificationWrapper.getNewSscsCaseData().setCreatedInGapsFrom("validAppeal");

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);
        Notification notification = new Notification(Template.builder().emailTemplateId("emailTemplateId").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), new HashMap<>(), new Reference(), null);
        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        then(notificationHandler).should(atLeastOnce()).sendNotification(
            eq(ccdNotificationWrapper), eq("emailTemplateId"), eq("Email"),
            any(NotificationHandler.SendNotification.class));
    }

    @Ignore
    @Test
    public void willNotSendHearingNotifications_whenHearingBookedAndHearingDateIsDifferent() {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(HEARING_BOOKED, APPELLANT_WITH_ADDRESS, null, null);
        addHearings(ccdNotificationWrapper);
        ccdNotificationWrapper.getOldSscsCaseData().getLatestHearing().getValue().setHearingDate(LocalDate.now().plusDays(1).toString());

        sendWrapperAndVerifyNoMoreInteractions(ccdNotificationWrapper);
    }

    @Ignore
    @Test
    public void willNotSendHearingNotifications_whenHearingBookedAndHearingLocationIsDifferent() {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(HEARING_BOOKED, APPELLANT_WITH_ADDRESS, null, null);
        addHearings(ccdNotificationWrapper);
        ccdNotificationWrapper.getOldSscsCaseData().getLatestHearing().getValue().setEpimsId("3242342");

        sendWrapperAndVerifyNoMoreInteractions(ccdNotificationWrapper);
    }

    @Ignore
    @Test
    public void willNotSendHearingNotifications_whenHearingBookedAndHearingChannelIsDifferent() {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(HEARING_BOOKED, APPELLANT_WITH_ADDRESS, null, null);
        addHearings(ccdNotificationWrapper);
        ccdNotificationWrapper.getOldSscsCaseData().getLatestHearing().getValue().setHearingChannel(HearingChannel.VIDEO);

        sendWrapperAndVerifyNoMoreInteractions(ccdNotificationWrapper);
    }

    private void sendWrapperAndVerifyNoMoreInteractions(CcdNotificationWrapper ccdNotificationWrapper) {
        SendNotificationService sendNotificationService = new SendNotificationService(notificationSender, notificationHandler, notificationValidService, pdfLetterService, pdfStoreService);

        final NotificationService notificationService = new NotificationService(factory, reminderService,
            notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService, false
        );

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        then(notificationHandler).shouldHaveNoMoreInteractions();
    }

    private void addHearings(CcdNotificationWrapper ccdNotificationWrapper) {
        String currentTime = LocalTime.now().toString();
        ccdNotificationWrapper.getOldSscsCaseData().setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingId("0")
            .hearingDate(LocalDate.now().toString())
            .time(currentTime)
            .epimsId("324")
            .hearingChannel(HearingChannel.PAPER)
            .build()).build()));

        ccdNotificationWrapper.getNewSscsCaseData().setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingId("0")
            .hearingDate(LocalDate.now().toString())
            .time(currentTime)
            .epimsId("324")
            .hearingChannel(HearingChannel.PAPER)
            .build()).build()));
    }

    @Test
    @Parameters({"DIRECTION_ISSUED", "DECISION_ISSUED", "ISSUE_FINAL_DECISION", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED_WELSH"})
    public void givenReissueDocumentEventReceivedAndResendToOtherPartyYes_thenOverrideNotificationTypeAndSendToOtherParty(NotificationEventType notificationEventType) throws IOException {

        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapperOtherParty(REISSUE_DOCUMENT, APPELLANT_WITH_ADDRESS, SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setOtherPartyOptions(getOtherPartyOptions(YesNo.YES));
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setReissueFurtherEvidenceDocument(new DynamicList(notificationEventType.getId()));
        ccdNotificationWrapper.getNewSscsCaseData().setSubscriptions(null);

        Notification notification = new Notification(Template.builder().docmosisTemplateId(LETTER_TEMPLATE_ID).emailTemplateId(null).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        given(factory.create(ccdNotificationWrapperCaptor.capture(), any())).willReturn(notification);
        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), any())).willReturn(true);
        given(notificationValidService.isNotificationStillValidToSend(anyList(), any()))
            .willReturn(true);

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));
        given(pdfLetterService.generateLetter(any(), any(), any())).willReturn(sampleDirectionCoversheet);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        assertEquals(notificationEventType, ccdNotificationWrapperCaptor.getValue().getNotificationType());

        then(notificationHandler).should(times(1)).sendNotification(
            eq(ccdNotificationWrapper), any(), eq("Letter"),
            any(NotificationHandler.SendNotification.class));
    }

    @Test
    @Parameters({"DIRECTION_ISSUED, No", "DIRECTION_ISSUED, null", "DECISION_ISSUED, No", "DECISION_ISSUED, null",
        "DIRECTION_ISSUED_WELSH, No", "DIRECTION_ISSUED_WELSH, null", "DECISION_ISSUED_WELSH, No", "DECISION_ISSUED_WELSH, null",
        "ISSUE_FINAL_DECISION, No", "ISSUE_FINAL_DECISION, null", "ISSUE_FINAL_DECISION_WELSH, No", "ISSUE_FINAL_DECISION_WELSH, null", "ISSUE_ADJOURNMENT_NOTICE, No", "ISSUE_ADJOURNMENT_NOTICE, null"})
    public void givenReissueDocumentEventReceivedAndResendToOtherPartyNotSet_thenDoNotSendToAppellant(NotificationEventType notificationEventType, @Nullable String resendToOtherParty) {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapperOtherParty(REISSUE_DOCUMENT, APPELLANT_WITH_ADDRESS, SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setOtherPartyOptions(getOtherPartyOptions(getYesNoFromString(resendToOtherParty)));
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setReissueFurtherEvidenceDocument(new DynamicList(notificationEventType.getId()));
        ccdNotificationWrapper.getNewSscsCaseData().setSubscriptions(null);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verifyNoInteractions(notificationHandler);
    }

    @Test
    @Parameters({"DIRECTION_ISSUED", "DECISION_ISSUED", "ISSUE_FINAL_DECISION", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED_WELSH"})
    public void givenReissueDocumentEventReceivedAndResendToAppellantYes_thenOverrideNotificationTypeAndSendToAppellant(NotificationEventType notificationEventType) throws IOException {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(REISSUE_DOCUMENT, APPELLANT_WITH_ADDRESS, null, SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setResendToAppellant(YesNo.YES);
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setReissueFurtherEvidenceDocument(new DynamicList(notificationEventType.getId()));
        ccdNotificationWrapper.getNewSscsCaseData().setSubscriptions(null);

        Notification notification = new Notification(Template.builder().docmosisTemplateId(LETTER_TEMPLATE_ID).emailTemplateId(null).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        given(factory.create(ccdNotificationWrapperCaptor.capture(), eq(getSubscriptionWithType(ccdNotificationWrapper)))).willReturn(notification);
        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), any())).willReturn(true);
        given(notificationValidService.isNotificationStillValidToSend(anyList(), any()))
            .willReturn(true);

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));
        given(pdfLetterService.generateLetter(any(), any(), any())).willReturn(sampleDirectionCoversheet);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        assertEquals(notificationEventType, ccdNotificationWrapperCaptor.getValue().getNotificationType());

        then(notificationHandler).should().sendNotification(
            eq(ccdNotificationWrapper), any(), eq("Letter"),
            any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void givenReissueDocumentEventReceivedAndResendToAppellantYesAndEventWelshAndLongLetter_thenOverrideNotificationTypeAndSendToAppellantAndSendEnglishAndWelshSeparately() throws IOException {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(REISSUE_DOCUMENT, APPELLANT_WITH_ADDRESS, null, SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setResendToAppellant(YesNo.YES);
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setReissueFurtherEvidenceDocument(new DynamicList(ISSUE_FINAL_DECISION_WELSH.getId()));
        ccdNotificationWrapper.getNewSscsCaseData().setSubscriptions(null);

        Notification notification = new Notification(Template.builder().docmosisTemplateId(LETTER_TEMPLATE_ID).emailTemplateId(null).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        given(factory.create(ccdNotificationWrapperCaptor.capture(), eq(getSubscriptionWithType(ccdNotificationWrapper)))).willReturn(notification);
        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), any())).willReturn(true);
        given(notificationValidService.isNotificationStillValidToSend(anyList(), any()))
            .willReturn(true);

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));
        given(pdfLetterService.generateLetter(any(), any(), any())).willReturn(sampleDirectionCoversheet);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        assertEquals(ISSUE_FINAL_DECISION, ccdNotificationWrapperCaptor.getValue().getNotificationType());

        then(notificationHandler).should(times(2)).sendNotification(
            eq(ccdNotificationWrapper), any(), eq("Letter"),
            any(NotificationHandler.SendNotification.class));
    }

    @Test
    @Parameters({"DRAFT_TO_NON_COMPLIANT, NON_COMPLIANT"})
    public void givenNotificationTypeToBeOverriden_thenOverrideNotificationTypeAndSend(NotificationEventType receivedNotificationType, NotificationEventType sentNotificationType) throws IOException {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(receivedNotificationType, APPELLANT_WITH_ADDRESS, null, SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().setSubscriptions(null);

        Notification notification = new Notification(Template.builder().docmosisTemplateId(LETTER_TEMPLATE_ID).emailTemplateId(null).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        given(factory.create(ccdNotificationWrapperCaptor.capture(), eq(getSubscriptionWithType(ccdNotificationWrapper)))).willReturn(notification);
        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), any())).willReturn(true);
        given(notificationValidService.isNotificationStillValidToSend(anyList(), any()))
            .willReturn(true);

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));
        given(pdfLetterService.generateLetter(any(), any(), any())).willReturn(sampleDirectionCoversheet);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, true);

        assertEquals(sentNotificationType, ccdNotificationWrapperCaptor.getValue().getNotificationType());

        then(notificationHandler).should(atLeastOnce()).sendNotification(
            eq(ccdNotificationWrapper), any(), eq("Letter"),
            any(NotificationHandler.SendNotification.class));
    }

    @Test
    @Parameters({"DIRECTION_ISSUED, No", "DIRECTION_ISSUED, null", "DECISION_ISSUED, No", "DECISION_ISSUED, null",
        "DIRECTION_ISSUED_WELSH, No", "DIRECTION_ISSUED_WELSH, null", "DECISION_ISSUED_WELSH, No", "DECISION_ISSUED_WELSH, null",
        "ISSUE_FINAL_DECISION, No", "ISSUE_FINAL_DECISION, null", "ISSUE_FINAL_DECISION_WELSH, No", "ISSUE_FINAL_DECISION_WELSH, null", "ISSUE_ADJOURNMENT_NOTICE, No", "ISSUE_ADJOURNMENT_NOTICE, null"})
    public void givenReissueDocumentEventReceivedAndResendToAppellantNotSet_thenDoNotSendToAppellant(NotificationEventType notificationEventType, @Nullable String resendToAppellant) {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(REISSUE_DOCUMENT, APPELLANT_WITH_ADDRESS, null, SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setResendToAppellant(getYesNoFromString(resendToAppellant));
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setReissueFurtherEvidenceDocument(new DynamicList(notificationEventType.getId()));
        ccdNotificationWrapper.getNewSscsCaseData().setSubscriptions(null);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verifyNoInteractions(notificationHandler);
    }

    @Test
    @Parameters({"DIRECTION_ISSUED", "DECISION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED_WELSH", "ISSUE_FINAL_DECISION", "ISSUE_ADJOURNMENT_NOTICE"})
    public void givenReissueDocumentEventReceivedAndResendToRepYes_thenOverrideNotificationTypeAndSendToRep(NotificationEventType notificationEventType) throws IOException {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(REISSUE_DOCUMENT, APPELLANT_WITH_ADDRESS, Representative.builder().hasRepresentative("yes").address(Address.builder().line1("test").postcode("Bla").build()).build(), SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setResendToRepresentative(YesNo.YES);
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setReissueFurtherEvidenceDocument(new DynamicList(notificationEventType.getId()));
        ccdNotificationWrapper.getNewSscsCaseData().setSubscriptions(null);

        Notification notification = new Notification(Template.builder().docmosisTemplateId(LETTER_TEMPLATE_ID).emailTemplateId(null).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        given(factory.create(ccdNotificationWrapperCaptor.capture(), eq(getSubscriptionWithTypeRep(ccdNotificationWrapper)))).willReturn(notification);
        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), any())).willReturn(true);
        given(notificationValidService.isNotificationStillValidToSend(anyList(), any()))
            .willReturn(true);

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));
        given(pdfLetterService.generateLetter(any(), any(), any())).willReturn(sampleDirectionCoversheet);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        assertEquals(notificationEventType, ccdNotificationWrapperCaptor.getValue().getNotificationType());

        then(notificationHandler).should(atLeastOnce()).sendNotification(
            eq(ccdNotificationWrapper), any(), eq("Letter"),
            any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void givenReissueDocumentEventReceivedAndResendToRepYesAndEventWelshAndLongLetter_thenOverrideNotificationTypeAndSendToRepAndSendEnglishAndWelshSeparately() throws IOException {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(REISSUE_DOCUMENT, APPELLANT_WITH_ADDRESS, Representative.builder().hasRepresentative("yes").address(Address.builder().line1("test").postcode("Bla").build()).build(), SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setResendToRepresentative(YesNo.YES);
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setReissueFurtherEvidenceDocument(new DynamicList(ISSUE_FINAL_DECISION_WELSH.getId()));
        ccdNotificationWrapper.getNewSscsCaseData().setSubscriptions(null);

        Notification notification = new Notification(Template.builder().docmosisTemplateId(LETTER_TEMPLATE_ID).emailTemplateId(null).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        given(factory.create(ccdNotificationWrapperCaptor.capture(), eq(getSubscriptionWithTypeRep(ccdNotificationWrapper)))).willReturn(notification);
        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), any())).willReturn(true);
        given(notificationValidService.isNotificationStillValidToSend(anyList(), any()))
            .willReturn(true);

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));
        given(pdfLetterService.generateLetter(any(), any(), any())).willReturn(sampleDirectionCoversheet);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        assertEquals(ISSUE_FINAL_DECISION, ccdNotificationWrapperCaptor.getValue().getNotificationType());

        then(notificationHandler).should(times(2)).sendNotification(
            eq(ccdNotificationWrapper), any(), eq("Letter"),
            any(NotificationHandler.SendNotification.class));
    }

    @Test
    @Parameters({"DIRECTION_ISSUED, No", "DIRECTION_ISSUED, null", "DECISION_ISSUED, No", "DECISION_ISSUED, null",
        "DIRECTION_ISSUED, No", "DIRECTION_ISSUED, null", "DECISION_ISSUED, No", "DECISION_ISSUED, null",
        "ISSUE_FINAL_DECISION, No", "ISSUE_FINAL_DECISION, null", "ISSUE_FINAL_DECISION_WELSH, No", "ISSUE_FINAL_DECISION_WELSH, null", "ISSUE_ADJOURNMENT_NOTICE, No", "ISSUE_ADJOURNMENT_NOTICE, null"})
    public void givenReissueDocumentEventReceivedAndResendToRepNotSet_thenDoNotSendToRep(NotificationEventType notificationEventType, @Nullable String resendToRep) {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(REISSUE_DOCUMENT, APPELLANT_WITH_ADDRESS, Representative.builder().hasRepresentative("yes").build(), SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setResendToRepresentative(getYesNoFromString(resendToRep));
        ccdNotificationWrapper.getNewSscsCaseData().getReissueArtifactUi().setReissueFurtherEvidenceDocument(new DynamicList(notificationEventType.getId()));
        ccdNotificationWrapper.getNewSscsCaseData().setSubscriptions(null);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verifyNoInteractions(notificationHandler);
    }


    @Test
    @Parameters({"DIRECTION_ISSUED, Yes", "DECISION_ISSUED, Yes", "ISSUE_ADJOURNMENT_NOTICE, Yes", "PROCESS_AUDIO_VIDEO, Yes", "ISSUE_FINAL_DECISION, Yes", "ACTION_POSTPONEMENT_REQUEST, Yes"})
    public void givenIssueDocumentEventReceivedAndWelshLanguagePref_thenDoNotSendToNotifications(NotificationEventType notificationEventType, @Nullable String languagePrefWelsh) {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(notificationEventType, APPELLANT_WITH_ADDRESS, Representative.builder().hasRepresentative("no").build(), SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());

        ccdNotificationWrapper.getNewSscsCaseData().setState(State.WITH_DWP);
        ccdNotificationWrapper.getNewSscsCaseData().setSubscriptions(null);
        ccdNotificationWrapper.getNewSscsCaseData().setLanguagePreferenceWelsh(languagePrefWelsh);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verifyNoInteractions(notificationHandler);
    }

    @Test
    @Parameters({"issueDirectionsNotice", "excludeEvidence", "admitEvidence"})
    public void givenProcessAudioVideo_thenProcessNotificationForCertainActions(String action) {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(PROCESS_AUDIO_VIDEO, APPELLANT_WITH_ADDRESS, Representative.builder().hasRepresentative("no").build(), SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().setProcessAudioVideoAction(new DynamicList(new DynamicListItem(action, action), null));
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);
        Notification notification = new Notification(Template.builder().docmosisTemplateId(LETTER_TEMPLATE_ID).emailTemplateId(null).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        given(factory.create(ccdNotificationWrapperCaptor.capture(), any())).willReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);
        verify(reminderService).createReminders(ccdNotificationWrapper);
    }

    @Test
    @Parameters({"sendToJudge", "sendToAdmin"})
    public void givenProcessAudioVideo_thenDoProcessNotificationForCertainActions(String action) {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(PROCESS_AUDIO_VIDEO, APPELLANT_WITH_ADDRESS, Representative.builder().hasRepresentative("no").build(), SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().setProcessAudioVideoAction(new DynamicList(new DynamicListItem(action, action), null));
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);
        verifyNoInteractions(reminderService);
    }

    @Test
    @Parameters({"DIRECTION_ISSUED_WELSH, Yes", "DECISION_ISSUED_WELSH, Yes"})
    public void givenIssueDocumentEventReceivedAndEventWelsh_thenDoSendNotifications(NotificationEventType notificationEventType, @Nullable String languagePrefWelsh) {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(notificationEventType, APPELLANT_WITH_ADDRESS, Representative.builder().hasRepresentative("no").build(), SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().setState(State.WITH_DWP);
        ccdNotificationWrapper.getNewSscsCaseData().setSubscriptions(Subscriptions.builder().appellantSubscription(Subscription.builder()
            .tya(APPEAL_NUMBER)
            .email(EMAIL)
            .subscribeEmail(YES)
            .mobile(MOBILE_NUMBER_1)
            .subscribeSms(YES).wantSmsNotifications(YES)
            .build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().setLanguagePreferenceWelsh(languagePrefWelsh);

        String emailTemplateId = "abc";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), new HashMap<>(), new Reference(), null);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        then(notificationHandler).should(atLeastOnce()).sendNotification(
            eq(ccdNotificationWrapper), eq(emailTemplateId), eq("Email"),
            any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void givenIssueDocumentEventReceivedAndEventWelshAndLongLetter_thenSendEnglishAndWelshSeparately() {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(ISSUE_FINAL_DECISION_WELSH, APPELLANT_WITH_ADDRESS, Representative.builder().hasRepresentative("no").build(), SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().setState(State.WITH_DWP);
        ccdNotificationWrapper.getNewSscsCaseData().setSubscriptions(Subscriptions.builder().appellantSubscription(Subscription.builder()
            .tya(APPEAL_NUMBER)
            .email(EMAIL)
            .subscribeEmail(YES)
            .mobile(MOBILE_NUMBER_1)
            .subscribeSms(YES).wantSmsNotifications(YES)
            .build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().setLanguagePreferenceWelsh("Yes");

        String emailTemplateId = "abc";
        Notification notification = new Notification(Template.builder().emailTemplateId(emailTemplateId).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), new HashMap<>(), new Reference(), null);
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);

        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        then(notificationHandler).should(times(2)).sendNotification(
            eq(ccdNotificationWrapper), eq(emailTemplateId), eq("Email"),
            any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void givenDigitalCaseAndResponseReceived_willNotSendNotifications() {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(DWP_RESPONSE_RECEIVED, APPELLANT_WITH_ADDRESS, Representative.builder().hasRepresentative("no").build(), SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());

        ccdNotificationWrapper.getNewSscsCaseData().setState(State.WITH_DWP);
        ccdNotificationWrapper.getNewSscsCaseData().setCreatedInGapsFrom("readyToList");
        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        verifyNoInteractions(notificationValidService);
        verifyNoInteractions(notificationHandler);
    }

    @Test
    public void givenNonDigitalCaseAndResponseReceived_willSendNotifications() {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(DWP_RESPONSE_RECEIVED, APPELLANT_WITH_ADDRESS, Representative.builder().hasRepresentative("no").build(), SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        Notification notification = new Notification(Template.builder().emailTemplateId("emailTemplateId").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), new HashMap<>(), new Reference(), null);

        ccdNotificationWrapper.getNewSscsCaseData().setState(State.WITH_DWP);
        ccdNotificationWrapper.getNewSscsCaseData().setCreatedInGapsFrom("validAppeal");
        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);
        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        then(notificationHandler).should(atLeastOnce()).sendNotification(
            eq(ccdNotificationWrapper), eq("emailTemplateId"), eq("Email"),
            any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void givenUpdateOtherPartyDataReceivedAndNewOtherPartyHasBeenAdded_thenSendToLetterToOtherParty() throws IOException {

        final CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapperOtherParty(UPDATE_OTHER_PARTY_DATA, APPELLANT_WITH_ADDRESS, SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());


        Notification notification = new Notification(Template.builder().docmosisTemplateId(LETTER_TEMPLATE_ID).emailTemplateId(null).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        given(factory.create(ccdNotificationWrapperCaptor.capture(), any())).willReturn(notification);
        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), any())).willReturn(true);
        given(notificationValidService.isNotificationStillValidToSend(anyList(), any()))
            .willReturn(true);

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));
        given(pdfLetterService.generateLetter(any(), any(), any())).willReturn(sampleDirectionCoversheet);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        assertEquals(UPDATE_OTHER_PARTY_DATA, ccdNotificationWrapperCaptor.getValue().getNotificationType());

        then(notificationHandler).should(times(2)).sendNotification(
            eq(ccdNotificationWrapper), any(), eq("Letter"),
            any(NotificationHandler.SendNotification.class));
    }

    @Test
    public void givenDwpUploadResponseReceivedAndNewOtherPartyHasBeenAdded_thenOverrideNotificationTypeAndSendToLetterToOtherParty() throws IOException {

        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapperOtherParty(DWP_UPLOAD_RESPONSE, APPELLANT_WITH_ADDRESS, SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());

        ccdNotificationWrapper.getSscsCaseDataWrapper().getNewSscsCaseData().setCreatedInGapsFrom(READY_TO_LIST.getId());
        Notification notification = new Notification(Template.builder().docmosisTemplateId(LETTER_TEMPLATE_ID).emailTemplateId(null).smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms("07823456746").build(), new HashMap<>(), new Reference(), null);
        given(factory.create(ccdNotificationWrapperCaptor.capture(), any())).willReturn(notification);
        given(notificationValidService.isHearingTypeValidToSendNotification(
            any(SscsCaseData.class), any())).willReturn(true);
        given(notificationValidService.isNotificationStillValidToSend(anyList(), any()))
            .willReturn(true);

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));
        given(pdfLetterService.generateLetter(any(), any(), any())).willReturn(sampleDirectionCoversheet);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        assertEquals(UPDATE_OTHER_PARTY_DATA, ccdNotificationWrapperCaptor.getValue().getNotificationType());

        then(notificationHandler).should(times(6)).sendNotification(
            eq(ccdNotificationWrapper), any(), eq("Letter"),
            any(NotificationHandler.SendNotification.class));
    }


    @SuppressWarnings({"Indentation", "UnusedPrivateMethod"})
    private Object[] allEventTypesExceptRequestForInformationAndProcessingHearingRequest() {
        return Arrays.stream(NotificationEventType.values()).filter(eventType -> (
            !REQUEST_FOR_INFORMATION.equals(eventType)
                && !ACTION_HEARING_RECORDING_REQUEST.equals(eventType)
                && !EVENTS_FOR_ACTION_FURTHER_EVIDENCE.contains(eventType)
                && !EVENT_TYPES_FOR_BUNDLED_LETTER.contains(eventType))
        ).toArray();
    }

    @Test
    public void hasJustSubscribedNoChange_returnsFalse() {
        assertFalse(NotificationService.hasCaseJustSubscribed(subscription, subscription));
    }

    @Test
    public void hasJustSubscribedUnsubscribedEmailAndSms_returnsFalse() {
        Subscription newSubscription = subscription.toBuilder().subscribeEmail(NO).subscribeSms(NO).wantSmsNotifications(NO).build();
        assertFalse(NotificationService.hasCaseJustSubscribed(newSubscription, subscription));
    }

    @Test
    public void hasJustSubscribedEmailAndMobile_returnsTrue() {
        Subscription oldSubscription = subscription.toBuilder().subscribeEmail(NO).subscribeSms(NO).wantSmsNotifications(NO).build();
        assertTrue(NotificationService.hasCaseJustSubscribed(subscription, oldSubscription));
    }

    @Test
    public void hasJustSubscribedEmail_returnsTrue() {
        Subscription oldSubscription = subscription.toBuilder().subscribeEmail(NO).build();
        assertTrue(NotificationService.hasCaseJustSubscribed(subscription, oldSubscription));
    }

    @Test
    public void hasJustSubscribedSms_returnsTrue() {
        Subscription oldSubscription = subscription.toBuilder().subscribeSms(NO).wantSmsNotifications(NO).build();
        assertTrue(NotificationService.hasCaseJustSubscribed(subscription, oldSubscription));
    }

    @Test
    public void willNotSendHearingNotifications_whenGapsAndActionPostponementRequest() {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(POSTPONEMENT, APPELLANT_WITH_ADDRESS, null, null);
        ccdNotificationWrapper.getNewSscsCaseData().getSchedulingAndListingFields().setHearingRoute(HearingRoute.GAPS);

        SendNotificationService sendNotificationService = new SendNotificationService(notificationSender, notificationHandler, notificationValidService, pdfLetterService, pdfStoreService);

        final NotificationService notificationService = new NotificationService(factory, reminderService,
            notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService, false
        );

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        then(notificationHandler).shouldHaveNoMoreInteractions();
    }

    @Test
    public void willSendHearingNotifications_whenCaseIsListAssistAndActionPostponementRequest() {
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapper(POSTPONEMENT, APPELLANT_WITH_ADDRESS, Representative.builder().hasRepresentative("no").build(), SscsDocument.builder().value(SscsDocumentDetails.builder().build()).build());
        ccdNotificationWrapper.getNewSscsCaseData().getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);
        ccdNotificationWrapper.getNewSscsCaseData().setState(State.WITH_DWP);
        ccdNotificationWrapper.getNewSscsCaseData().setCreatedInGapsFrom("validAppeal");

        when(notificationValidService.isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when(notificationValidService.isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);
        Notification notification = new Notification(Template.builder().emailTemplateId("emailTemplateId").smsTemplateId(null).build(), Destination.builder().email("test@testing.com").sms(null).build(), new HashMap<>(), new Reference(), null);
        when(factory.create(ccdNotificationWrapper, getSubscriptionWithType(ccdNotificationWrapper))).thenReturn(notification);

        notificationService.manageNotificationAndSubscription(ccdNotificationWrapper, false);

        then(notificationHandler).should(atLeastOnce()).sendNotification(
            eq(ccdNotificationWrapper), eq("emailTemplateId"), eq("Email"),
            any(NotificationHandler.SendNotification.class));
    }

    private NotificationService getNotificationService() {
        SendNotificationService sendNotificationService = new SendNotificationService(notificationSender, notificationHandler, notificationValidService, pdfLetterService, pdfStoreService);

        final NotificationService notificationService = new NotificationService(factory, reminderService,
            notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService, false
        );
        return notificationService;
    }

    private CcdNotificationWrapper buildWrapperWithDocuments(NotificationEventType eventType, String fileUrl, Appellant appellant, Representative rep, String documentType) {

        SscsDocumentDetails sscsDocumentDetails = SscsDocumentDetails.builder()
            .documentType(documentType)
            .documentLink(
                DocumentLink.builder()
                    .documentUrl(fileUrl)
                    .documentFilename("direction-text.pdf")
                    .documentBinaryUrl(fileUrl + "/binary")
                    .build()
            )
            .build();

        SscsDocument sscsDocument = SscsDocument.builder().value(sscsDocumentDetails).build();

        return buildBaseWrapper(eventType, appellant, rep, sscsDocument);
    }

    private SubscriptionWithType getSubscriptionWithType(CcdNotificationWrapper ccdNotificationWrapper) {
        return new SubscriptionWithType(getSubscription(ccdNotificationWrapper.getNewSscsCaseData(),
            SubscriptionType.APPELLANT), SubscriptionType.APPELLANT,
            ccdNotificationWrapper.getNewSscsCaseData().getAppeal().getAppellant(),
            ccdNotificationWrapper.getNewSscsCaseData().getAppeal().getAppellant());
    }

    private SubscriptionWithType getSubscriptionWithTypeJoint(CcdNotificationWrapper ccdNotificationWrapper) {
        return new SubscriptionWithType(getSubscription(ccdNotificationWrapper.getNewSscsCaseData(), JOINT_PARTY),
            JOINT_PARTY, ccdNotificationWrapper.getNewSscsCaseData().getJointParty(),
            ccdNotificationWrapper.getNewSscsCaseData().getJointParty());
    }

    private SubscriptionWithType getSubscriptionWithTypeRep(CcdNotificationWrapper ccdNotificationWrapper) {
        return new SubscriptionWithType(getSubscription(ccdNotificationWrapper.getNewSscsCaseData(), REPRESENTATIVE),
            SubscriptionType.REPRESENTATIVE, ccdNotificationWrapper.getNewSscsCaseData().getAppeal().getAppellant(),
            ccdNotificationWrapper.getNewSscsCaseData().getAppeal().getRep());
    }

    private SubscriptionWithType getSubscriptionWithTypeOtherParty(CcdNotificationWrapper ccdNotificationWrapper, String otherPartyId) {
        Subscription otherPartySubs = ccdNotificationWrapper.getNewSscsCaseData().getOtherParties().stream()
            .map(o -> o.getValue())
            .flatMap(op -> Stream.of(Pair.of(op.getId(), op.getOtherPartySubscription()),
                (op.hasAppointee() ? Pair.of(op.getAppointee().getId(), op.getOtherPartyAppointeeSubscription()) : null),
                (op.hasRepresentative() ? Pair.of(op.getRep().getId(), op.getOtherPartyRepresentativeSubscription()) : null)))
            .filter(Objects::nonNull)
            .filter(p -> p.getLeft() != null && p.getRight() != null)
            .filter(p -> p.getLeft().equals(String.valueOf(otherPartyId)))
            .map(Pair::getRight)
            .findFirst()
            .orElse(null);
        OtherParty otherParty = ccdNotificationWrapper.getNewSscsCaseData().getOtherParties().get(0).getValue();
        return new SubscriptionWithType(otherPartySubs, SubscriptionType.OTHER_PARTY, otherParty, otherParty, otherPartyId);
    }

    public static CcdNotificationWrapper buildBaseWrapper(NotificationEventType eventType, Appellant appellant, Representative rep, SscsDocument sscsDocument) {
        return buildBaseWrapperWithCaseData(getSscsCaseDataBuilder(appellant, rep, sscsDocument).build(), getSscsCaseDataBuilder(appellant, rep, sscsDocument).build(), eventType);
    }

    private static List<OtherPartyOption> getOtherPartyOptions(YesNo resendToOtherParty) {
        return Collections.singletonList(OtherPartyOption
            .builder()
            .value(OtherPartyOptionDetails
                .builder()
                .otherPartyOptionId("3")
                .otherPartyOptionName("OPAppointee OP3 - Appointee")
                .resendToOtherParty(resendToOtherParty)
                .build())
            .build());
    }

    public static CcdNotificationWrapper buildBaseWrapperOtherParty(NotificationEventType eventType, Appellant appellant, SscsDocument sscsDocument) {
        final OtherParty otherParty1 = OtherParty.builder()
            .id("1")
            .name(Name.builder().firstName("OP").lastName("OP1").build())
            .address(Address.builder().line1("line 1").postcode("TS1 1ST").build())
            .sendNewOtherPartyNotification(YesNo.YES)
            .rep(Representative.builder()
                .id("2")
                .hasRepresentative(YES)
                .name(Name.builder().firstName("OPRep").lastName("OP2").build())
                .address(Address.builder().line1("line 1").postcode("TS2 2ST").build())
                .build())
            .isAppointee(YES)
            .appointee(Appointee.builder()
                .id("3")
                .name(Name.builder().firstName("OPAppointee").lastName("OP3").build())
                .address(Address.builder().line1("line 1").postcode("TS3 3ST").build())
                .build())
            .build();
        final OtherParty otherParty2 = OtherParty.builder()
            .id("4")
            .sendNewOtherPartyNotification(YesNo.NO)
            .name(Name.builder().firstName("OP").lastName("OP4").build())
            .address(Address.builder().line1("line 1").postcode("TS4 4ST").build())
            .build();
        SscsCaseData sscsCaseData = getSscsCaseDataBuilder(appellant, null, sscsDocument)
            .otherParties(List.of(otherParty1, otherParty2).stream()
                .map(CcdValue::new)
                .collect(Collectors.toList()))
            .functionalTest(YesNo.YES)
            .build();
        return buildBaseWrapperWithCaseData(sscsCaseData, eventType);
    }

    public static CcdNotificationWrapper buildBaseWrapperJointParty(NotificationEventType eventType, Appellant appellant, Name name, Address address, SscsDocument sscsDocument) {
        SscsCaseData sscsCaseData = getSscsCaseDataBuilder(appellant, null, sscsDocument)
            .jointParty(JointParty.builder()
                .hasJointParty(YesNo.YES)
                .name(name)
                .jointPartyAddressSameAsAppellant(address == null ? YesNo.YES : YesNo.NO)
                .address(address)
                .build())
            .build();
        CcdNotificationWrapper ccdNotificationWrapper = buildBaseWrapperWithCaseData(sscsCaseData, eventType);
        return ccdNotificationWrapper;
    }

    public static CcdNotificationWrapper buildBaseWrapperWithCaseData(SscsCaseData sscsCaseDataWithDocuments, NotificationEventType eventType) {
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseDataWithDocuments)
            .oldSscsCaseData(sscsCaseDataWithDocuments)
            .notificationEventType(eventType)
            .build();
        return new CcdNotificationWrapper(caseDataWrapper);
    }

    public static CcdNotificationWrapper buildBaseWrapperWithCaseData(SscsCaseData newSscsCaseDataWithDocuments, SscsCaseData oldSscsCaseDataWithDocuments, NotificationEventType eventType) {
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(newSscsCaseDataWithDocuments)
            .oldSscsCaseData(oldSscsCaseDataWithDocuments)
            .notificationEventType(eventType)
            .build();
        return new CcdNotificationWrapper(caseDataWrapper);
    }

    public static CcdNotificationWrapper buildBaseWrapperWithReasonableAdjustment() {
        SscsCaseData caseData = SscsCaseData.builder()
            .reasonableAdjustments(ReasonableAdjustments.builder()
                .appellant(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
                .appointee(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
                .representative(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
                .jointParty(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
                .build()).build();
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(caseData)
            .oldSscsCaseData(caseData)
            .notificationEventType(APPEAL_RECEIVED)
            .build();
        return new CcdNotificationWrapper(caseDataWrapper);
    }

    protected static SscsCaseData.SscsCaseDataBuilder getSscsCaseDataBuilder(Appellant appellant, Representative rep, SscsDocument sscsDocument) {
        return SscsCaseData.builder()
            .appeal(
                Appeal
                    .builder()
                    .benefitType(BenefitType.builder().code(Benefit.PIP.name()).description(Benefit.PIP.getDescription()).build())
                    .receivedVia("Online")
                    .hearingType(AppealHearingType.ORAL.name())
                    .hearingOptions(HearingOptions.builder().wantsToAttend(YES).build())
                    .appellant(appellant)
                    .rep(rep)
                    .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeEmail(YES)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build()
                )
                .build())
            .caseReference(CASE_REFERENCE)
            .dwpState(DwpState.RESPONSE_SUBMITTED_DWP)
            .sscsInterlocDecisionDocument(SscsInterlocDecisionDocument.builder().documentLink(DocumentLink.builder().documentUrl("http://dm-store:4506/documents/1e1eb3d2-5b6c-430d-8dad-ebcea1ad7ecf")
                .documentFilename("test.pdf")
                .documentBinaryUrl("test/binary").build()).build())
            .sscsInterlocDirectionDocument(SscsInterlocDirectionDocument.builder().documentLink(DocumentLink.builder().documentUrl("http://dm-store:4506/documents/1e1eb3d2-5b6c-430d-8dad-ebcea1ad7ecf")
                .documentFilename("test.pdf")
                .documentBinaryUrl("test/binary").build()).build())
            .sscsStrikeOutDocument(SscsStrikeOutDocument.builder().documentLink(DocumentLink.builder().documentUrl("http://dm-store:4506/documents/1e1eb3d2-5b6c-430d-8dad-ebcea1ad7ecf")
                .documentFilename("test.pdf")
                .documentBinaryUrl("test/binary").build()).build())
            .ccdCaseId(CASE_ID)
            .hearings(emptyList())
            .sscsDocument(new ArrayList<>(singletonList(sscsDocument)));
    }

    protected static SscsCaseData.SscsCaseDataBuilder getSscsCaseDataBuilderSettingInformationFromAppellant(Appellant appellant, Representative rep, SscsDocument sscsDocument, String informationFromAppellant) {
        return SscsCaseData.builder()
            .appeal(
                Appeal
                    .builder()
                    .hearingType(AppealHearingType.ORAL.name())
                    .hearingOptions(HearingOptions.builder().wantsToAttend(YES).build())
                    .appellant(appellant)
                    .rep(rep)
                    .build())
            .dwpState(DwpState.RESPONSE_SUBMITTED_DWP)
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .tya(APPEAL_NUMBER)
                    .email(EMAIL)
                    .mobile(MOBILE_NUMBER_1)
                    .subscribeEmail(YES)
                    .subscribeSms(YES).wantSmsNotifications(YES)
                    .build()
                )
                .build())
            .caseReference(CASE_REFERENCE)
            .sscsInterlocDecisionDocument(SscsInterlocDecisionDocument.builder().documentLink(DocumentLink.builder().documentUrl("http://dm-store:4506/documents/1e1eb3d2-5b6c-430d-8dad-ebcea1ad7ecf")
                .documentFilename("test.pdf")
                .documentBinaryUrl("test/binary").build()).build())
            .sscsInterlocDirectionDocument(SscsInterlocDirectionDocument.builder().documentLink(DocumentLink.builder().documentUrl("http://dm-store:4506/documents/1e1eb3d2-5b6c-430d-8dad-ebcea1ad7ecf")
                .documentFilename("test.pdf")
                .documentBinaryUrl("test/binary").build()).build())
            .sscsStrikeOutDocument(SscsStrikeOutDocument.builder().build().builder().documentLink(DocumentLink.builder().documentUrl("http://dm-store:4506/documents/1e1eb3d2-5b6c-430d-8dad-ebcea1ad7ecf")
                .documentFilename("test.pdf")
                .documentBinaryUrl("test/binary").build()).build())
            .ccdCaseId(CASE_ID)
            .hearings(List.of(Hearing.builder().value(HearingDetails.builder().build()).build()))
            .sscsDocument(new ArrayList<>(singletonList(sscsDocument)))
            .informationFromAppellant(informationFromAppellant);
    }

    private void verifyExpectedLogErrorCount(Appender<ILoggingEvent> mockAppender, ArgumentCaptor captorLoggingEvent, int wantedNumberOfEmailNotificationsSent, int wantedNumberOfSmsNotificationsSent) {
        int expectedErrors = 0;
        if (wantedNumberOfEmailNotificationsSent > 0
            || wantedNumberOfSmsNotificationsSent > 0) {
            expectedErrors = 1;
        }
        verify(mockAppender, atLeast(expectedErrors)).doAppend(
            (ILoggingEvent) captorLoggingEvent.capture()
        );
        List<ILoggingEvent> logEvents = (List<ILoggingEvent>) captorLoggingEvent.getAllValues();
        if (expectedErrors == 0) {
            if (logEvents.isEmpty()) {
                assertEquals(logEvents.size(), expectedErrors);
            } else {
                assertFalse(logEvents.stream().noneMatch(e -> e.getLevel().equals(Level.ERROR)));
            }
        } else {
            assertTrue(logEvents.stream().noneMatch(e -> e.getLevel().equals(Level.ERROR)));
        }
    }

    protected static void verifyNoErrorsLogged(Appender<ILoggingEvent> mockAppender, ArgumentCaptor captorLoggingEvent) {
        verify(mockAppender, atLeast(0)).doAppend(
            (ILoggingEvent) captorLoggingEvent.capture()
        );
        List<ILoggingEvent> logEvents = (List<ILoggingEvent>) captorLoggingEvent.getAllValues();
        assertTrue(logEvents.stream().noneMatch(e -> e.getLevel().equals(Level.ERROR)));
    }

    protected static void verifyExpectedLogMessage(Appender<ILoggingEvent> mockAppender, ArgumentCaptor captorLoggingEvent, String ccdCaseId, String errorMessage, Level logLevel) {
        verify(mockAppender, atLeastOnce()).doAppend(
            (ILoggingEvent) captorLoggingEvent.capture()
        );
        List<ILoggingEvent> logEvents = (List<ILoggingEvent>) captorLoggingEvent.getAllValues();
        assertFalse(logEvents.stream().noneMatch(e -> e.getLevel().equals(logLevel)));
        assertEquals(1, logEvents.stream().filter(logEvent -> logEvent.getFormattedMessage().contains(errorMessage)).count());
        assertTrue(logEvents.stream().filter(logEvent -> logEvent.getFormattedMessage().contains(ccdCaseId)).count() >= 1);
    }

    private static void verifyErrorLogMessageNotLogged(Appender<ILoggingEvent> mockAppender, ArgumentCaptor captorLoggingEvent, String errorText) {
        verify(mockAppender, atLeast(0)).doAppend(
            (ILoggingEvent) captorLoggingEvent.capture()
        );
        List<ILoggingEvent> logEvents = (List<ILoggingEvent>) captorLoggingEvent.getAllValues();
        assertEquals(0, logEvents.stream().filter(logEvent -> logEvent.getFormattedMessage().contains(errorText)).count());
    }

    private YesNo getYesNoFromString(String yesNoString) {

        if (YesNo.YES.getValue().equalsIgnoreCase(yesNoString)) {
            return YesNo.YES;
        } else if (YesNo.NO.getValue().equalsIgnoreCase(yesNoString)) {
            return YesNo.NO;
        }
        return null;
    }

    @Test
    @Parameters({"CORRECTION_REQUEST", "LIBERTY_TO_APPLY_REQUEST", "STATEMENT_OF_REASONS_REQUEST", "SET_ASIDE_REQUEST"})
    public void givenNotificationEventTypeAndSenderIsInValid_shouldManageNotificationAndSubscriptionAccordingly(NotificationEventType eventType) {
        DynamicList sender = new DynamicList(new DynamicListItem("dwp", "dwp"), new ArrayList<>());
        SscsCaseData caseData = SscsCaseData.builder().ccdCaseId("1234").originalSender(sender).build();
        CcdNotificationWrapper notificationWrapper = new CcdNotificationWrapper(
            NotificationSscsCaseDataWrapper.builder().notificationEventType(eventType).newSscsCaseData(caseData).build());

        notificationService.manageNotificationAndSubscription(notificationWrapper, false);
        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, notificationWrapper.getNewSscsCaseData().getCcdCaseId(),
            "Incomplete Information with empty or no Information regarding sender for event", Level.INFO);
    }

    @DisplayName("When the original sender is null and Event type is VALID_SEND_TO_INTERLOC then return false.")
    @Test
    @Parameters({"CORRECTION_REQUEST", "LIBERTY_TO_APPLY_REQUEST", "STATEMENT_OF_REASONS_REQUEST", "SET_ASIDE_REQUEST"})
    public void isNotificationStillValidToSendSetAsideRequest_senderIsNull_returnFalse(NotificationEventType eventType) {
        SscsCaseData caseData = SscsCaseData.builder().ccdCaseId("1234").originalSender(null).build();
        assertFalse(notificationService.isNotificationStillValidToSendSetAsideRequest(caseData, eventType));
    }

    @DisplayName("When the original sender is not null and sender is dwp then return false.")
    @Test
    @Parameters({"CORRECTION_REQUEST", "LIBERTY_TO_APPLY_REQUEST", "STATEMENT_OF_REASONS_REQUEST", "SET_ASIDE_REQUEST"})
    public void isNotificationStillValidToSendSetAsideRequest_senderIsInValid_returnFalse(NotificationEventType eventType) {
        DynamicList sender = new DynamicList(new DynamicListItem("dwp", "dwp"), new ArrayList<>());
        SscsCaseData caseData = SscsCaseData.builder().ccdCaseId("1234").originalSender(sender).build();
        assertFalse(notificationService.isNotificationStillValidToSendSetAsideRequest(caseData, eventType));
    }

    @DisplayName("When the original sender is not null, Event type is VALID_SEND_TO_INTERLOC and sender is other than dwp "
        + "then return true.")
    @Test
    @Parameters({"CORRECTION_REQUEST", "LIBERTY_TO_APPLY_REQUEST", "STATEMENT_OF_REASONS_REQUEST", "SET_ASIDE_REQUEST"})
    public void isNotificationStillValidToSendSetAsideRequest_senderIsValid_returnFalse(NotificationEventType eventType) {
        DynamicList sender = new DynamicList(new DynamicListItem("appellant", "appellant"), new ArrayList<>());
        SscsCaseData caseData = SscsCaseData.builder().ccdCaseId("1234").originalSender(sender).build();
        assertTrue(notificationService.isNotificationStillValidToSendSetAsideRequest(caseData, eventType));
    }

    @DisplayName("When the original sender is not null, Event type is other than VALID_SEND_TO_INTERLOC and sender is other than dwp "
        + " then return true.")
    @Test
    public void isNotificationStillValidToSendSetAsideRequest_InValidEventType_returnTrue() {
        DynamicList sender = new DynamicList(new DynamicListItem("dwp1", "dwp1"), new ArrayList<>());
        SscsCaseData caseData = SscsCaseData.builder().ccdCaseId("1234").originalSender(sender).build();
        assertTrue(notificationService.isNotificationStillValidToSendSetAsideRequest(caseData, ADJOURNED));
    }
}
