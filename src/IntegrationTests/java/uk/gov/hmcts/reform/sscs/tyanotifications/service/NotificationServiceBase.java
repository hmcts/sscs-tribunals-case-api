package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.docmosis.PdfLetterService;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
@Getter
public class NotificationServiceBase {
    static final String DATE = "2018-01-01T14:01:18.243";
    private static final String APPEAL_NUMBER = "GLSCRR";
    static final String YES = "Yes";
    static final String NO = "No";
    static final String CASE_REFERENCE = "ABC123";
    static final String CASE_ID = "1000001";
    static final String EMAIL_TEST_1 = "test1@email.com";
    static final String EMAIL_TEST_2 = "test2@email.com";
    static final String MOBILE_NUMBER_1 = "+447983495065";
    static final String MOBILE_NUMBER_2 = "+447123456789";

    @Setter
    private NotificationService notificationService;

    @Autowired
    private NotificationValidService notificationValidService;

    @Autowired
    private NotificationFactory notificationFactory;

    @Autowired
    private NotificationConfig notificationConfig;

    @SpyBean
    private NotificationHandler notificationHandler;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private ReminderService reminderService;

    @Mock
    private OutOfHoursCalculator outOfHoursCalculator;

    @Mock
    private PdfStoreService pdfStoreService;

    @Mock
    private PdfLetterService pdfLetterService;

    @Mock
    private IdamService idamService;

    private final Subscription subscription = Subscription.builder()
        .tya(NotificationServiceBase.APPEAL_NUMBER)
        .email(NotificationServiceBase.EMAIL_TEST_1)
        .mobile(NotificationServiceBase.MOBILE_NUMBER_1)
        .subscribeEmail(NotificationServiceBase.YES)
        .subscribeSms(NotificationServiceBase.YES)
        .wantSmsNotifications(NotificationServiceBase.YES)
        .build();


    @BeforeEach
    public void setup() {
        openMocks(this);
        notificationService = initialiseNotificationService();

        Mockito.when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);

        String authHeader = "authHeader";
        String serviceAuthHeader = "serviceAuthHeader";
        IdamTokens idamTokens = IdamTokens.builder().idamOauth2Token(authHeader).serviceAuthorization(serviceAuthHeader).build();

        Mockito.when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    NotificationService initialiseNotificationService() {
        SendNotificationService sendNotificationService = new SendNotificationService(notificationSender,
            notificationHandler, notificationValidService, pdfLetterService, pdfStoreService);
        return new NotificationService(notificationFactory, reminderService, notificationValidService,
            notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService, false
        );
    }

    public SscsCaseData getSscsCaseData(Subscription subscription, String who) {
        if (who.equals("appellant")) {
            return getSscsCaseData(subscription);
        } else if (who.equals("representative")) {
            return getSscsCaseDataForRep(subscription);
        }
        return getSscsCaseDataForAppointee(subscription);
    }

    public SscsCaseData getSscsCaseData(Subscription subscription) {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(NotificationServiceBase.DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        return SscsCaseData.builder().ccdCaseId(NotificationServiceBase.CASE_ID).events(events)
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(NotificationServiceBase.DATE).dwpIssuingOffice("office").build())
                .appealReasons(AppealReasons.builder().build())
                .rep(Representative.builder()
                    .hasRepresentative(NotificationServiceBase.YES)
                    .name(Name.builder().firstName("Rep").lastName("lastName").build())
                    .contact(Contact.builder().email(NotificationServiceBase.EMAIL_TEST_2).phone(NotificationServiceBase.MOBILE_NUMBER_2).build())
                    .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                    .build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("firstName").lastName("lastName").build())
                    .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .contact(Contact.builder().email(NotificationServiceBase.EMAIL_TEST_1).phone(NotificationServiceBase.MOBILE_NUMBER_1).build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build()).build())
                .hearingType(AppealHearingType.ORAL.name())
                .benefitType(BenefitType.builder().code(Benefit.PIP.name()).build())
                .hearingOptions(HearingOptions.builder()
                    .wantsToAttend(NotificationServiceBase.YES)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(subscription)
                .representativeSubscription(getSubscription().toBuilder().tya("REP_TYA").build())
                .build())
            .caseReference(NotificationServiceBase.CASE_REFERENCE).build();
    }

    public SscsCaseData getSscsCaseDataForRep(Subscription subscription) {
        Subscription appellantSubscription = getSubscription().toBuilder().tya("APPELLANT_TYA").build();
        SscsCaseData sscsCaseData = getSscsCaseData(appellantSubscription);
        return sscsCaseData.toBuilder()
            .subscriptions(sscsCaseData.getSubscriptions().toBuilder().representativeSubscription(subscription).build())
            .build();
    }

    public SscsCaseData getSscsCaseDataForAppointee(Subscription subscription) {
        SscsCaseData sscsCaseData = getSscsCaseData(subscription);
        sscsCaseData.getAppeal().getAppellant().setAppointee(Appointee.builder()
            .name(Name.builder().firstName("Appoin").lastName("Tee").build())
            .address(sscsCaseData.getAppeal().getAppellant().getAddress())
            .contact(sscsCaseData.getAppeal().getAppellant().getContact())
            .identity(sscsCaseData.getAppeal().getAppellant().getIdentity())
            .build());
        return sscsCaseData.toBuilder()
            .appeal(sscsCaseData.getAppeal().toBuilder().appellant(sscsCaseData.getAppeal().getAppellant()).build())
            .subscriptions(sscsCaseData.getSubscriptions().toBuilder()
                .appellantSubscription(null)
                .appointeeSubscription(subscription).build())
            .build();
    }

    public NotificationSscsCaseDataWrapper getSscsCaseDataWrapper(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData,
                                                                  NotificationEventType subscriptionUpdatedNotification) {
        return NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(newSscsCaseData)
            .oldSscsCaseData(oldSscsCaseData)
            .notificationEventType(subscriptionUpdatedNotification).build();
    }
}
