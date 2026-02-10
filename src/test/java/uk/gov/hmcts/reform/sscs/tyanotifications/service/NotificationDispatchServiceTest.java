package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.REP_SALUTATION;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.NotificationEventTypeLists.EVENT_TYPES_FOR_BUNDLED_LETTER;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.SubscriptionType.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.LetterUtils.getAddressToUseForLetter;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.LetterUtils.getRepSalutation;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.NotificationDispatchService.getBundledLetterDocumentUrl;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationProcessingServiceTest.verifyExpectedLogMessage;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationProcessingServiceTest.verifyNoErrorsLogged;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReasonableAdjustmentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReasonableAdjustments;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsInterlocDecisionDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsStrikeOutDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.NotificationDispatchService;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.NotificationExecutionManager;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.NotificationGateway;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.NotificationValidService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.Destination;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.docmosis.PdfLetterService;
import uk.gov.service.notify.NotificationClientException;

@ExtendWith(MockitoExtension.class)
public class NotificationDispatchServiceTest {
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
    private NotificationGateway notificationGateway;

    @Mock
    private PdfStoreService pdfStoreService;

    @Mock
    private NotificationExecutionManager notificationExecutionManager;

    @Mock
    private NotificationValidService notificationValidService;

    @Mock
    private PdfLetterService pdfLetterService;

    private NotificationDispatchService classUnderTest;

    @Mock
    private Appender<ILoggingEvent> mockAppender;
    @Captor
    private ArgumentCaptor captorLoggingEvent;


    @Captor
    private ArgumentCaptor<String> smsTemplateIdCaptor;

    @Captor
    private ArgumentCaptor<NotificationExecutionManager.SendAction> sender;

    @BeforeEach
    public void setup() {
        classUnderTest = new NotificationDispatchService(notificationGateway, notificationExecutionManager, notificationValidService, pdfLetterService, pdfStoreService);

        Logger logger = (Logger) LoggerFactory.getLogger(NotificationDispatchService.class.getName());
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
    public void dispatchLetterNotificationForAppellant() throws NotificationClientException {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.APPELLANT, null, null);
        classUnderTest.dispatchLetterNotificationToAddress(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId()), LETTER, APPELLANT_WITH_ADDRESS.getAddress(), appellantEmptySubscription);

        verify(notificationGateway).sendLetter(eq(LETTER.getLetterTemplate()), eq(APPELLANT_WITH_ADDRESS.getAddress()), any(), any(), any(), any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void dispatchLetterNotificationForRep() throws NotificationClientException {
        SubscriptionWithType representativeEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.REPRESENTATIVE, null, null);
        classUnderTest.dispatchLetterNotificationToAddress(buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_WITH_ADDRESS), LETTER, REP_WITH_ADDRESS.getAddress(), representativeEmptySubscription);

        verify(notificationGateway).sendLetter(eq(LETTER.getLetterTemplate()), eq(REP_WITH_ADDRESS.getAddress()), any(), any(), any(), any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void dispatchLetterNotificationForRepWithOrgName() throws NotificationClientException {
        SubscriptionWithType representativeEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.REPRESENTATIVE, null, null);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, NotificationEventType.CASE_UPDATED, REP_ORG_WITH_ADDRESS);
        classUnderTest.dispatchLetterNotificationToAddress(wrapper, LETTER, REP_WITH_ADDRESS.getAddress(), representativeEmptySubscription);

        verify(notificationGateway).sendLetter(eq(LETTER.getLetterTemplate()), eq(REP_WITH_ADDRESS.getAddress()), any(), any(), any(), any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void dispatchLetterNotificationForJointParty() throws NotificationClientException {
        SubscriptionWithType jointPartyEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, JOINT_PARTY,
            null, null);
        final CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, CASE_UPDATED, Name.builder().firstName("Jp").lastName("Party").build(), JOINT_PARTY_ADDRESS);
        classUnderTest.dispatchLetterNotificationToAddress(wrapper, LETTER, JOINT_PARTY_ADDRESS, jointPartyEmptySubscription);

