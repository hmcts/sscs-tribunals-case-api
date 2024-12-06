package uk.gov.hmcts.reform.sscs.tyanotifications.personalisation;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getLongBenefitNameDescriptionWithOptionalAcronym;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.tyanotifications.SscsCaseDataUtils.getWelshDate;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationConfiguration.PersonalisationKey;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationConfiguration;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.properties.EvidenceProperties;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.MessageAuthenticationServiceImpl;

@Slf4j
@RunWith(JUnitParamsRunner.class)
public class PersonalisationTest {

    private static final String CASE_ID = "54321";
    private static final String ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String ADDRESS2 = "Social Security & Child Support Appeals";
    private static final String ADDRESS3 = "Prudential Buildings";
    private static final String ADDRESS4 = "36 Dale Street";
    private static final String CITY = "LIVERPOOL";
    private static final String POSTCODE = "L2 5UZ";
    private static final String PHONE = "0300 999 8888";
    private static final String PHONE_WELSH = "0300 999 9999";
    private static final String DATE = "2018-07-01T14:01:18.243";

    @Mock
    private NotificationConfig config;

    @Mock
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private NotificationDateConverterUtil notificationDateConverterUtil;

    @Mock
    private EvidenceProperties evidenceProperties;

    @InjectMocks
    public Personalisation personalisation;

    @Spy
    private PersonalisationConfiguration personalisationConfiguration;

    protected Subscriptions subscriptions;

    protected Name name;

    private RegionalProcessingCenter rpc;
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private String evidenceAddressLine1 = "line1";
    private String evidenceAddressLine2 = "line2";
    private String evidenceAddressLine3 = "line3";
    private String evidenceAddressScottishLine3 = "scottishLine3";
    private String evidenceAddressTown = "town";
    private String evidenceAddressCounty = "county";
    private String evidenceAddressPostcode = "postcode";
    private String evidenceAddressScottishPostcode = "scottishPostcode";
    private String evidenceAddressTelephone = "telephone";
    private String evidenceAddressTelephoneWelsh = PHONE_WELSH;
    private EvidenceProperties.EvidenceAddress evidenceAddress = new EvidenceProperties.EvidenceAddress();

    @Before
    public void setup() {
        openMocks(this);
        when(config.getTrackAppealLink()).thenReturn(Link.builder().linkUrl("http://tyalink.com/appeal_id").build());
        when(config.getMyaLink()).thenReturn(Link.builder().linkUrl("http://myalink.com/appeal_id").build());
        when(config.getMyaClaimingExpensesLink()).thenReturn(Link.builder().linkUrl("http://myalink.com/claimingExpenses").build());
        when(config.getMyaEvidenceSubmissionInfoLink()).thenReturn(Link.builder().linkUrl("http://myalink.com/evidenceSubmission").build());
        when(config.getMyaHearingInfoLink()).thenReturn(Link.builder().linkUrl("http://myalink.com/hearingInfo").build());
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/appeal_id").build());
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://link.com/manage-email-notifications/mac").build());
        when(config.getClaimingExpensesLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/expenses").build());
        when(config.getHearingInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/abouthearing").build());
        when(config.getOnlineHearingLinkWithEmail()).thenReturn(Link.builder().linkUrl("http://link.com/onlineHearing?email={email}").build());
        when(config.getOnlineHearingLink()).thenReturn("http://link.com");
        when(config.getHelplineTelephone()).thenReturn("0300 123 1142");
        when(config.getHelplineTelephoneScotland()).thenReturn("0300 790 6234");
        when(notificationDateConverterUtil.toEmailDate(LocalDate.now().plusDays(1))).thenReturn("1 January 2018");
        when(notificationDateConverterUtil.toEmailDate(LocalDate.now().plusDays(7))).thenReturn("1 February 2018");
        when(notificationDateConverterUtil.toEmailDate(LocalDate.now().plusDays(56))).thenReturn("1 February 2019");
        when(macService.generateToken(eq("GLSCRR"), any())).thenReturn("ZYX");
        when(hearingContactDateExtractor.extract(any())).thenReturn(Optional.empty());

        rpc = RegionalProcessingCenter.builder()
            .name("LIVERPOOL").address1(ADDRESS1).address2(ADDRESS2).address3(ADDRESS3).address4(ADDRESS4).city(CITY).postcode(POSTCODE).phoneNumber(PHONE).build();

        when(regionalProcessingCenterService.getByScReferenceCode("SC/1234/5")).thenReturn(rpc);

        Subscription subscription = Subscription.builder()
            .tya("GLSCRR")
            .email("test@email.com")
            .mobile("07983495065")
            .subscribeEmail("Yes")
            .subscribeSms("No")
            .build();

        subscriptions = Subscriptions.builder().appellantSubscription(subscription).jointPartySubscription(subscription).build();
        name = Name.builder().firstName("Harry").lastName("Kane").title("Mr").build();

        evidenceAddress.setLine1(evidenceAddressLine1);
        evidenceAddress.setLine2(evidenceAddressLine2);
        evidenceAddress.setLine3(evidenceAddressLine3);
        evidenceAddress.setScottishLine3(evidenceAddressScottishLine3);
        evidenceAddress.setTown(evidenceAddressTown);
        evidenceAddress.setCounty(evidenceAddressCounty);
        evidenceAddress.setPostcode(evidenceAddressPostcode);
        evidenceAddress.setScottishPostcode(evidenceAddressScottishPostcode);
        evidenceAddress.setTelephone(evidenceAddressTelephone);
        evidenceAddress.setTelephoneWelsh(evidenceAddressTelephoneWelsh);
        when(evidenceProperties.getAddress()).thenReturn(evidenceAddress);

        Map<String, String> englishMap = new HashMap<>();
        englishMap.put(PersonalisationKey.LANGUAGE_INTERPRETER.name(), "Language interpreter: ");
        englishMap.put(PersonalisationKey.SIGN_INTERPRETER.name(), "Sign interpreter: ");
        englishMap.put(PersonalisationKey.HEARING_LOOP.name(), "Hearing loop: ");
        englishMap.put(PersonalisationKey.DISABLED_ACCESS.name(), "Disabled access: ");
        englishMap.put(PersonalisationKey.OTHER_ARRANGEMENTS.name(), "Any other arrangements: ");
        englishMap.put(PersonalisationKey.REQUIRED.name(), "Required");
        englishMap.put(PersonalisationKey.NOT_REQUIRED.name(), "Not required");

        Map<String, String> welshMap = new HashMap<>();
        welshMap.put(PersonalisationKey.LANGUAGE_INTERPRETER.name(), "Dehonglydd iaith arwyddion: ");
        welshMap.put(PersonalisationKey.SIGN_INTERPRETER.name(), "Dehonglydd iaith arwyddion: ");
        welshMap.put(PersonalisationKey.HEARING_LOOP.name(), "Dolen glyw: ");
        welshMap.put(PersonalisationKey.DISABLED_ACCESS.name(), "Mynediad i bobl anab: ");
        welshMap.put(PersonalisationKey.OTHER_ARRANGEMENTS.name(), "Unrhyw drefniadau eraill: ");
        welshMap.put(PersonalisationKey.REQUIRED.name(), "Gofynnol");
        welshMap.put(PersonalisationKey.NOT_REQUIRED.name(), "Dim yn ofynnol");

        Map<LanguagePreference, Map<String, String>> personalisations = new HashMap<>();
        personalisations.put(LanguagePreference.ENGLISH, englishMap);
        personalisations.put(LanguagePreference.WELSH, welshMap);
        personalisationConfiguration.setPersonalisation(personalisations);
    }


    @Test
    @Parameters({"APPEAL_TO_PROCEED, directionIssued.appealToProceed, APPELLANT",
        "APPEAL_TO_PROCEED, directionIssued.appealToProceed, JOINT_PARTY",
        "PROVIDE_INFORMATION, directionIssued.provideInformation, REPRESENTATIVE",
        "GRANT_EXTENSION, directionIssued.grantExtension, APPOINTEE",
        "REFUSE_EXTENSION, directionIssued.refuseExtension, APPELLANT",
        "GRANT_REINSTATEMENT, directionIssued.grantReinstatement, APPELLANT",
        "REFUSE_REINSTATEMENT, directionIssued.refuseReinstatement, APPOINTEE",
        "REFUSE_HEARING_RECORDING_REQUEST, directionIssued.refuseHearingRecordingRequest, APPOINTEE"
    })
    public void whenDirectionIssuedAndDirectionTypeShouldGenerateCorrectTemplate(DirectionType directionType,
                                                                                 String templateConfig,
                                                                                 SubscriptionType subscriptionType) {

        NotificationWrapper notificationWrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(SscsCaseData.builder()
                .directionTypeDl(new DynamicList(directionType.toString()))
                .appeal(Appeal.builder()
                    .hearingType(ONLINE.getValue())
                    .build())
                .build())
            .notificationEventType(DIRECTION_ISSUED)
            .build());

        personalisation.getTemplate(notificationWrapper, PIP, subscriptionType);

        verify(config).getTemplate(eq(DIRECTION_ISSUED.getId()),
            eq(DIRECTION_ISSUED.getId()),
            eq(DIRECTION_ISSUED.getId()),
            eq(templateConfig + "." + lowerCase(subscriptionType.toString())),
            any(Benefit.class), any(NotificationWrapper.class), eq(null)
        );
    }

    @Test
    @Parameters({"APPELLANT, grantUrgentHearing, directionIssued.grantUrgentHearing",
        "JOINT_PARTY, grantUrgentHearing, directionIssued.grantUrgentHearing",
        "REPRESENTATIVE, grantUrgentHearing, directionIssued.grantUrgentHearing",
        "APPOINTEE, grantUrgentHearing, directionIssued.grantUrgentHearing",
        "APPELLANT, refuseUrgentHearing, directionIssued.refuseUrgentHearing",
        "JOINT_PARTY, refuseUrgentHearing, directionIssued.refuseUrgentHearing",
        "REPRESENTATIVE, refuseUrgentHearing, directionIssued.refuseUrgentHearing",
        "APPOINTEE, refuseUrgentHearing, directionIssued.refuseUrgentHearing"})
    public void whenDirectionIssuedAndGrantOrRefuseUrgentHearingShouldGenerateCorrectTemplate(SubscriptionType subscriptionType, String directionTypeString, String templateConfig) {

        NotificationWrapper notificationWrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(SscsCaseData.builder()
                .directionTypeDl(new DynamicList("grantUrgentHearing"))
                .appeal(Appeal.builder()
                    .hearingType(ONLINE.getValue())
                    .build())
                .build())
            .notificationEventType(DIRECTION_ISSUED)
            .build());

        personalisation.getTemplate(notificationWrapper, PIP, subscriptionType);

        verify(config).getTemplate(eq(DIRECTION_ISSUED.getId()),
            eq(DIRECTION_ISSUED.getId()),
            eq(DIRECTION_ISSUED.getId()),
            eq("directionIssued.grantUrgentHearing" + "." + lowerCase(subscriptionType.toString())),
            any(Benefit.class), any(NotificationWrapper.class), eq(null)
        );
    }


