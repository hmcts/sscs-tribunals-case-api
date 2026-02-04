package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasons;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.docmosis.PdfLetterService;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
@Getter
public class NotificationProcessingServiceBaseTest {

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
    private NotificationProcessingService notificationProcessingService;

    @Autowired
    private NotificationValidService notificationValidService;

    @Autowired
    private NotificationFactory notificationFactory;

    @Autowired
    private NotificationConfig notificationConfig;

    @MockitoSpyBean
    private NotificationExecutionManager notificationExecutionManager;

    @Mock
    private NotificationGateway notificationGateway;

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
        .tya(NotificationProcessingServiceBaseTest.APPEAL_NUMBER)
        .email(NotificationProcessingServiceBaseTest.EMAIL_TEST_1)
        .mobile(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_1)
        .subscribeEmail(NotificationProcessingServiceBaseTest.YES)
        .subscribeSms(NotificationProcessingServiceBaseTest.YES)
        .wantSmsNotifications(NotificationProcessingServiceBaseTest.YES)
        .build();

    private AutoCloseable mocks;

    @BeforeEach
    public void setup() {
        mocks = openMocks(this);
        notificationProcessingService = initialiseNotificationService();

        Mockito.when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);

        String authHeader = "authHeader";
        String serviceAuthHeader = "serviceAuthHeader";
        IdamTokens idamTokens = IdamTokens.builder().idamOauth2Token(authHeader).serviceAuthorization(serviceAuthHeader).build();

        Mockito.when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    NotificationProcessingService initialiseNotificationService() {
        NotificationDispatchService notificationDispatchService = new NotificationDispatchService(notificationGateway,
                notificationExecutionManager, notificationValidService, pdfLetterService, pdfStoreService);
        return new NotificationProcessingService(notificationFactory, reminderService, notificationValidService,
                notificationExecutionManager, outOfHoursCalculator, notificationConfig, notificationDispatchService, false
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
        events.add(Event.builder().value(EventDetails.builder().date(NotificationProcessingServiceBaseTest.DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        return SscsCaseData.builder().ccdCaseId(NotificationProcessingServiceBaseTest.CASE_ID).events(events)
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(NotificationProcessingServiceBaseTest.DATE).dwpIssuingOffice("office").build())
                .appealReasons(AppealReasons.builder().build())
                .rep(Representative.builder()
                    .hasRepresentative(NotificationProcessingServiceBaseTest.YES)
                    .name(Name.builder().firstName("Rep").lastName("lastName").build())
                    .contact(Contact.builder().email(NotificationProcessingServiceBaseTest.EMAIL_TEST_2).phone(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_2).build())
                    .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                    .build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("firstName").lastName("lastName").build())
                    .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .contact(Contact.builder().email(NotificationProcessingServiceBaseTest.EMAIL_TEST_1).phone(NotificationProcessingServiceBaseTest.MOBILE_NUMBER_1).build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build()).build())
                .hearingType(AppealHearingType.ORAL.name())
                .benefitType(BenefitType.builder().code(Benefit.PIP.name()).build())
                .hearingOptions(HearingOptions.builder()
                    .wantsToAttend(NotificationProcessingServiceBaseTest.YES)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(subscription)
                .representativeSubscription(getSubscription().toBuilder().tya("REP_TYA").build())
                .build())
            .caseReference(NotificationProcessingServiceBaseTest.CASE_REFERENCE).build();
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