        verify(notificationGateway).sendLetter(eq(LETTER.getLetterTemplate()), eq(JOINT_PARTY_ADDRESS), any(), any(), any(), any());
        verifyNoErrorsLogged(mockAppender, captorLoggingEvent);
    }

    @Test
    public void doNotDispatchLetterNotificationIfAddressEmpty() throws NotificationClientException {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.APPELLANT, null, null);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_EMPTY_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.dispatchLetterNotification(wrapper, LETTER, appellantEmptySubscription, NotificationEventType.CASE_UPDATED);

        verifyNoInteractions(notificationGateway);
        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Failed to send letter for event id", Level.ERROR);
    }

    @Test
    public void doNotDispatchLetterNotificationIfNoAddress() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.APPELLANT, null, null);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_NO_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.dispatchLetterNotification(wrapper, LETTER, appellantEmptySubscription, NotificationEventType.CASE_UPDATED);

        verifyNoInteractions(notificationGateway);
        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, wrapper.getNewSscsCaseData().getCcdCaseId(), "Failed to send letter for event id", Level.ERROR);
    }

    @Test
    public void logErrorMessageWhenNoNotificationSent() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.APPELLANT, null, null);
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_NO_ADDRESS, NotificationEventType.CASE_UPDATED, READY_TO_LIST.getId());
        classUnderTest.dispatchNotification(
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

    @ParameterizedTest
    @EnumSource(value = DwpState.class, names = { "CORRECTION_REFUSED", "CORRECTION_GRANTED" })
    public void validBundledLetterForCorrection(DwpState dwpState) {
        SscsCaseData caseData = buildBaseWrapper(APPELLANT_WITH_ADDRESS, ISSUE_FINAL_DECISION, READY_TO_LIST.getId()).getNewSscsCaseData();
        caseData.setDwpState(dwpState);
        String bundledLetterDocumentUrl = getBundledLetterDocumentUrl(ISSUE_FINAL_DECISION, caseData);

        assertNotNull(bundledLetterDocumentUrl);
    }

    @ParameterizedTest
    @MethodSource(value = "bundledLetterTemplates")
    public void validBundledLetterType(NotificationEventType eventType) {
        assertNotNull(getBundledLetterDocumentUrl(eventType, buildBaseWrapper(APPELLANT_WITH_ADDRESS, eventType, READY_TO_LIST.getId()).getNewSscsCaseData()));
    }

    @ParameterizedTest
    @MethodSource(value = "nonBundledLetterTemplates")
    public void invalidBundledLetterTileType(NotificationEventType eventType) {
        assertNull(getBundledLetterDocumentUrl(eventType, buildBaseWrapper(APPELLANT_WITH_ADDRESS, eventType, READY_TO_LIST.getId()).getNewSscsCaseData()));
    }

    @ParameterizedTest
    @EnumSource(value = NotificationEventType.class, names = { "APPEAL_RECEIVED", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "ISSUE_FINAL_DECISION", "ISSUE_FINAL_DECISION_WELSH", "ISSUE_ADJOURNMENT_NOTICE", "DWP_UPLOAD_RESPONSE", "DWP_RESPONSE_RECEIVED" })
    public void sendLetterForNotificationType(NotificationEventType notificationEventType) {
        SubscriptionWithType appellantEmptySub = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT, null, null);
        when(pdfLetterService.generateLetter(any(), any(), any())).thenReturn("PDF".getBytes());
        var wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS, notificationEventType, VALID_APPEAL.getId());

        classUnderTest.dispatchNotification(wrapper, DOCMOSIS_LETTER, appellantEmptySub, APPEAL_RECEIVED);

        verify(pdfLetterService).generateLetter(any(), any(), any());
        verify(pdfLetterService).buildCoversheet(any(), any());
        verifyNoMoreInteractions(pdfLetterService);
        verify(notificationExecutionManager, atLeastOnce()).executeNotification(any(), any(), eq("Letter"), sender.capture());
        verifyNotificationIsSent(sender.getValue(), wrapper);
    }

    @ParameterizedTest
    @EnumSource(value = NotificationEventType.class, names = {"APPEAL_RECEIVED", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "ISSUE_FINAL_DECISION", "ISSUE_ADJOURNMENT_NOTICE", "DWP_UPLOAD_RESPONSE", "DWP_RESPONSE_RECEIVED"})
    public void saveAppellantReasonableAdjustmentLetterForNotificationType(NotificationEventType notificationEventType) {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT,
            null, null);
        when(pdfLetterService.generateLetter(any(), any(), any())).thenReturn("PDF".getBytes());
        classUnderTest.dispatchNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, notificationEventType, REASONABLE_ADJUSTMENTS), DOCMOSIS_LETTER, appellantEmptySubscription, APPEAL_RECEIVED);
        verify(pdfLetterService).generateLetter(any(), any(), any());
        verify(pdfLetterService).buildCoversheet(any(), any());
        verifyNoMoreInteractions(pdfLetterService);
        verify(notificationExecutionManager, atLeastOnce()).executeNotification(any(), any(), eq("Letter"), sender.capture());
        verifyNotificationIsSaved(sender.getValue(), notificationEventType, appellantEmptySubscription.getSubscriptionType());
    }

    @ParameterizedTest
    @EnumSource(value = NotificationEventType.class, names = {"APPEAL_RECEIVED", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "ISSUE_FINAL_DECISION", "ISSUE_ADJOURNMENT_NOTICE", "DWP_UPLOAD_RESPONSE", "DWP_RESPONSE_RECEIVED"})
    public void saveRepReasonableAdjustmentLetterForNotificationType(NotificationEventType notificationEventType) {
        SubscriptionWithType repEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, REPRESENTATIVE,
            null, null);
        when(pdfLetterService.generateLetter(any(), any(), any())).thenReturn("PDF".getBytes());
        classUnderTest.dispatchNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, notificationEventType, REASONABLE_ADJUSTMENTS), DOCMOSIS_LETTER, repEmptySubscription, APPEAL_RECEIVED);
        verify(pdfLetterService).generateLetter(any(), any(), any());
        verify(pdfLetterService).buildCoversheet(any(), any());
        verifyNoMoreInteractions(pdfLetterService);
        verify(notificationExecutionManager, atLeastOnce()).executeNotification(any(), any(), eq("Letter"), sender.capture());
        verifyNotificationIsSaved(sender.getValue(), notificationEventType, repEmptySubscription.getSubscriptionType());
    }

    @ParameterizedTest
    @EnumSource(value = NotificationEventType.class, names = {"APPEAL_RECEIVED", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "ISSUE_FINAL_DECISION", "ISSUE_FINAL_DECISION_WELSH", "ISSUE_ADJOURNMENT_NOTICE", "DWP_UPLOAD_RESPONSE", "DWP_RESPONSE_RECEIVED"})
    public void saveAppointeeReasonableAdjustmentLetterForNotificationType(NotificationEventType notificationEventType) {
        SubscriptionWithType appointeeEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPOINTEE,
            null, null);
        when(pdfLetterService.generateLetter(any(), any(), any())).thenReturn("PDF".getBytes());
        classUnderTest.dispatchNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS_AND_APPOINTEE, notificationEventType, REASONABLE_ADJUSTMENTS), DOCMOSIS_LETTER, appointeeEmptySubscription, APPEAL_RECEIVED);
        verify(pdfLetterService).generateLetter(any(), any(), any());
        verify(pdfLetterService).buildCoversheet(any(), any());
        verifyNoMoreInteractions(pdfLetterService);
        verify(notificationExecutionManager, atLeastOnce()).executeNotification(any(), any(), eq("Letter"), sender.capture());
        verifyNotificationIsSaved(sender.getValue(), notificationEventType, appointeeEmptySubscription.getSubscriptionType());
    }

    @ParameterizedTest
    @EnumSource(value = NotificationEventType.class, names = {"APPEAL_RECEIVED", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "ISSUE_FINAL_DECISION", "ISSUE_FINAL_DECISION_WELSH", "ISSUE_ADJOURNMENT_NOTICE", "DWP_UPLOAD_RESPONSE", "DWP_RESPONSE_RECEIVED"})
    public void saveJointPartyReasonableAdjustmentLetterForNotificationType(NotificationEventType notificationEventType) {
        SubscriptionWithType jointPartyEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, JOINT_PARTY,
            null, null);
        when(pdfLetterService.generateLetter(any(), any(), any())).thenReturn("PDF".getBytes());
        classUnderTest.dispatchNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, notificationEventType, REASONABLE_ADJUSTMENTS), DOCMOSIS_LETTER, jointPartyEmptySubscription, APPEAL_RECEIVED);
        verify(pdfLetterService).generateLetter(any(), any(), any());
        verify(pdfLetterService).buildCoversheet(any(), any());
        verifyNoMoreInteractions(pdfLetterService);
        verify(notificationExecutionManager, atLeastOnce()).executeNotification(any(), any(), eq("Letter"), sender.capture());
        verifyNotificationIsSaved(sender.getValue(), notificationEventType, jointPartyEmptySubscription.getSubscriptionType());
    }

    @Test
    public void givenNonDigitalCase_willNotSendAppealLodgedLetters() {
        SubscriptionWithType appellantEmptySubscription = new SubscriptionWithType(EMPTY_SUBSCRIPTION, APPELLANT,
            null, null);
        classUnderTest.dispatchNotification(buildBaseWrapper(APPELLANT_WITH_ADDRESS, APPEAL_RECEIVED, State.VALID_APPEAL.getId()), LETTER, appellantEmptySubscription, APPEAL_RECEIVED);
        verifyNoInteractions(notificationExecutionManager);
    }


    private void verifyNotificationIsSaved(NotificationExecutionManager.SendAction sender, NotificationEventType eventType, SubscriptionType subscriptionType) {
        try {
            sender.send();
            verify(notificationGateway).saveLettersToReasonableAdjustment(any(), eq(eventType), any(), eq(CASE_ID), eq(subscriptionType));
        } catch (NotificationClientException e) {
            fail("Not expected exception");
        }

    }

    private void verifyNotificationIsSent(NotificationExecutionManager.SendAction sender, NotificationWrapper wrapper) {
        try {
            sender.send();
            verify(notificationGateway).sendBundledLetter(eq(wrapper), any(), any());
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

    public static Object[] bundledLetterTemplates() {
        return EVENT_TYPES_FOR_BUNDLED_LETTER.toArray();
    }

    public static Object[] nonBundledLetterTemplates() {
        Object[] originalValues = Arrays.stream(NotificationEventType.values())
            .filter(type -> !EVENT_TYPES_FOR_BUNDLED_LETTER.contains(type))
            .toArray();

        ArrayList<Object> x = new ArrayList<Object>(Arrays.asList(originalValues));
        x.add(null);

        return x.toArray();
    }
}