    @Test
    @Parameters({"APPEAL_TO_PROCEED, directionIssuedWelsh.appealToProceed, APPELLANT",
        "APPEAL_TO_PROCEED, directionIssuedWelsh.appealToProceed, JOINT_PARTY",
        "PROVIDE_INFORMATION, directionIssuedWelsh.provideInformation, REPRESENTATIVE",
        "GRANT_EXTENSION, directionIssuedWelsh.grantExtension, APPOINTEE",
        "REFUSE_EXTENSION, directionIssuedWelsh.refuseExtension, APPELLANT",
        "GRANT_REINSTATEMENT, directionIssuedWelsh.grantReinstatement, APPELLANT",
        "REFUSE_REINSTATEMENT, directionIssuedWelsh.refuseReinstatement, APPOINTEE",
        "REFUSE_HEARING_RECORDING_REQUEST, directionIssuedWelsh.refuseHearingRecordingRequest, APPOINTEE"
    })
    public void whenDirectionIssuedWelshAndDirectionTypeShouldGenerateCorrectTemplate(DirectionType directionType,
                                                                                      String templateConfig,
                                                                                      SubscriptionType subscriptionType) {

        NotificationWrapper notificationWrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(SscsCaseData.builder()
                .directionTypeDl(new DynamicList(directionType.toString()))
                .languagePreferenceWelsh("Yes")
                .appeal(Appeal.builder()
                    .hearingType(ONLINE.getValue())
                    .build())
                .build())
            .notificationEventType(DIRECTION_ISSUED_WELSH)
            .build());

        personalisation.getTemplate(notificationWrapper, PIP, subscriptionType);

