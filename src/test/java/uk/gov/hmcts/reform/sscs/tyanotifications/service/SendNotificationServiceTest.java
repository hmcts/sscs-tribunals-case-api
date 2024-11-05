package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.REP_SALUTATION;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.EVENT_TYPES_FOR_BUNDLED_LETTER;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.ADDRESS_LINE_1;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.ADDRESS_LINE_2;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.ADDRESS_LINE_3;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.ADDRESS_LINE_4;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.ADDRESS_LINE_5;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.getAddressToUseForLetter;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationServiceTest.verifyExpectedLogMessage;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationServiceTest.verifyNoErrorsLogged;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.SendNotificationHelper.getRepSalutation;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.SendNotificationService.getAddressPlaceholders;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.SendNotificationService.getBundledLetterDocumentUrl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Destination;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.docmosis.PdfLetterService;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public class SendNotificationServiceTest {
    private static final String YES = "Yes";
    public static final String NO = "No";
    private static final String CASE_REFERENCE = "ABC123";
    private static final String CASE_ID = "1000001";

    private static Appellant APPELLANT_WITH_NO_ADDRESS = Appellant.builder()
        .name(Name.builder().firstName("Ap").lastName("pellant").build())
        .build();

    private static Appellant APPELLANT_WITH_EMPTY_ADDRESS = Appellant.builder()
        .name(Name.builder().firstName("Ap").lastName("pellant").build())
        .address(Address.builder().line1("").postcode("").build())
        .build();

    private static Appellant APPELLANT_WITH_ADDRESS = Appellant.builder()
        .name(Name.builder().firstName("Ap").lastName("pellant").build())
        .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
        .build();

    private static Appointee APPOINTEE_WITH_ADDRESS = Appointee.builder()
        .address(Address.builder().line1("Appointee Line 1").town("Appointee Town").county("Appointee County").postcode("AP9 0IN").build())
        .name(Name.builder().firstName("Ap").lastName("Pointee").build())
        .build();

    protected static Appellant APPELLANT_WITH_ADDRESS_AND_APPOINTEE = Appellant.builder()
        .name(Name.builder().firstName("Ap").lastName("Pellant").build())
        .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
        .appointee(APPOINTEE_WITH_ADDRESS)
        .isAppointee("Yes")
        .build();

    protected static Representative REP_WITH_ADDRESS = Representative.builder()
        .name(Name.builder().firstName("Re").lastName("Presentative").build())
        .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
        .build();

    protected static Address JOINT_PARTY_ADDRESS = Address.builder().line1("JP Line 1")
        .town("Jp Town").county("Jp County").postcode("RE9 3LL").build();

    private static Representative REP_ORG_WITH_ADDRESS = Representative.builder()
        .organisation("Rep Org")
        .address(Address.builder().line1("Rep Org Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
        .build();

    private static Representative REP_ORG_WITH_NAME_AND_ADDRESS = Representative.builder()
        .organisation("Rep Org")
        .name(Name.builder().firstName("Re").lastName("Presentative").build())
        .address(Address.builder().line1("Rep Org Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
        .build();

    private static Subscription SMS_SUBSCRIPTION = Subscription.builder().mobile("07831292000").subscribeSms("Yes").wantSmsNotifications("Yes").build();

    private static Notification SMS = Notification.builder()
        .destination(Destination.builder().sms("07831292000").build())
        .template(Template.builder().smsTemplateId(Arrays.asList("someSmsTemplateId")).build())
        .build();

    private static Notification WELSH_SMS = Notification.builder()
        .destination(Destination.builder().sms("07831292000").build())
        .template(Template.builder().smsTemplateId(Arrays.asList("englishSmsTemplateId", "welshSmsTemplateId")).build())
        .build();

    private static Subscription EMAIL_SUBSCRIPTION = Subscription.builder().email("test@some.com").subscribeEmail("Yes").build();

    private static Notification EMAIL = Notification.builder()
        .destination(Destination.builder().email("test@some.com").build())
        .template(Template.builder().emailTemplateId("someEmailTemplateId").build())
        .build();

    private static Subscription EMPTY_SUBSCRIPTION = Subscription.builder().build();

    private static Notification EMPTY_TEMPLATE = Notification.builder()
        .destination(Destination.builder().build())
        .template(Template.builder().build())
        .build();

    private static Notification LETTER = Notification.builder()
        .destination(Destination.builder().build())
        .template(Template.builder().letterTemplateId("someLetterTemplateId").build())
        .placeholders(new HashMap<>())
        .build();

    private static Notification DOCMOSIS_LETTER = Notification.builder()
        .destination(Destination.builder().build())
        .template(Template.builder().docmosisTemplateId("AWord.doc").letterTemplateId("someLetterTemplateId").build())
        .placeholders(new HashMap<>())
        .build();

    private static ReasonableAdjustments REASONABLE_ADJUSTMENTS = ReasonableAdjustments.builder()
        .appellant(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
        .representative(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
        .jointParty(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
        .appointee(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
        .build();

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private PdfStoreService pdfStoreService;

    @Mock
    private NotificationHandler notificationHandler;

    @Mock
    private NotificationValidService notificationValidService;

    @Mock
    private PdfLetterService pdfLetterService;

    private SendNotificationService classUnderTest;

    @Mock
    private Appender<ILoggingEvent> mockAppender;
    @Captor
    private ArgumentCaptor captorLoggingEvent;


    @Captor
    private ArgumentCaptor<String> smsTemplateIdCaptor;

    @Captor
    private ArgumentCaptor<NotificationHandler.SendNotification> sender;

    @Before
    public void setup() {
        openMocks(this);

        classUnderTest = new SendNotificationService(notificationSender, notificationHandler, notificationValidService, pdfLetterService, pdfStoreService);

        Logger logger = (Logger) LoggerFactory.getLogger(SendNotificationService.class.getName());
        logger.addAppender(mockAppender);
    }

    @Test
    public void getAppellantAddressToUseForLetter() {
        Address expectedAddress = APPELLANT_WITH_ADDRESS.getAddress();
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS);

        Address actualAddress = getAddressToUseForLetter(wrapper, new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            APPELLANT, wrapper.getNewSscsCaseData().getAppeal().getAppellant(),
            wrapper.getNewSscsCaseData().getAppeal().getAppellant()));
        assertEquals(expectedAddress.getLine1(), actualAddress.getLine1());
        assertEquals(expectedAddress.getLine2(), actualAddress.getLine2());
        assertEquals(expectedAddress.getTown(), actualAddress.getTown());
        assertEquals(expectedAddress.getCounty(), actualAddress.getCounty());
        assertEquals(expectedAddress.getPostcode(), actualAddress.getPostcode());
    }

    @Test
    public void getAppointeeAddressToUseForLetter() {
        Address expectedAddress = APPOINTEE_WITH_ADDRESS.getAddress();
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS_AND_APPOINTEE);

        Address actualAddress = getAddressToUseForLetter(wrapper, new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            APPOINTEE, wrapper.getNewSscsCaseData().getAppeal().getAppellant(),
            wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee()));
        assertEquals(expectedAddress.getLine1(), actualAddress.getLine1());
        assertEquals(expectedAddress.getLine2(), actualAddress.getLine2());
        assertEquals(expectedAddress.getTown(), actualAddress.getTown());
        assertEquals(expectedAddress.getCounty(), actualAddress.getCounty());
        assertEquals(expectedAddress.getPostcode(), actualAddress.getPostcode());
    }

    @Test
    public void getRepAddressToUseForLetter() {
        Address expectedAddress = REP_WITH_ADDRESS.getAddress();
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS_AND_APPOINTEE, STRUCK_OUT, REP_WITH_ADDRESS);

        Address actualAddress = getAddressToUseForLetter(wrapper, new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            REPRESENTATIVE, wrapper.getNewSscsCaseData().getAppeal().getAppellant(),
            wrapper.getNewSscsCaseData().getAppeal().getRep()));
        assertEquals(expectedAddress.getLine1(), actualAddress.getLine1());
        assertEquals(expectedAddress.getLine2(), actualAddress.getLine2());
        assertEquals(expectedAddress.getTown(), actualAddress.getTown());
        assertEquals(expectedAddress.getCounty(), actualAddress.getCounty());
        assertEquals(expectedAddress.getPostcode(), actualAddress.getPostcode());
    }

    @Test
    public void sendLetterNotificationForAppellant() throws NotificationClientException {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.APPELLANT, null, null);
        classUnderTest.sendLetterNotificationToAddress(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId()), LETTER, APPELLANT_WITH_ADDRESS.getAddress(), appellantEmptySubscription);

        verify(notificationSender).sendLetter(eq(LETTER.getLetterTemplate()), eq(APPELLANT_WITH_ADDRESS.getAddress()), any(), any(), any(), any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void sendLetterNotificationForRep() throws NotificationClientException {
        SubscriptionWithType representativeEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.REPRESENTATIVE, null, null);
        classUnderTest.sendLetterNotificationToAddress(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS), LETTER, REP_WITH_ADDRESS.getAddress(), representativeEmptySubscription);

        verify(notificationSender).sendLetter(eq(LETTER.getLetterTemplate()), eq(REP_WITH_ADDRESS.getAddress()), any(), any(), any(), any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void sendLetterNotificationForRepWithOrgName() throws NotificationClientException {
        SubscriptionWithType representativeEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.REPRESENTATIVE, null, null);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_ADDRESS);
        classUnderTest.sendLetterNotificationToAddress(wrapper, LETTER, REP_WITH_ADDRESS.getAddress(), representativeEmptySubscription);

        verify(notificationSender).sendLetter(eq(LETTER.getLetterTemplate()), eq(REP_WITH_ADDRESS.getAddress()), any(), any(), any(), any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void sendLetterNotificationForJointParty() throws NotificationClientException {
        SubscriptionWithType jointPartyEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, JOINT_PARTY,
            null, null);
        final CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, CASE_UPDATED, Name.builder().firstName("Jp").lastName("Party").build(), JOINT_PARTY_ADDRESS);
        classUnderTest.sendLetterNotificationToAddress(wrapper, LETTER, JOINT_PARTY_ADDRESS, jointPartyEmptySubscription);

        verify(notificationSender).sendLetter(eq(LETTER.getLetterTemplate()), eq(JOINT_PARTY_ADDRESS), any(), any(), any(), any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void doNotSendLetterNotificationIfAddressEmpty() throws NotificationClientException {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.APPELLANT, null, null);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_EMPTY_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.sendLetterNotification(wrapper, LETTER, appellantEmptySubscription, NotificationEventType.CASE_UPDATED);

        verifyNoInteractions(notificationSender);
        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Failed to send letter for event id", Level.ERROR);
    }

    @Test
    public void doNotSendLetterNotificationIfNoAddress() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.APPELLANT, null, null);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_NO_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.sendLetterNotification(wrapper, LETTER, appellantEmptySubscription, NotificationEventType.CASE_UPDATED);

        verifyNoInteractions(notificationSender);
        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Failed to send letter for event id", Level.ERROR);
    }

    @Test
    public void logErrorMessageWhenNoNotificationSent() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.APPELLANT, null, null);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_NO_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.sendEmailSmsLetterNotification(
            wrapper,
            LETTER,
            appellantEmptySubscription,
            APPEAL_RECEIVED
        );

        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Did not send a notification for event", Level.ERROR);
    }

    @Test
    public void getRepNameWhenRepHasName() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS);
        assertEquals(REP_WITH_ADDRESS.getName().getFullNameNoTitle(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepOrganisationWhenRepHasOrgButNoName() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_ADDRESS);
        assertEquals(wrapper.getNewSscsCaseData().getAppeal().getRep().getOrganisation(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepSalutationWhenRepHasOrgAndNoNameAndIngnoreOrgFlagSet() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_ADDRESS);
        assertEquals(REP_SALUTATION, getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), true));
    }

    @Test
    public void getRepOrganisationWhenRepHasOrgButNameSetToUndefined() {
        Representative repOrgWithAddressUndefinedName = Representative.builder()
            .organisation("Rep Org")
            .name(Name.builder().firstName("undefined").lastName("undefined").build())
            .address(Address.builder().line1("Rep Org Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
            .build();

        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_ADDRESS);
        assertEquals(repOrgWithAddressUndefinedName.getOrganisation(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepSalutationWhenRepHasNoOrgAndNameSetToUndefined() {
        Representative repWithAddressAndUndefinedName = Representative.builder()
            .name(Name.builder().firstName("undefined").lastName("undefined").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
            .build();

        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, repWithAddressAndUndefinedName);
        assertEquals(REP_SALUTATION, getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepSalutationWhenRepHasNoOrgAndNameSetToEmptyString() {
        Representative repWithAddressNoName = Representative.builder()
            .name(Name.builder().firstName("").lastName("").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
            .build();

        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, repWithAddressNoName);
        assertEquals(REP_SALUTATION, getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepSalutationWhenOrgAndNameBothSetToEmptyString() {
        Representative repWithAddressNoName = Representative.builder()
            .organisation("")
            .name(Name.builder().firstName("").lastName("").build())
            .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
            .build();

        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, repWithAddressNoName);
        assertEquals(REP_SALUTATION, getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepNameWhenRepHasOrgAndName() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_NAME_AND_ADDRESS);
        assertEquals(REP_ORG_WITH_NAME_AND_ADDRESS.getName().getFullNameNoTitle(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepNameWhenNameIsNotnull() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS);
        assertEquals(REP_WITH_ADDRESS.getName().getFullNameNoTitle(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepOrganisationWhenNameNoFirstName() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_ADDRESS);
        assertEquals(wrapper.getNewSscsCaseData().getAppeal().getRep().getOrganisation(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    public void getRepNameWhenNameHasFirstNameLastNameAndOrg() {
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_NAME_AND_ADDRESS);
        assertEquals(REP_ORG_WITH_NAME_AND_ADDRESS.getName().getFullNameNoTitle(), getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false));
    }

    @Test
    @Parameters({"CORRECTION_REFUSED", "CORRECTION_GRANTED"})
    public void validBundledLetterForCorrection(DwpState dwpState) {
        SscsCaseData caseData = buildBaseWrapper(APPELLANT_WITH_ADDRESS, ISSUE_FINAL_DECISION, READY_TO_LIST.getId()).getNewSscsCaseData();
        caseData.setDwpState(dwpState);
        String bundledLetterDocumentUrl = getBundledLetterDocumentUrl(ISSUE_FINAL_DECISION, caseData);

        assertNotNull(bundledLetterDocumentUrl);
    }

    @Test
    @Parameters(method = "bundledLetterTemplates")
    public void validBundledLetterType(NotificationEventType eventType) {
        assertNotNull(getBundledLetterDocumentUrl(eventType, buildBaseWrapper(APPELLANT_WITH_ADDRESS, eventType, READY_TO_LIST.getId()).getNewSscsCaseData()));
    }

    @Test
    @Parameters(method = "nonBundledLetterTemplates")
    public void invalidBundledLetterTileType(NotificationEventType eventType) {
        assertNull(getBundledLetterDocumentUrl(eventType, buildBaseWrapper(APPELLANT_WITH_ADDRESS, eventType, READY_TO_LIST.getId()).getNewSscsCaseData()));
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "ISSUE_FINAL_DECISION", "ISSUE_FINAL_DECISION_WELSH", "ISSUE_ADJOURNMENT_NOTICE", "DWP_UPLOAD_RESPONSE", "DWP_RESPONSE_RECEIVED"})
    public void sendLetterForNotificationType(NotificationEventType notificationEventType) {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT,
            null, null);
        when(pdfLetterService.generateLetter(any(), any(), any())).thenReturn("PDF".getBytes());
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, notificationEventType, VALID_APPEAL.getId()), DOCMOSIS_LETTER, appellantEmptySubscription, NotificationEventType.APPEAL_RECEIVED);
        verify(pdfLetterService).generateLetter(any(), any(), any());
        verify(pdfLetterService).buildCoversheet(any(), any());
        verifyNoMoreInteractions(pdfLetterService);
        verify(notificationHandler, atLeastOnce()).sendNotification(any(), any(), eq("Letter"), sender.capture());
        verifyNotificationIsSent(sender.getValue(), notificationEventType, CASE_ID);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "ISSUE_FINAL_DECISION", "ISSUE_ADJOURNMENT_NOTICE", "DWP_UPLOAD_RESPONSE", "DWP_RESPONSE_RECEIVED"})
    public void saveAppellantReasonableAdjustmentLetterForNotificationType(NotificationEventType notificationEventType) {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT,
            null, null);
        when(pdfLetterService.generateLetter(any(), any(), any())).thenReturn("PDF".getBytes());
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, notificationEventType, REASONABLE_ADJUSTMENTS), DOCMOSIS_LETTER, appellantEmptySubscription, NotificationEventType.APPEAL_RECEIVED);
        verify(pdfLetterService).generateLetter(any(), any(), any());
        verify(pdfLetterService).buildCoversheet(any(), any());
        verifyNoMoreInteractions(pdfLetterService);
        verify(notificationHandler, atLeastOnce()).sendNotification(any(), any(), eq("Letter"), sender.capture());
        verifyNotificationIsSaved(sender.getValue(), notificationEventType, CASE_ID, appellantEmptySubscription.getSubscriptionType());
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "ISSUE_FINAL_DECISION", "ISSUE_ADJOURNMENT_NOTICE", "DWP_UPLOAD_RESPONSE", "DWP_RESPONSE_RECEIVED"})
    public void saveRepReasonableAdjustmentLetterForNotificationType(NotificationEventType notificationEventType) {
        SubscriptionWithType repEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, REPRESENTATIVE,
            null, null);
        when(pdfLetterService.generateLetter(any(), any(), any())).thenReturn("PDF".getBytes());
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, notificationEventType, REASONABLE_ADJUSTMENTS), DOCMOSIS_LETTER, repEmptySubscription, NotificationEventType.APPEAL_RECEIVED);
        verify(pdfLetterService).generateLetter(any(), any(), any());
        verify(pdfLetterService).buildCoversheet(any(), any());
        verifyNoMoreInteractions(pdfLetterService);
        verify(notificationHandler, atLeastOnce()).sendNotification(any(), any(), eq("Letter"), sender.capture());
        verifyNotificationIsSaved(sender.getValue(), notificationEventType, CASE_ID, repEmptySubscription.getSubscriptionType());
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "ISSUE_FINAL_DECISION", "ISSUE_FINAL_DECISION_WELSH", "ISSUE_ADJOURNMENT_NOTICE", "DWP_UPLOAD_RESPONSE", "DWP_RESPONSE_RECEIVED"})
    public void saveAppointeeReasonableAdjustmentLetterForNotificationType(NotificationEventType notificationEventType) {
        SubscriptionWithType appointeeEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPOINTEE,
            null, null);
        when(pdfLetterService.generateLetter(any(), any(), any())).thenReturn("PDF".getBytes());
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS_AND_APPOINTEE, notificationEventType, REASONABLE_ADJUSTMENTS), DOCMOSIS_LETTER, appointeeEmptySubscription, NotificationEventType.APPEAL_RECEIVED);
        verify(pdfLetterService).generateLetter(any(), any(), any());
        verify(pdfLetterService).buildCoversheet(any(), any());
        verifyNoMoreInteractions(pdfLetterService);
        verify(notificationHandler, atLeastOnce()).sendNotification(any(), any(), eq("Letter"), sender.capture());
        verifyNotificationIsSaved(sender.getValue(), notificationEventType, CASE_ID, appointeeEmptySubscription.getSubscriptionType());
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "ISSUE_FINAL_DECISION", "ISSUE_FINAL_DECISION_WELSH", "ISSUE_ADJOURNMENT_NOTICE", "DWP_UPLOAD_RESPONSE", "DWP_RESPONSE_RECEIVED"})
    public void saveJointPartyReasonableAdjustmentLetterForNotificationType(NotificationEventType notificationEventType) {
        SubscriptionWithType jointPartyEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, JOINT_PARTY,
            null, null);
        when(pdfLetterService.generateLetter(any(), any(), any())).thenReturn("PDF".getBytes());
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, notificationEventType, REASONABLE_ADJUSTMENTS), DOCMOSIS_LETTER, jointPartyEmptySubscription, NotificationEventType.APPEAL_RECEIVED);
        verify(pdfLetterService).generateLetter(any(), any(), any());
        verify(pdfLetterService).buildCoversheet(any(), any());
        verifyNoMoreInteractions(pdfLetterService);
        verify(notificationHandler, atLeastOnce()).sendNotification(any(), any(), eq("Letter"), sender.capture());
        verifyNotificationIsSaved(sender.getValue(), notificationEventType, CASE_ID, jointPartyEmptySubscription.getSubscriptionType());
    }

    @Test
    public void givenNonDigitalCase_willNotSendAppealLodgedLetters() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT,
            null, null);
        classUnderTest.sendEmailSmsLetterNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.APPEAL_RECEIVED, State.VALID_APPEAL.getId()), LETTER, appellantEmptySubscription, NotificationEventType.APPEAL_RECEIVED);
        verifyNoInteractions(notificationHandler);
    }

    @Test
    public void getAddressPlaceholders_returnsExpectedValuesUKkAddress() {
        String fullNameNoTitle = "Jane Doe";
        Address testAddress = Address.builder()
                .line1("Somerset House")
                .line2("Strand")
                .town("London")
                .county("Greater London")
                .postcode("WC2R 1LA")
                .inMainlandUk(YesNo.YES)
                .build();

        Map<String, Object> placeholders = getAddressPlaceholders(testAddress, fullNameNoTitle);

        assertEquals("Jane Doe", placeholders.get(ADDRESS_LINE_1));
        assertEquals("Somerset House", placeholders.get(ADDRESS_LINE_2));
        assertEquals("Strand", placeholders.get(ADDRESS_LINE_3));
        assertEquals("London", placeholders.get(ADDRESS_LINE_4));
        assertEquals("Greater London", placeholders.get(ADDRESS_LINE_5));
        assertEquals("WC2R 1LA", placeholders.get(POSTCODE_LITERAL));
    }

    public void getAddressPlaceholders_returnsExpectedValuesInternationalAddressNoPostcode() {
        String fullNameNoTitle = "Jane Doe";
        Address testAddress = Address.builder()
                .line1("Catherdrale Notre-Dame de Paris")
                .line2("6 Parvis Notre-dame - Pl. Jean-Paul II")
                .town("Paris")
                .county("Ile-de_France")
                .country("France")
                .inMainlandUk(YesNo.NO)
                .build();

        Map<String, Object> placeholders = getAddressPlaceholders(testAddress, fullNameNoTitle);

        assertEquals("Jane Doe", placeholders.get(ADDRESS_LINE_1));
        assertEquals("Catherdrale Notre-Dame de Paris", placeholders.get(ADDRESS_LINE_2));
        assertEquals("6 Parvis Notre-dame - Pl. Jean-Paul II", placeholders.get(ADDRESS_LINE_3));
        assertEquals("Paris", placeholders.get(ADDRESS_LINE_4));
        assertEquals("Ile-de_France", placeholders.get(ADDRESS_LINE_5));
        assertEquals("France", placeholders.get(POSTCODE_LITERAL));
    }

    @Test
    public void getAddressPlaceholders_returnsExpectedKeys() {
        String fullNameNoTitle = "Jane Doe";
        Address testAddress = Address.builder()
                .line1("Catherdrale Notre-Dame de Paris")
                .line2("6 Parvis Notre-dame - Pl. Jean-Paul II")
                .town("Paris")
                .county("Ile-de_France")
                .postcode("75004")
                .country("France")
                .inMainlandUk(YesNo.NO)
                .build();

        List<String> addressConstants = List.of(ADDRESS_LINE_1, ADDRESS_LINE_2, ADDRESS_LINE_3, ADDRESS_LINE_4,
                ADDRESS_LINE_5, POSTCODE_LITERAL);

        Map<String, Object> actualPlaceholders = getAddressPlaceholders(testAddress, fullNameNoTitle);
        for (String addressConstant : addressConstants) {
            assertTrue(actualPlaceholders.containsKey(addressConstant) && actualPlaceholders.get(addressConstant) != null);
        }
    }



    private void verifyNotificationIsSaved(NotificationHandler.SendNotification sender, NotificationEventType eventType, String ccdCaseId, SubscriptionType subscriptionType) {
        try {
            sender.send();
            verify(notificationSender).saveLettersToReasonableAdjustment(any(), eq(eventType), any(), eq(ccdCaseId), eq(subscriptionType));
        } catch (NotificationClientException e) {
            fail("Not expected exception");
        }

    }

    private void verifyNotificationIsSent(NotificationHandler.SendNotification sender, NotificationEventType eventType, String ccdCaseId) {
        try {
            sender.send();
            verify(notificationSender).sendBundledLetter(any(), any(), eq(eventType), any(), eq(ccdCaseId));
        } catch (NotificationClientException e) {
            fail("Not expected exception");
        }
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant) {
        return buildBaseWrapper(appellant, STRUCK_OUT, null, Benefit.PIP, "Online", READY_TO_LIST.getId());
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant, NotificationEventType eventType, String createdInGapsFrom) {
        return buildBaseWrapper(appellant, eventType, null, Benefit.PIP, "Online", createdInGapsFrom);
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant, NotificationEventType eventType, Representative representative) {
        return buildBaseWrapper(appellant, eventType, representative, Benefit.PIP, "Online", READY_TO_LIST.getId());
    }

    private CcdNotificationWrapper buildBaseWrapper(final Appellant appellant, final NotificationEventType eventType, final Name name, final Address jointPartyAddress) {
        CcdNotificationWrapper wrapper = buildBaseWrapper(appellant, eventType, null, Benefit.PIP, "Online", READY_TO_LIST.getId());
        SscsCaseData sscsCaseData = wrapper.getNewSscsCaseData().toBuilder()
            .jointParty(JointParty.builder()
                .hasJointParty(YesNo.YES)
                .name(name)
                .jointPartyAddressSameAsAppellant(jointPartyAddress == null ? YesNo.YES : YesNo.NO)
                .address(jointPartyAddress)
                .build())
            .build();
        NotificationSscsCaseDataWrapper wraper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseData)
            .oldSscsCaseData(sscsCaseData)
            .notificationEventType(eventType)
            .build();
        return new CcdNotificationWrapper(wraper);
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant, NotificationEventType eventType, ReasonableAdjustments reasonableAdjustments) {
        CcdNotificationWrapper wrapper = buildBaseWrapper(appellant, eventType, REP_WITH_ADDRESS, Benefit.PIP, "Online", READY_TO_LIST.getId());
        SscsCaseData sscsCaseData = wrapper.getNewSscsCaseData().toBuilder()
            .reasonableAdjustments(reasonableAdjustments)
            .jointParty(JointParty.builder()
                .name(Name.builder()
                    .firstName("J")
                    .lastName("Party")
                    .build())
                .jointPartyAddressSameAsAppellant(YesNo.YES)
                .build())
            .build();
        NotificationSscsCaseDataWrapper wraper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseData)
            .oldSscsCaseData(sscsCaseData)
            .notificationEventType(eventType)
            .build();
        return new CcdNotificationWrapper(wraper);
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant, NotificationEventType eventType, Representative representative, Benefit benefit, String receivedVia, String createdInGapsFrom) {
        Subscription repSubscription = null;
        if (null != representative) {
            repSubscription = Subscription.builder().email("test@test.com").subscribeEmail(YES).mobile("07800000000").subscribeSms(YES).build();
        }

        Subscription appellantSubscription = null;
        if (null != appellant) {
            appellantSubscription = Subscription.builder().tya("GLSCRR").email("Email").mobile("07983495065").subscribeEmail(YES).subscribeSms(YES).build();
        }

        List<SscsDocument> documents = new ArrayList<>();

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.DIRECTION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.FINAL_DECISION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.DECISION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl2").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.FINAL_DECISION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl3").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.CORRECTION_GRANTED.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl7").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.CORRECTION_REFUSED.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl8").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.ADJOURNMENT_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl4").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl5").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.POSTPONEMENT_REQUEST_DIRECTION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl6").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.STATEMENT_OF_REASONS.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl6").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());


        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.STATEMENT_OF_REASONS_GRANTED.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl7").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.STATEMENT_OF_REASONS_REFUSED.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl8").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.SET_ASIDE_GRANTED.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl9").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.SET_ASIDE_REFUSED.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl20").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.LIBERTY_TO_APPLY_GRANTED.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl7").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.LIBERTY_TO_APPLY_REFUSED.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl8").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        documents.add(SscsDocument.builder().value(
                SscsDocumentDetails.builder().documentType(DocumentType.CORRECTED_DECISION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl9").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        List<SscsWelshDocument> welshDocuments = new ArrayList<>();

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.DIRECTION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.FINAL_DECISION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.DECISION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl2").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.FINAL_DECISION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl3").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.ADJOURNMENT_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl4").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl5").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.POSTPONEMENT_REQUEST_DIRECTION_NOTICE.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl6").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.CORRECTION_GRANTED.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl7").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        welshDocuments.add(SscsWelshDocument.builder().value(
                SscsWelshDocumentDetails.builder().documentType(DocumentType.CORRECTION_REFUSED.getValue())
                    .documentLink(DocumentLink.builder().documentUrl("testUrl8").build())
                    .documentDateAdded(LocalDate.now().minusDays(1).toString())
                    .build())
            .build());

        SscsCaseData sscsCaseDataWithDocuments = SscsCaseData.builder()
            .appeal(
                Appeal
                    .builder()
                    .benefitType(BenefitType.builder().code(benefit.name()).description(benefit.getDescription()).build())
                    .hearingType(AppealHearingType.ORAL.name())
                    .hearingOptions(HearingOptions.builder().wantsToAttend(YES).build())
                    .appellant(appellant)
                    .rep(representative)
                    .receivedVia(receivedVia)
                    .build())
            .subscriptions(
                Subscriptions.builder()
                    .appellantSubscription(appellantSubscription)
                    .representativeSubscription(repSubscription)
                    .build())
            .createdInGapsFrom(createdInGapsFrom)
            .sscsDocument(documents)
            .sscsWelshDocuments(welshDocuments)
            .caseReference(CASE_REFERENCE)
            .ccdCaseId(CASE_ID)
            .sscsInterlocDecisionDocument(SscsInterlocDecisionDocument.builder().documentLink(DocumentLink.builder().documentUrl("testUrl").build()).build())
            .sscsStrikeOutDocument(SscsStrikeOutDocument.builder().documentLink(DocumentLink.builder().documentUrl("testUrl").build()).build())
            .build();

        NotificationSscsCaseDataWrapper struckOutNotificationSscsCaseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseDataWithDocuments)
            .oldSscsCaseData(sscsCaseDataWithDocuments)
            .notificationEventType(eventType)
            .build();
        return new CcdNotificationWrapper(struckOutNotificationSscsCaseDataWrapper);
    }

    public Object[] bundledLetterTemplates() {
        return EVENT_TYPES_FOR_BUNDLED_LETTER.toArray();
    }

    public Object[] nonBundledLetterTemplates() {
        Object[] originalValues = Arrays.stream(NotificationEventType.values())
            .filter(type -> !EVENT_TYPES_FOR_BUNDLED_LETTER.contains(type))
            .toArray();

        ArrayList<Object> x = new ArrayList<Object>(Arrays.asList(originalValues));
        x.add(null);

        return x.toArray();
    }
}