        verify(config).getTemplate(eq(DIRECTION_ISSUED_WELSH.getId()),
            eq(DIRECTION_ISSUED_WELSH.getId()),
            eq(DIRECTION_ISSUED_WELSH.getId()),
            eq(templateConfig + "." + lowerCase(subscriptionType.toString())),
            any(Benefit.class), any(NotificationWrapper.class), eq(null)
        );
    }

    @Test
    @Parameters(method = "generateNotificationTypeAndSubscriptionsScenarios")
    public void givenSubscriptionType_shouldGenerateEmailAndSmsAndLetterTemplateNamesPerSubscription(
        NotificationEventType notificationEventType, SubscriptionType subscriptionType, HearingType hearingType,
        boolean hasEmailTemplate, boolean hasSmsTemplate, boolean hasLetterTemplate, boolean hasDocmosisTemplate) {
        NotificationWrapper notificationWrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(SscsCaseData.builder()
                .directionTypeDl(new DynamicList(DirectionType.PROVIDE_INFORMATION.toString()))
                .appeal(Appeal.builder()
                    .hearingType(hearingType.name())
                    .build())
                .build())
            .notificationEventType(notificationEventType)
            .build());

        //noinspection unchecked
        personalisation.getTemplate(notificationWrapper, PIP, subscriptionType);

        verify(config).getTemplate(eq(hasEmailTemplate ? getExpectedTemplateName(notificationEventType, subscriptionType) : notificationEventType.getId()),
            eq(hasSmsTemplate ? getExpectedTemplateName(notificationEventType, subscriptionType) : notificationEventType.getId()),
            eq(hasLetterTemplate ? getExpectedTemplateName(notificationEventType, subscriptionType) : notificationEventType.getId()),
            eq(hasDocmosisTemplate ? getExpectedTemplateName(notificationEventType, subscriptionType) : notificationEventType.getId()),
            any(Benefit.class), any(NotificationWrapper.class), eq(null)
        );
    }

    private String getExpectedTemplateName(NotificationEventType notificationEventType,
                                           SubscriptionType subscriptionType) {
        return notificationEventType.getId() + (subscriptionType == null ? "" :
            "." + lowerCase(subscriptionType.name()));
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateNotificationTypeAndSubscriptionsScenarios() {
        return new Object[]{
            new Object[]{ACTION_POSTPONEMENT_REQUEST, APPELLANT, REGULAR, false, false, false, true},
            new Object[]{ACTION_POSTPONEMENT_REQUEST, APPOINTEE, REGULAR, false, false, false, true},
            new Object[]{ACTION_POSTPONEMENT_REQUEST, JOINT_PARTY, REGULAR, false, false, false, true},
            new Object[]{ACTION_POSTPONEMENT_REQUEST, REPRESENTATIVE, REGULAR, false, false, false, true},
            new Object[]{ACTION_POSTPONEMENT_REQUEST_WELSH, APPELLANT, REGULAR, false, false, false, true},
            new Object[]{ACTION_POSTPONEMENT_REQUEST_WELSH, APPOINTEE, REGULAR, false, false, false, true},
            new Object[]{ACTION_POSTPONEMENT_REQUEST_WELSH, JOINT_PARTY, REGULAR, false, false, false, true},
            new Object[]{ACTION_POSTPONEMENT_REQUEST_WELSH, REPRESENTATIVE, REGULAR, false, false, false, true},
            new Object[]{ADJOURNED, APPELLANT, ONLINE, true, true, false, false},
            new Object[]{ADJOURNED, APPELLANT, PAPER, true, true, false, false},
            new Object[]{ADJOURNED, APPELLANT, REGULAR, true, true, false, false},
            new Object[]{ADJOURNED, REPRESENTATIVE, ONLINE, true, true, false, false},
            new Object[]{ADJOURNED, REPRESENTATIVE, PAPER, true, true, false, false},
            new Object[]{ADJOURNED, REPRESENTATIVE, REGULAR, true, true, false, false},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, APPELLANT, ONLINE, true, true, false, true},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, APPELLANT, PAPER, true, true, false, true},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, APPELLANT, REGULAR, true, true, false, true},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, REPRESENTATIVE, ONLINE, true, true, false, true},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, REPRESENTATIVE, PAPER, true, true, false, true},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, REPRESENTATIVE, REGULAR, true, true, false, true},
            new Object[]{APPEAL_DORMANT, APPELLANT, ORAL, true, true, false, false},
            new Object[]{APPEAL_DORMANT, APPELLANT, PAPER, true, true, false, false},
            new Object[]{APPEAL_DORMANT, REPRESENTATIVE, ORAL, true, true, false, false},
            new Object[]{APPEAL_DORMANT, REPRESENTATIVE, PAPER, true, true, false, false},
            new Object[]{APPEAL_LAPSED, APPELLANT, ONLINE, true, true, false, true},
            new Object[]{APPEAL_LAPSED, APPELLANT, PAPER, true, true, false, true},
            new Object[]{APPEAL_LAPSED, APPELLANT, REGULAR, true, true, false, true},
            new Object[]{APPEAL_LAPSED, APPOINTEE, ONLINE, true, true, false, true},
            new Object[]{APPEAL_LAPSED, APPOINTEE, PAPER, true, true, false, true},
            new Object[]{APPEAL_LAPSED, APPOINTEE, REGULAR, true, true, false, true},
            new Object[]{APPEAL_RECEIVED, APPELLANT, ONLINE, true, true, true, true},
            new Object[]{APPEAL_RECEIVED, APPELLANT, PAPER, true, true, true, true},
            new Object[]{APPEAL_RECEIVED, APPELLANT, REGULAR, true, true, true, true},
            new Object[]{APPEAL_RECEIVED, APPOINTEE, ONLINE, true, true, true, true},
            new Object[]{APPEAL_RECEIVED, APPOINTEE, PAPER, true, true, true, true},
            new Object[]{APPEAL_RECEIVED, APPOINTEE, REGULAR, true, true, true, true},
            new Object[]{APPEAL_RECEIVED, REPRESENTATIVE, ONLINE, true, true, true, true},
            new Object[]{APPEAL_RECEIVED, REPRESENTATIVE, PAPER, true, true, true, true},
            new Object[]{APPEAL_RECEIVED, REPRESENTATIVE, REGULAR, true, true, true, true},
            new Object[]{APPEAL_WITHDRAWN, APPELLANT, ONLINE, true, true, false, true},
            new Object[]{APPEAL_WITHDRAWN, APPELLANT, PAPER, true, true, false, true},
            new Object[]{APPEAL_WITHDRAWN, APPELLANT, REGULAR, true, true, false, true},
            new Object[]{APPEAL_WITHDRAWN, REPRESENTATIVE, ONLINE, true, true, false, true},
            new Object[]{APPEAL_WITHDRAWN, REPRESENTATIVE, PAPER, true, true, false, true},
            new Object[]{APPEAL_WITHDRAWN, REPRESENTATIVE, REGULAR, true, true, false, true},
            new Object[]{DEATH_OF_APPELLANT, APPOINTEE, REGULAR, false, false, false, true},
            new Object[]{DEATH_OF_APPELLANT, REPRESENTATIVE, REGULAR, false, false, false, true},
            new Object[]{DECISION_ISSUED, APPELLANT, ONLINE, false, false, false, true},
            new Object[]{DECISION_ISSUED, APPOINTEE, ONLINE, false, false, false, true},
            new Object[]{DECISION_ISSUED, REPRESENTATIVE, ONLINE, false, false, false, true},
            new Object[]{DECISION_ISSUED_WELSH, APPELLANT, ONLINE, false, false, false, true},
            new Object[]{DECISION_ISSUED_WELSH, APPOINTEE, ONLINE, false, false, false, true},
            new Object[]{DECISION_ISSUED_WELSH, REPRESENTATIVE, ONLINE, false, false, false, true},
            new Object[]{DWP_RESPONSE_RECEIVED, APPELLANT, ONLINE, true, true, true, false},
            new Object[]{DWP_RESPONSE_RECEIVED, APPELLANT, PAPER, true, true, true, false},
            new Object[]{DWP_RESPONSE_RECEIVED, APPOINTEE, ONLINE, true, true, true, false},
            new Object[]{DWP_RESPONSE_RECEIVED, APPOINTEE, PAPER, true, true, true, false},
            new Object[]{DWP_RESPONSE_RECEIVED, REPRESENTATIVE, ONLINE, true, true, true, false},
            new Object[]{DWP_RESPONSE_RECEIVED, REPRESENTATIVE, PAPER, true, true, true, false},
            new Object[]{DWP_UPLOAD_RESPONSE, APPELLANT, ONLINE, true, true, true, false},
            new Object[]{DWP_UPLOAD_RESPONSE, APPELLANT, PAPER, true, true, true, false},
            new Object[]{DWP_UPLOAD_RESPONSE, APPOINTEE, ONLINE, true, true, true, false},
            new Object[]{DWP_UPLOAD_RESPONSE, APPOINTEE, PAPER, true, true, true, false},
            new Object[]{DWP_UPLOAD_RESPONSE, REPRESENTATIVE, ONLINE, true, true, true, false},
            new Object[]{DWP_UPLOAD_RESPONSE, REPRESENTATIVE, PAPER, true, true, true, false},
            new Object[]{EVIDENCE_RECEIVED, APPELLANT, ONLINE, true, true, true, false},
            new Object[]{EVIDENCE_RECEIVED, APPELLANT, PAPER, true, true, true, false},
            new Object[]{EVIDENCE_RECEIVED, APPELLANT, REGULAR, true, true, true, false},
            new Object[]{EVIDENCE_RECEIVED, REPRESENTATIVE, ONLINE, true, true, true, false},
            new Object[]{EVIDENCE_RECEIVED, REPRESENTATIVE, PAPER, true, true, true, false},
            new Object[]{EVIDENCE_RECEIVED, REPRESENTATIVE, REGULAR, true, true, true, false},
            new Object[]{HEARING_BOOKED, APPELLANT, ONLINE, true, true, false, true},
            new Object[]{HEARING_BOOKED, APPELLANT, PAPER, true, true, false, true},
            new Object[]{HEARING_BOOKED, APPELLANT, REGULAR, true, true, false, true},
            new Object[]{HEARING_BOOKED, REPRESENTATIVE, ONLINE, true, true, false, true},
            new Object[]{HEARING_BOOKED, REPRESENTATIVE, PAPER, true, true, false, true},
            new Object[]{HEARING_BOOKED, REPRESENTATIVE, REGULAR, true, true, false, true},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE, APPELLANT, ONLINE, false, false, false, true},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE, APPOINTEE, ONLINE, false, false, false, true},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE, REPRESENTATIVE, ONLINE, false, false, false, true},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE_WELSH, APPELLANT, ONLINE, false, false, false, true},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE_WELSH, APPOINTEE, ONLINE, false, false, false, true},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE_WELSH, REPRESENTATIVE, ONLINE, false, false, false, true},
            new Object[]{ISSUE_FINAL_DECISION, APPELLANT, ONLINE, false, false, false, true},
            new Object[]{ISSUE_FINAL_DECISION, APPOINTEE, ONLINE, false, false, false, true},
            new Object[]{ISSUE_FINAL_DECISION, REPRESENTATIVE, ONLINE, false, false, false, true},
            new Object[]{ISSUE_FINAL_DECISION_WELSH, APPELLANT, ONLINE, false, false, false, true},
            new Object[]{ISSUE_FINAL_DECISION_WELSH, APPOINTEE, ONLINE, false, false, false, true},
            new Object[]{ISSUE_FINAL_DECISION_WELSH, REPRESENTATIVE, ONLINE, false, false, false, true},
            new Object[]{POSTPONEMENT, APPELLANT, ONLINE, true, true, false, true},
            new Object[]{POSTPONEMENT, APPELLANT, PAPER, true, true, false, true},
            new Object[]{POSTPONEMENT, APPELLANT, REGULAR, true, true, false, true},
            new Object[]{POSTPONEMENT, REPRESENTATIVE, ONLINE, true, true, false, true},
            new Object[]{POSTPONEMENT, REPRESENTATIVE, PAPER, true, true, false, true},
            new Object[]{POSTPONEMENT, REPRESENTATIVE, REGULAR, true, true, false, true},
            new Object[]{PROVIDE_APPOINTEE_DETAILS, APPOINTEE, REGULAR, false, false, false, true},
            new Object[]{PROVIDE_APPOINTEE_DETAILS, REPRESENTATIVE, REGULAR, false, false, false, true},
            new Object[]{REQUEST_FOR_INFORMATION, APPELLANT, ONLINE, false, false, false, true},
            new Object[]{REQUEST_FOR_INFORMATION, APPOINTEE, ONLINE, false, false, false, true},
            new Object[]{REQUEST_FOR_INFORMATION, REPRESENTATIVE, ONLINE, false, false, false, true},
            new Object[]{RESEND_APPEAL_CREATED, APPELLANT, ONLINE, true, true, false, false},
            new Object[]{RESEND_APPEAL_CREATED, APPELLANT, PAPER, true, true, false, false},
            new Object[]{RESEND_APPEAL_CREATED, APPELLANT, REGULAR, true, true, false, false},
            new Object[]{RESEND_APPEAL_CREATED, REPRESENTATIVE, ONLINE, true, true, false, false},
            new Object[]{RESEND_APPEAL_CREATED, REPRESENTATIVE, PAPER, true, true, false, false},
            new Object[]{RESEND_APPEAL_CREATED, REPRESENTATIVE, REGULAR, true, true, false, false},
            new Object[]{REVIEW_CONFIDENTIALITY_REQUEST, APPELLANT, REGULAR, false, false, false, true},
            new Object[]{REVIEW_CONFIDENTIALITY_REQUEST, APPOINTEE, REGULAR, false, false, false, true},
            new Object[]{REVIEW_CONFIDENTIALITY_REQUEST, JOINT_PARTY, REGULAR, false, false, false, true},
            new Object[]{REVIEW_CONFIDENTIALITY_REQUEST, REPRESENTATIVE, REGULAR, false, false, false, true},
            new Object[]{SYA_APPEAL_CREATED, APPELLANT, ONLINE, true, true, true, false},
            new Object[]{SYA_APPEAL_CREATED, APPELLANT, PAPER, true, true, true, false},
            new Object[]{SYA_APPEAL_CREATED, APPELLANT, REGULAR, true, true, true, false},
            new Object[]{SYA_APPEAL_CREATED, APPOINTEE, ONLINE, true, true, true, false},
            new Object[]{SYA_APPEAL_CREATED, APPOINTEE, PAPER, true, true, true, false},
            new Object[]{SYA_APPEAL_CREATED, APPOINTEE, REGULAR, true, true, true, false},
            new Object[]{SYA_APPEAL_CREATED, REPRESENTATIVE, ONLINE, true, true, true, false},
            new Object[]{SYA_APPEAL_CREATED, REPRESENTATIVE, PAPER, true, true, true, false},
            new Object[]{SYA_APPEAL_CREATED, REPRESENTATIVE, REGULAR, true, true, true, false},
            new Object[]{VALID_APPEAL_CREATED, APPELLANT, ONLINE, true, true, true, true},
            new Object[]{VALID_APPEAL_CREATED, APPELLANT, PAPER, true, true, true, true},
            new Object[]{VALID_APPEAL_CREATED, APPELLANT, REGULAR, true, true, true, true},
            new Object[]{VALID_APPEAL_CREATED, APPOINTEE, ONLINE, true, true, true, true},
            new Object[]{VALID_APPEAL_CREATED, APPOINTEE, PAPER, true, true, true, true},
            new Object[]{VALID_APPEAL_CREATED, APPOINTEE, REGULAR, true, true, true, true},
            new Object[]{VALID_APPEAL_CREATED, REPRESENTATIVE, ONLINE, true, true, true, true},
            new Object[]{VALID_APPEAL_CREATED, REPRESENTATIVE, PAPER, true, true, true, true},
            new Object[]{VALID_APPEAL_CREATED, REPRESENTATIVE, REGULAR, true, true, true, true},
        };
    }

    @Test
    @Parameters({
        "PIP,judge\\, doctor and disability expert, Personal Independence Payment, Taliad Annibyniaeth Personol, barnwr\\, meddyg ac arbenigwr anableddau, PIP, PIP",
        "ESA,judge and a doctor, Employment and Support Allowance, Lwfans Cyflogaeth a Chymorth, barnwr a meddyg, ESA, ESA",
        "UC,judge\\, doctor and disability expert (if applicable), Universal Credit, Credyd Cynhwysol, barnwr\\, meddyg ac arbenigwr anabledd (os yw’n berthnasol), UC, UC",
        "DLA,judge\\, doctor and disability expert, Disability Living Allowance, Lwfans Byw i’r Anabl, barnwr\\, meddyg ac arbenigwr anableddau, DLA,DLA",
        "carersAllowance,judge, Carer's Allowance, Lwfans Gofalwr, barnwr, Carer's Allowance, Lwfans Gofalwr",
        "attendanceAllowance,judge\\, doctor and disability expert, Attendance Allowance, Lwfans Gweini, barnwr\\, meddyg ac arbenigwr anableddau, Attendance Allowance, Lwfans Gweini",
        "bereavementBenefit,judge, Bereavement Benefit, Budd-dal Profedigaeth, barnwr, Bereavement Benefit, Budd-dal Profedigaeth",
        "taxCredit, judge and Financially Qualified Panel Member (if applicable), Tax Credit, Credyd Treth, Barnwr ac Aelod Panel sydd â chymhwyster i ddelio gyda materion Ariannol (os yw’n berthnasol), Tax Credit, Credyd Treth",
        "infectedBloodCompensation,judge and if applicable a medical member and/or a qualified tribunal member, Infected Blood Compensation, Iawndal Gwaed Heintiedig, barnwr ac os yw’n berthnasol\\, aelod meddygol ac/neu aelod tribiwnlys cymwys, IBC, IGH"
    })
    public void customisePersonalisation(String benefitType,
                                         String expectedPanelComposition,
                                         String expectedBenefitDesc,
                                         String welshExpectedBenefitDesc,
                                         String welshExpectedPanelComposition,
                                         String expectedAcronym,
                                         String expectedWelshAcronym) {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(EventType.APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitType).build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .events(events)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(APPEAL_RECEIVED).build(),
            new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT,
                response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");
        String expectedDecisionPostedReceiveDate = dateFormatter.format(LocalDate.now().plusDays(7));
        assertEquals(expectedDecisionPostedReceiveDate, result.get("decision_posted_receive_date"));

        assertEquals(expectedPanelComposition, result.get(PANEL_COMPOSITION));
        assertEquals(welshExpectedPanelComposition, result.get(PANEL_COMPOSITION_WELSH));

        assertEquals(expectedAcronym, result.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals(expectedWelshAcronym, result.get(BENEFIT_NAME_ACRONYM_LITERAL_WELSH));
        assertEquals(expectedBenefitDesc, result.get(BENEFIT_FULL_NAME_LITERAL));
        assertEquals(welshExpectedBenefitDesc, result.get(BENEFIT_FULL_NAME_LITERAL_WELSH));
        assertEquals(getLongBenefitNameDescriptionWithOptionalAcronym(benefitType, true), result.get(BENEFIT_NAME_AND_OPTIONAL_ACRONYM));
        assertEquals(getLongBenefitNameDescriptionWithOptionalAcronym(benefitType, false), result.get(BENEFIT_NAME_AND_OPTIONAL_ACRONYM_WELSH));
        assertEquals("SC/1234/5", result.get(APPEAL_REF));
        assertEquals("SC/1234/5", result.get(CASE_REFERENCE_ID));
        assertEquals("GLSCRR", result.get(APPEAL_ID_LITERAL));
        assertEquals("Harry Kane", result.get(NAME));
        assertEquals("Harry Kane", result.get(APPELLANT_NAME));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));

        if (benefitType.equals("taxCredit")) {
            assertEquals(HMRC_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
            assertEquals(HMRC_FULL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
            assertEquals(HMRC_FULL_NAME_WELSH, result.get(FIRST_TIER_AGENCY_FULL_NAME_WELSH));
            assertEquals(HMRC_ACRONYM, result.get(FIRST_TIER_AGENCY_GROUP));
            assertEquals(HMRC_ACRONYM, result.get(FIRST_TIER_AGENCY_GROUP_WELSH));
            assertEquals("", result.get(WITH_OPTIONAL_THE));
            assertEquals("", result.get(WITH_OPTIONAL_THE_WELSH));
            assertEquals("0300 999 8888", result.get(PHONE_NUMBER));
            assertEquals(PHONE_WELSH, result.get(PHONE_NUMBER_WELSH));
        } else if (benefitType.equals("infectedBloodCompensation")) {
            assertEquals(IBCA_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
            assertEquals(IBCA_FULL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
            assertEquals(IBCA_FULL_NAME_WELSH, result.get(FIRST_TIER_AGENCY_FULL_NAME_WELSH));
            assertEquals(IBCA_FIRST_TIER_AGENCY_GROUP, result.get(FIRST_TIER_AGENCY_GROUP));
            assertEquals(IBCA_FIRST_TIER_AGENCY_GROUP_WELSH, result.get(FIRST_TIER_AGENCY_GROUP_WELSH));
            assertEquals("", result.get(WITH_OPTIONAL_THE));
            assertEquals("", result.get(WITH_OPTIONAL_THE_WELSH));
            assertEquals("01274267247", result.get(HELPLINE_PHONE_NUMBER));
            assertEquals("0300 303 5170", result.get(PHONE_NUMBER_WELSH));
            assertEquals("01274267247", result.get(PHONE_NUMBER));
        } else {
            assertEquals(DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
            assertEquals(DWP_FULL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
            assertEquals(DWP_FULL_NAME_WELSH, result.get(FIRST_TIER_AGENCY_FULL_NAME_WELSH));
            assertEquals(DWP_FIRST_TIER_AGENCY_GROUP, result.get(FIRST_TIER_AGENCY_GROUP));
            assertEquals(DWP_FIRST_TIER_AGENCY_GROUP_WELSH, result.get(FIRST_TIER_AGENCY_GROUP_WELSH));
            assertEquals(THE_STRING, result.get(WITH_OPTIONAL_THE));
            assertEquals(THE_STRING_WELSH, result.get(WITH_OPTIONAL_THE_WELSH));
            assertEquals("0300 999 8888", result.get(PHONE_NUMBER));
            assertEquals(PHONE_WELSH, result.get(PHONE_NUMBER_WELSH));
        }

        assertEquals("29 July 2018", result.get(APPEAL_RESPOND_DATE));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/expenses", result.get(CLAIMING_EXPENSES_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/abouthearing", result.get(HEARING_INFO_LINK_LITERAL));
        assertNull(result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
        assertEquals(EMPTY, result.get(JOINT));
        assertEquals(EMPTY, result.get(JOINT_WELSH));
        assertNull(result.get(JOINT_PARTY));

        assertEquals(ADDRESS1, result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(ADDRESS2, result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals(ADDRESS3, result.get(ADDRESS_LINE_LITERAL));
        assertEquals(ADDRESS4, result.get(TOWN_LITERAL));
        assertEquals(CITY, result.get(COUNTY_LITERAL));
        assertEquals(POSTCODE, result.get(POSTCODE_LITERAL));
        assertEquals(CASE_ID, result.get(CCD_ID));
        assertEquals("1 February 2019", result.get(TRIBUNAL_RESPONSE_DATE_LITERAL));
        assertEquals("1 February 2018", result.get(ACCEPT_VIEW_BY_DATE_LITERAL));
        assertEquals("1 January 2018", result.get(QUESTION_ROUND_EXPIRES_DATE_LITERAL));
        assertEquals("", result.get(APPOINTEE_DESCRIPTION));
    }

    @Test
    @Parameters({"null, SSCS1", "null, SSCS2", "null, SSCS5", "null, SSCS8", "null, null", ",null"})
    public void givenFormTypeWithNoBenefitType_customisePersonalisation(@Nullable String benefitType, @Nullable FormType formType) {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(EventType.APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .formType(formType)
            .appeal(Appeal.builder()
                .appellant(Appellant.builder().name(name).build())
                .benefitType(BenefitType.builder().code(benefitType).build())
                .build())
            .subscriptions(subscriptions)
            .events(events)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");
        String expectedDecisionPostedReceiveDate = dateFormatter.format(LocalDate.now().plusDays(7));
        assertEquals(expectedDecisionPostedReceiveDate, result.get("decision_posted_receive_date"));

        assertEquals("judge, doctor and disability expert (if applicable)", result.get(PANEL_COMPOSITION));

        assertNull(result.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertNull(result.get(BENEFIT_FULL_NAME_LITERAL));
        assertNull(result.get(BENEFIT_NAME_AND_OPTIONAL_ACRONYM));
        assertNull(result.get(BENEFIT_NAME_AND_OPTIONAL_ACRONYM_WELSH));
        assertEquals("SC/1234/5", result.get(APPEAL_REF));
        assertEquals("SC/1234/5", result.get(CASE_REFERENCE_ID));
        assertEquals("GLSCRR", result.get(APPEAL_ID_LITERAL));
        assertEquals("Harry Kane", result.get(NAME));
        assertEquals("Harry Kane", result.get(APPELLANT_NAME));
        assertEquals("0300 999 8888", result.get(PHONE_NUMBER));
        assertEquals(PHONE_WELSH, result.get(PHONE_NUMBER_WELSH));
        assertNull(result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));

        if (FormType.SSCS5.equals(formType)) {
            assertEquals(HMRC_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
            assertEquals(HMRC_FULL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
            assertEquals(HMRC_FULL_NAME_WELSH, result.get(FIRST_TIER_AGENCY_FULL_NAME_WELSH));
            assertEquals(HMRC_ACRONYM, result.get(FIRST_TIER_AGENCY_GROUP));
            assertEquals(HMRC_ACRONYM, result.get(FIRST_TIER_AGENCY_GROUP_WELSH));
            assertEquals("", result.get(WITH_OPTIONAL_THE));
            assertEquals("", result.get(WITH_OPTIONAL_THE_WELSH));
        } else if (FormType.SSCS8.equals(formType)) {
            assertEquals(IBCA_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
            assertEquals(IBCA_FULL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
            assertEquals(IBCA_FULL_NAME_WELSH, result.get(FIRST_TIER_AGENCY_FULL_NAME_WELSH));
            assertEquals(IBCA_FULL_NAME, result.get(FIRST_TIER_AGENCY_GROUP));
            assertEquals(IBCA_FULL_NAME_WELSH, result.get(FIRST_TIER_AGENCY_GROUP_WELSH));
            assertEquals("", result.get(WITH_OPTIONAL_THE));
            assertEquals("", result.get(WITH_OPTIONAL_THE_WELSH));
        } else {
            assertEquals(DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
            assertEquals(DWP_FULL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
            assertEquals(DWP_FULL_NAME_WELSH, result.get(FIRST_TIER_AGENCY_FULL_NAME_WELSH));
            assertEquals(DWP_FIRST_TIER_AGENCY_GROUP, result.get(FIRST_TIER_AGENCY_GROUP));
            assertEquals(DWP_FIRST_TIER_AGENCY_GROUP_WELSH, result.get(FIRST_TIER_AGENCY_GROUP_WELSH));
            assertEquals(THE_STRING, result.get(WITH_OPTIONAL_THE));
            assertEquals(THE_STRING_WELSH, result.get(WITH_OPTIONAL_THE_WELSH));
        }

        assertEquals("29 July 2018", result.get(APPEAL_RESPOND_DATE));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/expenses", result.get(CLAIMING_EXPENSES_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/abouthearing", result.get(HEARING_INFO_LINK_LITERAL));
        assertNull(result.get(EVIDENCE_RECEIVED_DATE_LITERAL));

        assertEquals(ADDRESS1, result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(ADDRESS2, result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals(ADDRESS3, result.get(ADDRESS_LINE_LITERAL));
        assertEquals(ADDRESS4, result.get(TOWN_LITERAL));
        assertEquals(CITY, result.get(COUNTY_LITERAL));
        assertEquals(POSTCODE, result.get(POSTCODE_LITERAL));
        assertEquals(CASE_ID, result.get(CCD_ID));
        assertEquals("1 February 2019", result.get(TRIBUNAL_RESPONSE_DATE_LITERAL));
        assertEquals("1 February 2018", result.get(ACCEPT_VIEW_BY_DATE_LITERAL));
        assertEquals("1 January 2018", result.get(QUESTION_ROUND_EXPIRES_DATE_LITERAL));
        assertEquals("", result.get(APPOINTEE_DESCRIPTION));
    }

    @Test
    public void givenNoRpc_thenGivePhoneNumberBasedOnSc() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(EventType.APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(null)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .events(events)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("0300 999 8888", result.get(PHONE_NUMBER));
    }

    @Test
    @Parameters({"readyToList,0300 790 6234", ",telephone"})
    public void givenRpcAndReadyToList_thenGiveCorrectPhoneNumber(String createdInGapsFrom, String phone) {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(EventType.APPEAL_RECEIVED.getCcdType()).build()).build());

        RegionalProcessingCenter rpc = RegionalProcessingCenter
            .builder()
            .name("GLASGOW")
            .phoneNumber(phone)
            .build();

        when(regionalProcessingCenterService.getByScReferenceCode("SC085/1234/5")).thenReturn(rpc);

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC085/1234/5")
            .regionalProcessingCenter(null)
            .createdInGapsFrom(createdInGapsFrom)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .events(events)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(phone, result.get(PHONE_NUMBER));
    }

    @Test
    public void appealRefWillReturnCcdCaseIdWhenCaseReferenceIsNotSet() {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference(null)
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(CASE_ID, result.get(APPEAL_REF));
        assertEquals(CASE_ID, result.get(CASE_REFERENCE_ID));
    }

    @Test
    public void testCorrectionGrantedDwpState() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference(null)
            .dwpState(DwpState.CORRECTION_GRANTED)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(true, result.get(IS_GRANTED));
    }

    @Test
    public void testCorrectionRefusedDwpState() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference(null)
            .dwpState(DwpState.CORRECTION_REFUSED)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(false, result.get(IS_GRANTED));
    }

    @Test
    public void appealRefWillReturnCcdCaseIdWhenCreatedInGapsFromReadyToList() {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("readyToList")
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(CASE_ID, result.get(APPEAL_REF));
        assertEquals(CASE_ID, result.get(CASE_REFERENCE_ID));
    }

    @Test
    public void appealRefWillReturnCaseReferenceWhenCreatedInGapsFromValidAppeal() {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("validAppeal")
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("SC/1234/5", result.get(APPEAL_REF));
        assertEquals("SC/1234/5", result.get(CASE_REFERENCE_ID));
    }

    @Test
    public void givenEvidenceReceivedNotification_customisePersonalisation() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(EventType.APPEAL_RECEIVED.getCcdType()).build()).build());

        List<Document> documents = new ArrayList<>();

        Document doc = Document.builder().value(DocumentDetails.builder()
            .dateReceived("2018-07-01")
            .evidenceType("Medical")
            .evidenceProvidedBy("Caseworker").build()).build();

        documents.add(doc);

        Evidence evidence = Evidence.builder().documents(documents).build();

        Subscription appellantSubscription = Subscription.builder()
            .tya("GLSCRR")
            .email("test@email.com")
            .mobile("07983495065")
            .subscribeEmail("Yes")
            .subscribeSms("No")
            .build();

        Subscriptions subscriptions = Subscriptions.builder().appellantSubscription(appellantSubscription).build();

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .events(events)
            .evidence(evidence)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(response).notificationEventType(EVIDENCE_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("1 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
        assertNull("Welsh evidence received date not set", result.get(EVIDENCE_RECEIVED_DATE_LITERAL_WELSH));
    }


    @Test
    public void givenEvidenceReceivedNotification_customisePersonalisation_welsh() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(EventType.APPEAL_RECEIVED.getCcdType()).build()).build());

        List<Document> documents = new ArrayList<>();

        Document doc = Document.builder().value(DocumentDetails.builder()
            .dateReceived("2018-07-01")
            .evidenceType("Medical")
            .evidenceProvidedBy("Caseworker").build()).build();

        documents.add(doc);

        Evidence evidence = Evidence.builder().documents(documents).build();

        Subscription appellantSubscription = Subscription.builder()
            .tya("GLSCRR")
            .email("test@email.com")
            .mobile("07983495065")
            .subscribeEmail("Yes")
            .subscribeSms("No")
            .build();

        Subscriptions subscriptions = Subscriptions.builder().appellantSubscription(appellantSubscription).build();

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .events(events)
            .evidence(evidence)
            .languagePreferenceWelsh("Yes")
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(response).notificationEventType(EVIDENCE_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("1 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
        assertEquals("Welsh evidence received date not set", getWelshDate().apply(result.get(EVIDENCE_RECEIVED_DATE_LITERAL), dateTimeFormatter), result.get(EVIDENCE_RECEIVED_DATE_LITERAL_WELSH));
    }

    @Test
    public void setAppealReceivedEventData() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(EventType.APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .events(events)
            .build();

        Map result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);

        assertNull("Welsh date is not set ", result.get(APPEAL_RESPOND_DATE_WELSH));
        assertEquals("29 July 2018", result.get(APPEAL_RESPOND_DATE));
    }


    @Test
    public void setAppealReceivedEventData_Welsh() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(EventType.APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .events(events)
            .languagePreferenceWelsh("yes")
            .build();

        Map result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);
        assertEquals("Welsh date is set ", getWelshDate().apply(result.get(APPEAL_RESPOND_DATE), dateTimeFormatter), result.get(APPEAL_RESPOND_DATE_WELSH));
        assertEquals("29 July 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void setAppealReceivedChildSupportEventData_Welsh() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(EventType.APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
            .events(events)
            .languagePreferenceWelsh("yes")
            .build();

        Map result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);
        assertEquals("Welsh date is set ", getWelshDate().apply(result.get(APPEAL_RESPOND_DATE), dateTimeFormatter), result.get(APPEAL_RESPOND_DATE_WELSH));
        assertEquals("12 August 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void givenDigitalCaseWithDateSentToDwp_thenUseCaseSentToDwpDateForAppealRespondDate() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .createdInGapsFrom("readyToList")
            .dateSentToDwp("2018-07-01")
            .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);
        assertNull("Welsh date is set ", result.get(APPEAL_RESPOND_DATE_WELSH));
        assertEquals("29 July 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void givenDigitalCaseWithDateSentToDwp_thenUseCaseSentToDwpDateForAppealRespondDate_Welsh() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .createdInGapsFrom("readyToList")
            .dateSentToDwp("2018-07-01")
            .languagePreferenceWelsh("yes")
            .build();

        Map<String, String> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);

        assertEquals("Welsh date is set ", getWelshDate().apply(result.get(APPEAL_RESPOND_DATE), dateTimeFormatter), result.get(APPEAL_RESPOND_DATE_WELSH));
        assertEquals("29 July 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void setAppealReceivedChildSupportEventData() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(EventType.APPEAL_RECEIVED.getCcdType()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
            .events(events)
            .build();

        Map result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);

        assertNull("Welsh date is not set ",  result.get(APPEAL_RESPOND_DATE_WELSH));
        assertEquals("12 August 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void givenDigitalCaseWithNoDateSentToDwp_thenUseTodaysDateForAppealRespondDate() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .createdInGapsFrom("readyToList")
            .build();

        Map result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);

        assertEquals(LocalDate.now().plusDays(personalisation.calculateMaxDwpResponseDays(response.getBenefitCode())).format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT)), result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void givenCaseWithCreatedDate_thenUseCreatedDate() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .caseCreated(LocalDate.now().minusDays(1).toString())
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(response).notificationEventType(APPEAL_RECEIVED).build(),
            new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(LocalDate.now().minusDays(1).toString(), result.get(CREATED_DATE));
    }

    @Test
    public void givenCaseWithCreatedDateSetToNull_thenUseTodaysDateForCreatedDate() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .caseCreated(null)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(response).notificationEventType(APPEAL_RECEIVED).build(),
            new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(LocalDate.now().toString(), result.get(CREATED_DATE));
    }

    @Test
    public void setJudgeDecisionAppealToProceedEventData() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(JUDGE_DECISION_APPEAL_TO_PROCEED.getId()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .events(events)
            .build();

        Map result = personalisation.setEventData(new HashMap<>(), response, JUDGE_DECISION_APPEAL_TO_PROCEED);

        assertEquals("29 July 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void setTcwDecisionAppealToProceedEventData() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(TCW_DECISION_APPEAL_TO_PROCEED.getId()).build()).build());

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .events(events)
            .build();

        Map result = personalisation.setEventData(new HashMap<>(), response, TCW_DECISION_APPEAL_TO_PROCEED);

        assertEquals("29 July 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @Test
    public void setEvidenceReceivedEventData() {
        List<Document> documents = new ArrayList<>();

        Document doc = Document.builder().value(DocumentDetails.builder()
            .dateReceived("2018-07-01")
            .evidenceType("Medical")
            .evidenceProvidedBy("Caseworker").build()).build();

        documents.add(doc);

        Evidence evidence = Evidence.builder().documents(documents).build();

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .evidence(evidence)
            .build();

        Map result = personalisation.setEvidenceReceivedNotificationData(new HashMap<>(), response, EVIDENCE_RECEIVED);

        assertEquals("1 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void setEvidenceReceivedEventDataWhenEvidenceIsEmpty() {
        List<Document> documents = null;

        Evidence evidence = Evidence.builder().documents(documents).build();

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .evidence(evidence)
            .build();

        Map result = personalisation.setEvidenceReceivedNotificationData(new HashMap<>(), response, EVIDENCE_RECEIVED);

        assertEquals("", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
        assertEquals("", result.get(EVIDENCE_RECEIVED_DATE_LITERAL_WELSH));
    }

    @Test
    @Parameters(method = "generateHearingNotificationTypeAndSubscriptionsScenarios")
    public void givenHearingData_correctlySetTheHearingDetails(NotificationEventType hearingNotificationEventType,
                                                               SubscriptionType subscriptionType) {
        LocalDate hearingDate = LocalDate.now().plusDays(7);

        Hearing hearing = createHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .hearings(hearingList)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(response).notificationEventType(hearingNotificationEventType).build(),
            new SubscriptionWithType(subscriptions.getAppellantSubscription(), subscriptionType, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(hearingDate.toString(), result.get(HEARING_DATE_LITERAL));
        assertEquals("12:00 PM", result.get(HEARING_TIME).toString());
        assertEquals("The venue, 12 The Road Avenue, Village, Aberdeen, Aberdeenshire, TS3 3ST", result.get(VENUE_ADDRESS_LITERAL));
        assertEquals("http://www.googlemaps.com/aberdeenvenue", result.get(VENUE_MAP_LINK_LITERAL));
        assertEquals("in 7 days", result.get(DAYS_TO_HEARING_LITERAL));
    }

    @Test
    @Parameters(method = "generateHearingNotificationTypeAndSubscriptionsScenarios")
    public void givenHearingData_correctlySetTheHearingDetails_welsh(NotificationEventType hearingNotificationEventType,
                                                                     SubscriptionType subscriptionType) {
        LocalDate hearingDate = LocalDate.now().plusDays(7);

        Hearing hearing = createHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .hearings(hearingList)
            .languagePreferenceWelsh("Yes")
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(response).notificationEventType(hearingNotificationEventType).build(),
            new SubscriptionWithType(subscriptions.getAppellantSubscription(), subscriptionType, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("Welsh hearing date is not set", LocalDateToWelshStringConverter.convert(hearingDate), result.get(HEARING_DATE_WELSH));
        assertEquals(hearingDate.toString(), result.get(HEARING_DATE_LITERAL));
        assertEquals("12:00 PM", result.get(HEARING_TIME).toString().toUpperCase(Locale.getDefault()));
        assertEquals("The venue, 12 The Road Avenue, Village, Aberdeen, Aberdeenshire, TS3 3ST", result.get(VENUE_ADDRESS_LITERAL));
        assertEquals("http://www.googlemaps.com/aberdeenvenue", result.get(VENUE_MAP_LINK_LITERAL));
        assertEquals("in 7 days", result.get(DAYS_TO_HEARING_LITERAL));
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateHearingNotificationTypeAndSubscriptionsScenarios() {
        return new Object[]{
            new Object[]{HEARING_BOOKED, APPELLANT},
            new Object[]{HEARING_BOOKED, APPOINTEE},

            new Object[]{HEARING_REMINDER, APPELLANT},
            new Object[]{HEARING_REMINDER, APPOINTEE},
        };
    }

    @Test
    public void givenOnlyOneDayUntilHearing_correctlySetTheDaysToHearingText() {
        LocalDate hearingDate = LocalDate.now().plusDays(1);

        Hearing hearing = createHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .hearings(hearingList)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(HEARING_BOOKED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("tomorrow", result.get(DAYS_TO_HEARING_LITERAL));
    }

    @Test
    public void checkWelshDatesAreSet() {
        LocalDate hearingDate = LocalDate.now().plusDays(1);

        Hearing hearing = createHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .hearings(hearingList)
            .languagePreferenceWelsh("Yes")
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(HEARING_BOOKED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));
        assertEquals("Welsh current date is set", LocalDateToWelshStringConverter.convert(LocalDate.now()), result.get(CURRENT_DATE_WELSH));
        assertEquals("Welsh decision posted receive date", getWelshDate().apply(result.get(DECISION_POSTED_RECEIVE_DATE), dateTimeFormatter), result.get(DECISION_POSTED_RECEIVE_DATE_WELSH));
        assertEquals("tomorrow", result.get(DAYS_TO_HEARING_LITERAL));
    }

    @Test
    public void checkWelshDataAreNotSet() {
        LocalDate hearingDate = LocalDate.now().plusDays(1);

        Hearing hearing = createHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .hearings(hearingList)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(HEARING_BOOKED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));
        assertNull("Welsh current date is not set", result.get(CURRENT_DATE_WELSH));
        assertNull("Welsh decision posted receive date is not set", result.get(DECISION_POSTED_RECEIVE_DATE_WELSH));
        assertEquals("tomorrow", result.get(DAYS_TO_HEARING_LITERAL));
    }

    @Test
    public void checkListAssistDataIsSet() {
        LocalDateTime hearingDate = LocalDateTime.now().plusDays(1);

        Hearing hearing = createListAssistHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .hearings(hearingList)
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST)
                .build())
            .build();

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(HEARING_BOOKED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertThat(result.get(HEARING)).isNotNull();
        HearingDetails hearingDetails = (HearingDetails) result.get(HEARING);

        assertThat(hearingDetails.getHearingId()).isEqualTo(hearing.getValue().getHearingId());
        assertThat(hearingDetails.getHearingChannel()).isEqualTo(hearing.getValue().getHearingChannel());
        assertThat(hearingDetails.getEpimsId()).isEqualTo(hearing.getValue().getEpimsId());
        assertThat(hearingDetails.getStart()).isCloseTo(hearing.getValue().getStart(), within(1, ChronoUnit.HOURS));
        assertThat(hearingDetails.getEnd()).isCloseTo(hearing.getValue().getEnd(), within(1, ChronoUnit.HOURS));
        assertThat(hearingDetails.getHearingStatus()).isEqualTo(hearing.getValue().getHearingStatus());
        assertThat(hearingDetails.getVenue()).isEqualTo(hearing.getValue().getVenue());

        LocalDate dateParsed = LocalDate.parse(result.get(HEARING_DATE_LITERAL).toString(), CC_DATE_FORMAT);
        assertThat(dateParsed).isEqualTo(hearing.getValue().getStart().toLocalDate());
        LocalTime time = LocalTime.parse(result.get(HEARING_TIME).toString(), DateTimeFormatter.ofPattern(HEARING_TIME_FORMAT, Locale.ENGLISH));
        assertThat(time).isCloseTo(hearing.getValue().getStart().toLocalTime(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    public void handleNullEventWhenPopulatingEventData() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .build();

        Map result = personalisation.setEventData(new HashMap<>(), response, POSTPONEMENT);

        assertEquals(new HashMap<>(), result);
    }

    @Test
    public void handleEmptyEventsWhenPopulatingEventData() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .events(Collections.emptyList())
            .build();

        Map result = personalisation.setEventData(new HashMap<>(), response, POSTPONEMENT);

        assertEquals(new HashMap<>(), result);
    }

    @Test
    public void shouldPopulateRegionalProcessingCenterFromCcdCaseIfItsPresent() {
        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("LIVERPOOL").address1(ADDRESS1).address2(ADDRESS2).address3(ADDRESS3).address4(ADDRESS4).city(CITY).postcode(POSTCODE).build();

        SscsCaseData response = SscsCaseData.builder().regionalProcessingCenter(rpc).build();

        Map result = personalisation.setEvidenceProcessingAddress(new HashMap<>(), response);

        verify(regionalProcessingCenterService, never()).getByScReferenceCode(anyString());

        assertEquals(ADDRESS1, result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(ADDRESS2, result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals(ADDRESS3, result.get(ADDRESS_LINE_LITERAL));
        assertEquals(ADDRESS4, result.get(TOWN_LITERAL));
        assertEquals(CITY, result.get(COUNTY_LITERAL));
        assertEquals(POSTCODE, result.get(POSTCODE_LITERAL));
    }

    @Test
    @Parameters({"yes, scottishLine3, scottishPostcode, true",
        "no, line3, postcode, true",
        "yes, line3, postcode, false",
        "no, line3, postcode, false"})
    public void shouldPopulateSendEvidenceAddressToDigitalAddressWhenOnTheDigitalJourney(String isScottish, String expectedLine3, String expectedPostcode, boolean scottishPoBoxFeature) {

        SscsCaseData response = SscsCaseData.builder()
            .createdInGapsFrom(EventType.READY_TO_LIST.getCcdType())
            .isScottishCase(isScottish)
            .build();

        evidenceAddress.setScottishPoBoxFeatureEnabled(scottishPoBoxFeature);

        Map result = personalisation.setEvidenceProcessingAddress(new HashMap<>(), response);

        assertEquals(evidenceAddressLine1, result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(evidenceAddressLine2, result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals(expectedLine3, result.get(ADDRESS_LINE_LITERAL));
        assertEquals(evidenceAddressTown, result.get(TOWN_LITERAL));
        assertEquals(evidenceAddressCounty, result.get(COUNTY_LITERAL));
        assertEquals(expectedPostcode, result.get(POSTCODE_LITERAL));
        assertEquals(evidenceAddressTelephone, result.get(PHONE_NUMBER));
        assertEquals(evidenceAddressTelephoneWelsh, result.get(PHONE_NUMBER_WELSH));
    }

    @Test
    public void shouldNotPopulateRegionalProcessingCenterIfRpcCannotBeFound() {

        SscsCaseData response = SscsCaseData.builder().regionalProcessingCenter(null).build();

        when(regionalProcessingCenterService.getByScReferenceCode("SC/1234/5")).thenReturn(null);

        Map result = personalisation.setEvidenceProcessingAddress(new HashMap<>(), response);

        verify(regionalProcessingCenterService, never()).getByScReferenceCode(anyString());

        assertNull(result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertNull(result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertNull(result.get(ADDRESS_LINE_LITERAL));
        assertNull(result.get(TOWN_LITERAL));
        assertNull(result.get(COUNTY_LITERAL));
        assertNull(result.get(POSTCODE_LITERAL));
    }

    @Test
    public void shouldPopulateHearingContactDateFromCcdCaseIfPresent() {

        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder().newSscsCaseData(SscsCaseData.builder().build()).build();

        ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1528907807), ZoneId.of("UTC"));
        when(hearingContactDateExtractor.extract(wrapper)).thenReturn(Optional.of(now));

        Map<String, String> values = new HashMap<>();
        personalisation.setHearingContactDate(values, wrapper);

        assertEquals("13 June 2018", values.get(HEARING_CONTACT_DATE));
    }

    @Test
    public void shouldNotPopulateHearingContactDateFromCcdCaseIfNotPresent() {

        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder().newSscsCaseData(SscsCaseData.builder().build()).build();

        when(hearingContactDateExtractor.extract(wrapper)).thenReturn(Optional.empty());

        Map<String, String> values = new HashMap<>();
        personalisation.setHearingContactDate(values, wrapper);

        assertFalse(values.containsKey(HEARING_CONTACT_DATE));
    }


    @Test
    public void shouldPopulateAppointeeSubscriptionPersonalisation() {
        final String tyaNumber = "tya";
        Name appointeeName = Name.builder().title("MR").firstName("George").lastName("Appointee").build();
        when(macService.generateToken(tyaNumber, PIP.name())).thenReturn("ZYX");

        final SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .caseReference("SC/1234/5")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code(PIP.name())
                    .build())
                .appellant(Appellant.builder()
                    .name(name)
                    .appointee(Appointee.builder()
                        .name(appointeeName)
                        .build())
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appointeeSubscription(Subscription.builder()
                    .tya(tyaNumber)
                    .subscribeEmail("Yes")
                    .email("appointee@example.com")
                    .build())
                .representativeSubscription(Subscription.builder()
                    .tya("repTya")
                    .subscribeEmail("Yes")
                    .email("rep@example.com")
                    .build())
                .build())
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseData)
            .notificationEventType(SUBSCRIPTION_CREATED)
            .build(), new SubscriptionWithType(sscsCaseData.getSubscriptions().getAppointeeSubscription(), APPOINTEE, sscsCaseData.getAppeal().getAppellant(), sscsCaseData.getAppeal().getAppellant().getAppointee()));

        assertNotNull(result);
        assertEquals(CASE_ID, result.get(CCD_ID));
        assertEquals(appointeeName.getFullNameNoTitle(), result.get(NAME));
        assertEquals(name.getFullNameNoTitle(), result.get(APPELLANT_NAME));
        assertEquals(tyaNumber, result.get(APPEAL_ID_LITERAL));
        assertEquals(EMPTY, result.get(JOINT));
        assertEquals(EMPTY, result.get(JOINT_WELSH));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/" + tyaNumber, result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals("You are receiving this update as the appointee for Harry Kane.\r\n\r\n", result.get(APPOINTEE_DESCRIPTION));
        assertEquals("George Appointee", result.get(APPOINTEE_NAME));
    }

    @Test
    public void shouldPopulateRepSubscriptionPersonalisation() {
        final String tyaNumber = "tya";
        final String repTyaNumber = "repTya";
        when(macService.generateToken(repTyaNumber, PIP.name())).thenReturn("ZYX");

        final SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .caseReference("SC/1234/5")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code(PIP.name())
                    .build())
                .appellant(Appellant.builder()
                    .name(name)
                    .build())
                .rep(Representative.builder()
                    .name(name)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .tya(tyaNumber)
                    .subscribeEmail("Yes")
                    .email("appointee@example.com")
                    .build())
                .representativeSubscription(Subscription.builder()
                    .tya(repTyaNumber)
                    .subscribeEmail("Yes")
                    .email("rep@example.com")
                    .build())
                .build())
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .notificationEventType(SUBSCRIPTION_CREATED)
                .build(),
            new SubscriptionWithType(sscsCaseData.getSubscriptions()
                .getRepresentativeSubscription(), REPRESENTATIVE, sscsCaseData.getAppeal().getAppellant(),
                sscsCaseData.getAppeal().getRep()));

        assertNotNull(result);
        assertEquals(repTyaNumber, result.get(APPEAL_ID_LITERAL));
        assertEquals(EMPTY, result.get(JOINT));
        assertEquals(EMPTY, result.get(JOINT_WELSH));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/" + repTyaNumber, result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals("http://link.com/" + repTyaNumber, result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/" + repTyaNumber, result.get(SUBMIT_EVIDENCE_INFO_LINK_LITERAL));
    }

    @Test
    public void shouldPopulateJointPartySubscriptionPersonalisation() {
        final String tyaNumber = "tya";
        final String jointPartyTyaNumber = "jointPartyTya";
        when(macService.generateToken(jointPartyTyaNumber, PIP.name())).thenReturn("ZYX");

        final SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .jointParty(JointParty.builder()
                .hasJointParty(YES)
                .name(Name.builder().title("Mr").firstName("Bob").lastName("Builder").build())
                .jointPartyAddressSameAsAppellant(YES)
                .build())
            .caseReference("SC/1234/5")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code(PIP.name())
                    .build())
                .appellant(Appellant.builder()
                    .name(name)
                    .build())
                .rep(Representative.builder()
                    .name(name)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .tya(tyaNumber)
                    .subscribeEmail("Yes")
                    .email("appointee@example.com")
                    .build())
                .jointPartySubscription(Subscription.builder()
                    .tya(jointPartyTyaNumber)
                    .subscribeEmail("Yes")
                    .email("jp@example.com")
                    .build())
                .build())
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .notificationEventType(SUBSCRIPTION_CREATED)
                .build(),
            new SubscriptionWithType(sscsCaseData.getSubscriptions()
                .getJointPartySubscription(), JOINT_PARTY, sscsCaseData.getJointParty(),
                sscsCaseData.getJointParty()));

        assertNotNull(result);
        assertEquals(jointPartyTyaNumber, result.get(APPEAL_ID_LITERAL));
        assertEquals("Bob Builder", result.get(NAME));
        assertEquals(JOINT_TEXT_WITH_A_SPACE, result.get(JOINT));
        assertEquals(JOINT_TEXT_WITH_A_SPACE_WELSH, result.get(JOINT_WELSH));
        assertEquals("Yes", result.get(PersonalisationMappingConstants.JOINT_PARTY));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/" + jointPartyTyaNumber, result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals("http://link.com/" + jointPartyTyaNumber, result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/" + jointPartyTyaNumber, result.get(SUBMIT_EVIDENCE_INFO_LINK_LITERAL));
        assertEquals("Yes", result.get(PersonalisationMappingConstants.JOINT_PARTY));
    }

    @Test
    public void shouldPopulateOtherPartySubscriptionPersonalisation() {
        final String tyaNumber = "tya";
        final String otherPartyTyaNumber = "otherPartyTya";
        when(macService.generateToken(otherPartyTyaNumber, PIP.name())).thenReturn("ZYX");

        final SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .otherParties(List.of(new CcdValue<>(OtherParty.builder()
                .id("1")
                .otherPartySubscription(
                    Subscription.builder()
                        .tya(otherPartyTyaNumber)
                        .subscribeEmail("Yes")
                        .email("op@example.com")
                        .build()
                )
                .name(Name.builder().firstName("Bob").lastName("Builder").build())
                .build())))
            .caseReference("SC/1234/5")
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code(PIP.name())
                    .build())
                .appellant(Appellant.builder()
                    .name(name)
                    .build())
                .rep(Representative.builder()
                    .name(name)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .tya(tyaNumber)
                    .subscribeEmail("Yes")
                    .email("appointee@example.com")
                    .build())
                .build())
            .build();

        OtherParty otherParty = sscsCaseData.getOtherParties().get(0).getValue();
        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .notificationEventType(SUBSCRIPTION_CREATED)
                .build(),
            new SubscriptionWithType(otherParty
                .getOtherPartySubscription(), SubscriptionType.OTHER_PARTY, otherParty, otherParty,
                "1"));

        assertNotNull(result);
        assertEquals(otherPartyTyaNumber, result.get(APPEAL_ID_LITERAL));
        assertEquals("Bob Builder", result.get(NAME));
        assertEquals("http://link.com/manage-email-notifications/ZYX", result.get(MANAGE_EMAILS_LINK_LITERAL));
        assertEquals("http://tyalink.com/" + otherPartyTyaNumber, result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals("http://link.com/" + otherPartyTyaNumber, result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/" + otherPartyTyaNumber, result.get(SUBMIT_EVIDENCE_INFO_LINK_LITERAL));
    }

    @Test
    public void shouldHandleNoSubscription() {
        when(macService.generateToken(EMPTY, PIP.name())).thenReturn("ZYX");
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference(null)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(null,
            APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(CASE_ID, result.get(APPEAL_REF));
    }

    @Test
    public void getLatestInfoRequestDetailWhenNoneProvided() {
        assertNull(Personalisation.getLatestInfoRequestDetail(createResponseWithInfoRequests(null)));
    }

    @Test
    public void getLatestInfoRequestDetailWhenOneProvided() {
        String expected = "Request for information";

        List<AppellantInfoRequest> requests = Collections.singletonList(
            AppellantInfoRequest.builder()
                .id("456")
                .appellantInfo(
                    AppellantInfo.builder()
                        .requestDate("2019-01-09").paragraph(expected)
                        .build()
                )
                .build()
        );

        InfoRequests infoRequests = InfoRequests.builder()
            .appellantInfoRequest(requests)
            .build();
        String latestInfoRequest = Personalisation.getLatestInfoRequestDetail(createResponseWithInfoRequests(infoRequests));
        assertNotNull(latestInfoRequest);
        assertEquals(expected, latestInfoRequest);
    }

    @Test
    public void getLatestInfoRequestDetailWhenMultipleProvided() {
        String expected = "Final request for information";

        List<AppellantInfoRequest> requests = Arrays.asList(
            AppellantInfoRequest.builder()
                .id("123")
                .appellantInfo(
                    AppellantInfo.builder()
                        .requestDate("2019-01-10").paragraph("Please provide the information requested")
                        .build()
                )
                .build(),
            AppellantInfoRequest.builder()
                .id("789")
                .appellantInfo(
                    AppellantInfo.builder()
                        .requestDate("2019-01-11").paragraph(expected)
                        .build()
                )
                .build(),
            AppellantInfoRequest.builder()
                .id("456")
                .appellantInfo(
                    AppellantInfo.builder()
                        .requestDate("2019-01-09").paragraph("Request for information")
                        .build()
                )
                .build()
        );

        InfoRequests infoRequests = InfoRequests.builder()
            .appellantInfoRequest(requests)
            .build();
        String latestInfoRequest = Personalisation.getLatestInfoRequestDetail(createResponseWithInfoRequests(infoRequests));
        assertNotNull(latestInfoRequest);
        assertEquals(expected, latestInfoRequest);
    }

    @Test
    public void trackYourAppealWillReturnMyaLinkWhenCreatedInGapsFromReadyToList() {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("readyToList")
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("http://myalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals("http://myalink.com/claimingExpenses", result.get(CLAIMING_EXPENSES_LINK_LITERAL));
        assertEquals("http://myalink.com/evidenceSubmission", result.get(SUBMIT_EVIDENCE_INFO_LINK_LITERAL));
        assertEquals("http://myalink.com/evidenceSubmission", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://myalink.com/hearingInfo", result.get(HEARING_INFO_LINK_LITERAL));
    }

    @Test
    public void trackYourAppealWillReturnTyaLinkWhenCreatedInGapsFromIsNotReadyToList() {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("validAppeal")
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("http://tyalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/expenses", result.get(CLAIMING_EXPENSES_LINK_LITERAL));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_INFO_LINK_LITERAL));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/abouthearing", result.get(HEARING_INFO_LINK_LITERAL));
    }

    @Test
    @Parameters({"GRANTED", "REFUSED"})
    public void givenConfidentialRequestForAppellant_thenSetConfidentialFields(RequestOutcome requestOutcome) {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .jointParty(JointParty.builder()
                .name(Name.builder().firstName("Jeff").lastName("Stelling").build())
                .build())
            .confidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(requestOutcome).build())
            .build();

        Map<String, String> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(REVIEW_CONFIDENTIALITY_REQUEST).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("Jeff Stelling", result.get(OTHER_PARTY_NAME));
        assertEquals(requestOutcome.getValue(), result.get(CONFIDENTIALITY_OUTCOME));
    }

    @Test
    @Parameters({"GRANTED", "REFUSED"})
    public void givenConfidentialRequestForJointParty_thenSetConfidentialFields(RequestOutcome requestOutcome) {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .jointParty(JointParty.builder()
                .name(Name.builder().firstName("Jeff").lastName("Stelling").build())
                .build())
            .confidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(requestOutcome).build())
            .build();

        Map<String, String> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(REVIEW_CONFIDENTIALITY_REQUEST).build(),
            new SubscriptionWithType(subscriptions.getJointPartySubscription(),
                JOINT_PARTY, response.getJointParty(), response.getJointParty()));

        assertEquals(name.getFullNameNoTitle(), result.get(OTHER_PARTY_NAME));
        assertEquals(requestOutcome.getValue(), result.get(CONFIDENTIALITY_OUTCOME));
    }

    @Test
    @Parameters({"yes, 0300 790 6234", "no, 0300 123 1142"})
    public void setHelplineTelephoneNumber_relevantToTheCaseCountry(String isScottish, String helpLineTelephone) {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .isScottishCase(isScottish)
            .build();

        Map<String, String> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(VALID_APPEAL_CREATED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(helpLineTelephone, result.get(HELPLINE_PHONE_NUMBER));
    }

    @Test
    public void shouldPopulateCorrectlyWithEntityAndParty() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder()
                    .name(name)
                    .appointee(Appointee.builder()
                        .name(Name.builder()
                            .firstName("Appointee")
                            .lastName("Name")
                            .build())
                        .build())
                    .build())
                .build())
            .build();

        Map<String, String> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(VALID_APPEAL_CREATED).build(),
            new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPOINTEE,
                response.getAppeal().getAppellant(), response.getAppeal().getAppellant().getAppointee()));

        assertThat(result)
            .containsEntry(NAME, "Appointee Name")
            .containsEntry(REPRESENTEE_NAME, "Harry Kane")
            .containsEntry(PARTY_TYPE, "Appellant")
            .containsEntry(ENTITY_TYPE, "Appointee");
    }


    @SuppressWarnings("unchecked")
    @Test
    public void shouldProvideCorrectValuesForPtaGrantedValues() {
        String date1 = LocalDate.now().toString();
        String date2 = LocalDate.now().minusDays(10).toString();
        SscsDocumentDetails document1 = SscsDocumentDetails.builder()
            .documentType(DocumentType.FINAL_DECISION_NOTICE.getValue())
            .documentDateAdded(date1)
            .build();
        SscsDocument sscsDocument1 = SscsDocument.builder().value(document1).build();

        SscsDocumentDetails document2 = SscsDocumentDetails.builder()
            .documentType(DocumentType.FINAL_DECISION_NOTICE.getValue())
            .documentDateAdded(date2)
            .build();
        SscsDocument sscsDocument2 = SscsDocument.builder().value(document2).build();

        DynamicListItem item = new DynamicListItem("appellant", "");
        DynamicList originalSender = new DynamicList(item, List.of());

        Appellant appellant = Appellant.builder().name(name).build();
        Appeal appeal = Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).appellant(appellant)
            .build();
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .sscsDocument(List.of(sscsDocument1, sscsDocument2))
            .originalSender(originalSender)
            .appeal(appeal)
            .dwpState(DwpState.PERMISSION_TO_APPEAL_GRANTED)
            .build();

        SubscriptionWithType subscription = new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT,
            appellant, appellant);
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(response)
            .notificationEventType(PERMISSION_TO_APPEAL_GRANTED).build();
        var result = personalisation.create(caseDataWrapper, subscription);

        assertThat(result)
            .containsEntry(IS_GRANTED, true)
            .containsEntry(SENDER_NAME, name.getFullNameNoTitle())
            .containsEntry(DECISION_DATE_LITERAL, date1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldProvideCorrectValuesForPtaRefusedValues() {
        String date = LocalDate.now().toString();
        SscsDocumentDetails document1 = SscsDocumentDetails.builder()
            .documentType(DocumentType.FINAL_DECISION_NOTICE.getValue())
            .documentDateAdded(date)
            .build();
        SscsDocument sscsDocument = SscsDocument.builder().value(document1).build();

        DynamicListItem item = new DynamicListItem("appellant", "");
        DynamicList originalSender = new DynamicList(item, List.of());

        Appellant appellant = Appellant.builder().name(name).build();
        Appeal appeal = Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).appellant(appellant).build();
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).appeal(appeal)
            .sscsDocument(List.of(sscsDocument))
            .originalSender(originalSender)
            .build();
        SubscriptionWithType subscription = new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT,
            appellant, appellant);
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(PERMISSION_TO_APPEAL_REFUSED).build();
        var result = personalisation.create(caseDataWrapper, subscription);

        assertThat(result)
            .containsEntry(IS_GRANTED, false)
            .containsEntry(SENDER_NAME, name.getFullNameNoTitle())
            .containsEntry(DECISION_DATE_LITERAL, date);
    }

    @Test
    public void givenASyaAppealWithHearingArrangements_setHearingArrangementsForTemplate() {
        List<String> arrangementList = new ArrayList<>();

        arrangementList.add("signLanguageInterpreter");
        arrangementList.add("hearingLoop");
        arrangementList.add("disabledAccess");

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                .arrangements(arrangementList)
                .languageInterpreter("Yes")
                .other("Other")
                .build()).build())
            .build();

        Map<String, String> result = personalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Required\n"
                + "\nSign interpreter: Required\n"
                + "\nHearing loop: Required\n"
                + "\nDisabled access: Required\n"
                + "\nAny other arrangements: Other",
            result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL));
        assertNull(result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL_WELSH));

    }

    @Test
    public void givenASyaAppealWithHearingArrangements_setHearingArrangementsForTemplate_Welsh() {

        List<String> arrangementList = new ArrayList<>();

        arrangementList.add("signLanguageInterpreter");
        arrangementList.add("hearingLoop");
        arrangementList.add("disabledAccess");

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .languagePreferenceWelsh("yes")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                .arrangements(arrangementList)
                .languageInterpreter("Yes")
                .other("Other")
                .build()).build())
            .build();

        Map<String, String> result = personalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Required\n"
                + "\nSign interpreter: Required\n"
                + "\nHearing loop: Required\n"
                + "\nDisabled access: Required\n"
                + "\nAny other arrangements: Other",
            result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL));

        assertEquals("Dehonglydd iaith arwyddion: Gofynnol\n"
                + "\nDehonglydd iaith arwyddion: Gofynnol\n"
                + "\nDolen glyw: Gofynnol\n"
                + "\nMynediad i bobl anab: Gofynnol\n"
                + "\nUnrhyw drefniadau eraill: Other",
            result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL_WELSH));
    }

    @Test
    public void testSetAsideGrantedDwpState() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference(null)
            .dwpState(DwpState.SET_ASIDE_GRANTED)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(true, result.get(IS_GRANTED));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldProvideCorrectValuesForBundleCreatedForUT() {
        String date = LocalDate.now().toString();
        SscsDocumentDetails document1 = SscsDocumentDetails.builder()
            .documentType(DocumentType.FINAL_DECISION_NOTICE.getValue())
            .documentDateAdded(date)
            .build();
        SscsDocument sscsDocument = SscsDocument.builder().value(document1).build();

        DynamicListItem item = new DynamicListItem("appellant", "");
        DynamicList originalSender = new DynamicList(item, List.of());

        Appellant appellant = Appellant.builder().name(name).build();
        Appeal appeal = Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).appellant(appellant).build();
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).appeal(appeal)
            .sscsDocument(List.of(sscsDocument))
            .originalSender(originalSender)
            .build();
        SubscriptionWithType subscription = new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT,
            appellant, appellant);
        NotificationSscsCaseDataWrapper caseDataWrapper = NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(BUNDLE_CREATED_FOR_UPPER_TRIBUNAL).build();
        var result = personalisation.create(caseDataWrapper, subscription);

        assertThat(result)
            .containsEntry(APPELLANT_NAME, name.getFullNameNoTitle())
            .containsEntry(ENTITY_TYPE, "Appellant")
            .containsEntry(DECISION_DATE_LITERAL, date);
    }

    @Test
    public void givenReviewAndSetAside_setCorrectPtaDecisionDate() {
        String date = LocalDate.now().toString();
        String date2 = LocalDate.now().minusDays(20).toString();
        SscsDocumentDetails document1 = SscsDocumentDetails.builder()
            .documentType(DocumentType.FINAL_DECISION_NOTICE.getValue())
            .documentDateAdded(date)
            .build();
        SscsDocumentDetails document2 = SscsDocumentDetails.builder()
            .documentType(DocumentType.FINAL_DECISION_NOTICE.getValue())
            .documentDateAdded(date2)
            .build();
        SscsDocument sscsDocument1 = SscsDocument.builder().value(document1).build();
        SscsDocument sscsDocument2 = SscsDocument.builder().value(document2).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscsDocument2);
        sscsDocuments.add(sscsDocument1);

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .sscsDocument(sscsDocuments)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder()
                    .name(name)
                    .appointee(Appointee.builder()
                        .name(Name.builder()
                            .firstName("Appointee")
                            .lastName("Name")
                            .build())
                        .build())
                    .build())
                .build())
            .build();

        Map<String, String> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(REVIEW_AND_SET_ASIDE).build(),
            new SubscriptionWithType(subscriptions.getAppellantSubscription(),
                APPOINTEE,
                response.getAppeal().getAppellant(),
                response.getAppeal().getAppellant().getAppointee()));

        assertThat(result)
            .containsEntry(DECISION_DATE_LITERAL, date);
    }

    @Test
    public void givenReviewAndSetAside_setCorrectPtaDecisionDateAndNoReviewAndSetAsideDocument_shouldNotHaveDecisionDate() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder()
                    .name(name)
                    .appointee(Appointee.builder()
                        .name(Name.builder()
                            .firstName("Appointee")
                            .lastName("Name")
                            .build())
                        .build())
                    .build())
                .build())
            .build();

        Map<String, String> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(REVIEW_AND_SET_ASIDE).build(),
            new SubscriptionWithType(subscriptions.getAppellantSubscription(),
                APPOINTEE, response.getAppeal().getAppellant(),
                response.getAppeal().getAppellant().getAppointee()));

        assertThat(result)
            .doesNotContainKey(DECISION_DATE_LITERAL);
    }

    @Test
    public void whenDwpStateIsLtaGranted_setIsGrantedToTrue() {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("validAppeal")
            .dwpState(DwpState.LIBERTY_TO_APPLY_GRANTED)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(LIBERTY_TO_APPLY_GRANTED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertTrue((boolean) result.get(IS_GRANTED));
    }

    @Test
    public void whenDwpStateIsLtaRefused_setIsGrantedToFalse() {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("validAppeal")
            .dwpState(DwpState.LIBERTY_TO_APPLY_REFUSED)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(LIBERTY_TO_APPLY_REFUSED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertFalse((boolean) result.get(IS_GRANTED));
    }

    @Test
    public void whenDwpStateIsCorrectionIssued_setIsGrantedToTrue() {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("validAppeal")
            .dwpState(DwpState.CORRECTED_DECISION_NOTICE_ISSUED)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(LIBERTY_TO_APPLY_GRANTED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertTrue((boolean) result.get(IS_GRANTED));
    }

    @Test
    public void whenPostHearingsIsTrueAndFinalDecisionIsSet_setFinalDecision() {
        ReflectionTestUtils.setField(personalisation, "isPostHearingsEnabled", true);
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("validAppeal")
            .dwpState(DwpState.CORRECTION_GRANTED)
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder().finalDecisionIssuedDate(LocalDate.now()).build())
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(CORRECTION_GRANTED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertTrue((boolean) result.get(IS_GRANTED));
        assertEquals(result.get(FINAL_DECISION_DATE), LocalDate.now().format(DateTimeFormatter.ofPattern(FINAL_DECISION_DATE_FORMAT)));
    }

    @Test
    public void whenPostHearingsIsTrueAndFinalDecisionIsNotSet_dontSetFinalDecision() {
        ReflectionTestUtils.setField(personalisation, "isPostHearingsEnabled", true);
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByScReferenceCode("SC/1234/5");
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("validAppeal")
            .dwpState(DwpState.CORRECTION_GRANTED)
            .build();

        Map result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(CORRECTION_GRANTED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertTrue((boolean) result.get(IS_GRANTED));
        assertNull(result.get(FINAL_DECISION_DATE));
    }

    private Hearing createHearing(LocalDate hearingDate) {
        return Hearing.builder().value(HearingDetails.builder()
            .hearingDate(hearingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            .time("12:00")
            .hearingId("1")
            .venue(Venue.builder()
                .name("The venue")
                .address(Address.builder()
                    .line1("12 The Road Avenue")
                    .line2("Village")
                    .town("Aberdeen")
                    .county("Aberdeenshire")
                    .postcode("TS3 3ST").build())
                .googleMapLink("http://www.googlemaps.com/aberdeenvenue")
                .build()).build()).build();
    }

    private Hearing createListAssistHearing(LocalDateTime hearingDate) {
        return Hearing.builder().value(HearingDetails.builder()
            .start(hearingDate)
            .end(hearingDate)
            .hearingChannel(HearingChannel.FACE_TO_FACE)
            .hearingStatus(HearingStatus.LISTED)
            .hearingId("1")
            .epimsId("223534")
            .venue(Venue.builder()
                .name("The venue")
                .address(Address.builder()
                    .line1("12 The Road Avenue")
                    .line2("Village")
                    .town("Aberdeen")
                    .county("Aberdeenshire")
                    .postcode("TS3 3ST").build())
                .googleMapLink("http://www.googlemaps.com/aberdeenvenue")
                .build()).build()).build();
    }

    private SscsCaseData createResponseWithInfoRequests(InfoRequests infoRequests) {
        return SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .infoRequests(infoRequests)
            .build();
    }
}
