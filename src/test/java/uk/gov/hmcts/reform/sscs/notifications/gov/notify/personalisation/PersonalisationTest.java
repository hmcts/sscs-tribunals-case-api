package uk.gov.hmcts.reform.sscs.notifications.gov.notify.personalisation;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getLongBenefitNameDescriptionWithOptionalAcronym;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.ONLINE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.ORAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.PAPER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.REGULAR;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.HMC_HEARING_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.SscsCaseDataUtils.getWelshDate;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.CC_DATE_FORMAT;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.DWP_ACRONYM;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.DWP_FIRST_TIER_AGENCY_GROUP;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.DWP_FIRST_TIER_AGENCY_GROUP_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.DWP_FULL_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.DWP_FULL_NAME_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.FINAL_DECISION_DATE_FORMAT;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.HEARING_TIME_FORMAT;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.HMRC_ACRONYM;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.HMRC_FULL_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.HMRC_FULL_NAME_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.IBCA_ACRONYM;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.IBCA_FIRST_TIER_AGENCY_GROUP;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.IBCA_FIRST_TIER_AGENCY_GROUP_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.IBCA_FULL_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.IBCA_FULL_NAME_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.JOINT_TEXT_WITH_A_SPACE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.JOINT_TEXT_WITH_A_SPACE_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.MAX_DWP_RESPONSE_DAYS;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.MAX_DWP_RESPONSE_DAYS_CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.RESPONSE_DATE_FORMAT;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.THE_STRING;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.THE_STRING_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration.PersonalisationKey;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.ACCEPT_VIEW_BY_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.ADDRESS_LINE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.APPEAL_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.APPEAL_REF;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.APPEAL_RESPOND_DATE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.APPEAL_RESPOND_DATE_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.APPELLANT_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.APPOINTEE_DESCRIPTION;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.APPOINTEE_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.BENEFIT_FULL_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.BENEFIT_FULL_NAME_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.BENEFIT_NAME_ACRONYM_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.BENEFIT_NAME_ACRONYM_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.BENEFIT_NAME_AND_OPTIONAL_ACRONYM;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.BENEFIT_NAME_AND_OPTIONAL_ACRONYM_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.CASE_REFERENCE_ID;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.CCD_ID;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.CLAIMING_EXPENSES_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.CONFIDENTIALITY_OUTCOME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.COUNTY_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.CREATED_DATE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.CURRENT_DATE_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.DAYS_TO_HEARING_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.DECISION_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.DECISION_POSTED_RECEIVE_DATE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.DECISION_POSTED_RECEIVE_DATE_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.ENTITY_TYPE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.EVIDENCE_RECEIVED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.EVIDENCE_RECEIVED_DATE_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.FINAL_DECISION_DATE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.FIRST_TIER_AGENCY_ACRONYM;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.FIRST_TIER_AGENCY_FULL_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.FIRST_TIER_AGENCY_FULL_NAME_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.FIRST_TIER_AGENCY_GROUP;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.FIRST_TIER_AGENCY_GROUP_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.FORM_TYPE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.HEARING;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.HEARING_CONTACT_DATE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.HEARING_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.HEARING_DATE_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.HEARING_INFO_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.HEARING_TIME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.HELPLINE_PHONE_NUMBER;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.IS_GRANTED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.JOINT;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.JOINT_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.MANAGE_EMAILS_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.OTHER_PARTY_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.PANEL_COMPOSITION;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.PANEL_COMPOSITION_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.PARTY_TYPE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.PHONE_NUMBER;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.PHONE_NUMBER_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.QUESTION_ROUND_EXPIRES_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.REGIONAL_OFFICE_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.REPRESENTEE_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.SENDER_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.SUBMIT_EVIDENCE_INFO_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.SUBMIT_EVIDENCE_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.SUPPORT_CENTRE_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.TOWN_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.TRACK_APPEAL_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.TRIBUNAL_RESPONSE_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.VENUE_ADDRESS_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.VENUE_MAP_LINK_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.WITH_OPTIONAL_THE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.WITH_OPTIONAL_THE_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.SubscriptionType.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.ACTION_POSTPONEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.ACTION_POSTPONEMENT_REQUEST_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.ADJOURNED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.APPEAL_DORMANT;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.APPEAL_LAPSED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.BUNDLE_CREATED_FOR_UPPER_TRIBUNAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.CORRECTION_GRANTED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.DEATH_OF_APPELLANT;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.DECISION_ISSUED_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.DIRECTION_ISSUED_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.EVIDENCE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.HEARING_BOOKED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.HEARING_REMINDER;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.ISSUE_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.ISSUE_ADJOURNMENT_NOTICE_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.JUDGE_DECISION_APPEAL_TO_PROCEED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.LIBERTY_TO_APPLY_GRANTED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.LIBERTY_TO_APPLY_REFUSED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.PERMISSION_TO_APPEAL_GRANTED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.PERMISSION_TO_APPEAL_REFUSED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.POSTPONEMENT;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.PROVIDE_APPOINTEE_DETAILS;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.REQUEST_FOR_INFORMATION;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.RESEND_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.REVIEW_AND_SET_ASIDE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.REVIEW_CONFIDENTIALITY_REQUEST;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.SUBSCRIPTION_CREATED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.SYA_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.TCW_DECISION_APPEAL_TO_PROCEED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.VALID_APPEAL_CREATED;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationConfiguration;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.properties.EvidenceProperties;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.MessageAuthenticationServiceImpl;

@Slf4j
public class PersonalisationTest {

    private static final String CASE_ID = "54321";
    private static final String ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String ADDRESS2 = "Social Security & Child Support Appeals";
    private static final String ADDRESS3 = "Prudential Buildings";
    private static final String ADDRESS4 = "36 Dale Street";
    private static final String CITY = "LIVERPOOL";
    private static final String POSTCODE = "L2 5UZ";
    private static final String PHONE = "0300 999 8888";
    private static final String PHONE_WELSH = "0300 303 5170";
    private static final String PHONE_IBC = "0300 131 2850";
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
    public Personalisation<NotificationWrapper> personalisation;

    @Spy
    private PersonalisationConfiguration personalisationConfiguration;

    protected Subscriptions subscriptions;

    protected Name name;

    private RegionalProcessingCenter rpc;
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final String evidenceAddressLine1 = "line1";
    private final String evidenceAddressLine2 = "line2";
    private final String evidenceAddressLine3 = "line3";
    private final String evidenceAddressScottishLine3 = "scottishLine3";
    private final String evidenceIbcAddressLine1 = "ibcLine1";
    private final String evidenceIbcAddressLine2 = "ibcLine2";
    private final String evidenceIbcAddressLine3 = "ibcLine3";
    private final String evidenceIbcAddressPostcode = "ibcPostcode";
    private final String evidenceAddressTown = "town";
    private final String evidenceAddressCounty = "county";
    private final String evidenceAddressPostcode = "postcode";
    private final String evidenceAddressScottishPostcode = "scottishPostcode";
    private final String evidenceAddressTelephone = "telephone";
    private final String evidenceAddressTelephoneWelsh = PHONE_WELSH;
    private final String evidenceAddressTelephoneIbc = PHONE_IBC;
    private final EvidenceProperties.EvidenceAddress evidenceAddress = new EvidenceProperties.EvidenceAddress();
    private AutoCloseable autoCloseable;

    @BeforeEach
    public void setup() {
        autoCloseable = openMocks(this);
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
        when(config.getHelplineTelephoneIbc()).thenReturn("0300 131 2850");
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
        evidenceAddress.setIbcAddressLine1(evidenceIbcAddressLine1);
        evidenceAddress.setIbcAddressLine2(evidenceIbcAddressLine2);
        evidenceAddress.setIbcAddressLine3(evidenceIbcAddressLine3);
        evidenceAddress.setIbcAddressPostcode(evidenceIbcAddressPostcode);
        evidenceAddress.setTown(evidenceAddressTown);
        evidenceAddress.setCounty(evidenceAddressCounty);
        evidenceAddress.setPostcode(evidenceAddressPostcode);
        evidenceAddress.setScottishPostcode(evidenceAddressScottishPostcode);
        evidenceAddress.setTelephone(evidenceAddressTelephone);
        evidenceAddress.setTelephoneWelsh(evidenceAddressTelephoneWelsh);
        evidenceAddress.setTelephoneIbc(evidenceAddressTelephoneIbc);
        when(evidenceProperties.getAddress()).thenReturn(evidenceAddress);

        Map<String, String> englishMap = getEnglishMap();
        Map<String, String> welshMap = getWelshMap();

        Map<LanguagePreference, Map<String, String>> personalisations = new HashMap<>();
        personalisations.put(LanguagePreference.ENGLISH, englishMap);
        personalisations.put(LanguagePreference.WELSH, welshMap);
        personalisationConfiguration.setPersonalisation(personalisations);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    private static Map<String, String> getWelshMap() {
        Map<String, String> welshMap = new HashMap<>();
        welshMap.put(PersonalisationKey.LANGUAGE_INTERPRETER.name(), "Dehonglydd iaith arwyddion: ");
        welshMap.put(PersonalisationKey.SIGN_INTERPRETER.name(), "Dehonglydd iaith arwyddion: ");
        welshMap.put(PersonalisationKey.HEARING_LOOP.name(), "Dolen glyw: ");
        welshMap.put(PersonalisationKey.DISABLED_ACCESS.name(), "Mynediad i bobl anab: ");
        welshMap.put(PersonalisationKey.OTHER_ARRANGEMENTS.name(), "Unrhyw drefniadau eraill: ");
        welshMap.put(PersonalisationKey.REQUIRED.name(), "Gofynnol");
        welshMap.put(PersonalisationKey.NOT_REQUIRED.name(), "Dim yn ofynnol");
        return welshMap;
    }

    private static Map<String, String> getEnglishMap() {
        Map<String, String> englishMap = new HashMap<>();
        englishMap.put(PersonalisationKey.LANGUAGE_INTERPRETER.name(), "Language interpreter: ");
        englishMap.put(PersonalisationKey.SIGN_INTERPRETER.name(), "Sign interpreter: ");
        englishMap.put(PersonalisationKey.HEARING_LOOP.name(), "Hearing loop: ");
        englishMap.put(PersonalisationKey.DISABLED_ACCESS.name(), "Disabled access: ");
        englishMap.put(PersonalisationKey.OTHER_ARRANGEMENTS.name(), "Any other arrangements: ");
        englishMap.put(PersonalisationKey.REQUIRED.name(), "Required");
        englishMap.put(PersonalisationKey.NOT_REQUIRED.name(), "Not required");
        return englishMap;
    }


    @ParameterizedTest
    @CsvSource({"APPEAL_TO_PROCEED, directionIssued.appealToProceed, APPELLANT",
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

    @ParameterizedTest
    @CsvSource({"APPELLANT, grantUrgentHearing, directionIssued.grantUrgentHearing",
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
                .directionTypeDl(new DynamicList(directionTypeString))
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

    @ParameterizedTest
    @CsvSource({"APPELLANT, grantUrgentHearing, directionIssuedWelsh.grantUrgentHearing",
        "JOINT_PARTY, grantUrgentHearing, directionIssuedWelsh.grantUrgentHearing",
        "REPRESENTATIVE, grantUrgentHearing, directionIssuedWelsh.grantUrgentHearing",
        "APPOINTEE, grantUrgentHearing, directionIssuedWelsh.grantUrgentHearing",
        "APPELLANT, refuseUrgentHearing, directionIssuedWelsh.refuseUrgentHearing",
        "JOINT_PARTY, refuseUrgentHearing, directionIssuedWelsh.refuseUrgentHearing",
        "REPRESENTATIVE, refuseUrgentHearing, directionIssuedWelsh.refuseUrgentHearing",
        "APPOINTEE, refuseUrgentHearing, directionIssuedWelsh.refuseUrgentHearing"})
    public void whenDirectionIssuedWelshAndGrantOrRefuseUrgentHearingShouldGenerateCorrectTemplate(SubscriptionType subscriptionType, String directionTypeString, String templateConfig) {

        NotificationWrapper notificationWrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(SscsCaseData.builder()
                .directionTypeDl(new DynamicList(directionTypeString))
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


    @ParameterizedTest
    @CsvSource({"APPEAL_TO_PROCEED, directionIssuedWelsh.appealToProceed, APPELLANT",
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

    @ParameterizedTest
    @MethodSource("generateNotificationTypeAndSubscriptionsScenarios")
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
    private static Object[] generateNotificationTypeAndSubscriptionsScenarios() {
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

    @ParameterizedTest
    @CsvSource({
        "PIP,'judge, doctor and disability expert', Personal Independence Payment, Taliad Annibyniaeth Personol, 'barnwr, meddyg ac arbenigwr anableddau', PIP, PIP, sscs1",
        "ESA,judge and a doctor, Employment and Support Allowance, Lwfans Cyflogaeth a Chymorth, barnwr a meddyg, ESA, ESA, sscs1",
        "UC,'judge, doctor and disability expert (if applicable)', Universal Credit, Credyd Cynhwysol, 'barnwr, meddyg ac arbenigwr anabledd (os yw’n berthnasol)', UC, UC, sscs1",
        "DLA,'judge, doctor and disability expert', Disability Living Allowance, Lwfans Byw i’r Anabl, 'barnwr, meddyg ac arbenigwr anableddau', DLA,DLA, sscs1",
        "carersAllowance,judge, Carer's Allowance, Lwfans Gofalwr, barnwr, Carer's Allowance, Lwfans Gofalwr, sscs1",
        "attendanceAllowance,'judge, doctor and disability expert', Attendance Allowance, Lwfans Gweini, 'barnwr, meddyg ac arbenigwr anableddau', Attendance Allowance, Lwfans Gweini, sscs1",
        "bereavementBenefit,judge, Bereavement Benefit, Budd-dal Profedigaeth, barnwr, Bereavement Benefit, Budd-dal Profedigaeth, sscs1",
        "taxCredit, judge and Financially Qualified Panel Member (if applicable), Tax Credit, Credyd Treth, Barnwr ac Aelod Panel sydd â chymhwyster i ddelio gyda materion Ariannol (os yw’n berthnasol), Tax Credit, Credyd Treth, sscs5",
        "infectedBloodCompensation,judge and if applicable a medical member and/or a financially qualified tribunal member, Infected Blood Compensation, Iawndal Gwaed Heintiedig, barnwr ac os yw’n berthnasol aelod meddygol a/neu aelod o’r tribiwnlys sy’n gymwys mewn materion ariannol, IBC, IGH, sscs8"
    })
    public void customisePersonalisation(String benefitType,
                                         String expectedPanelComposition,
                                         String expectedBenefitDesc,
                                         String welshExpectedBenefitDesc,
                                         String welshExpectedPanelComposition,
                                         String expectedAcronym,
                                         String expectedWelshAcronym,
                                         String formType) {
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
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
        assertEquals(formType, result.get(FORM_TYPE));
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
            assertEquals(PHONE, result.get(PHONE_NUMBER));
            assertEquals(PHONE_WELSH, result.get(PHONE_NUMBER_WELSH));
        } else if (benefitType.equals("infectedBloodCompensation")) {
            assertEquals(IBCA_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
            assertEquals(IBCA_FULL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
            assertEquals(IBCA_FULL_NAME_WELSH, result.get(FIRST_TIER_AGENCY_FULL_NAME_WELSH));
            assertEquals(IBCA_FIRST_TIER_AGENCY_GROUP, result.get(FIRST_TIER_AGENCY_GROUP));
            assertEquals(IBCA_FIRST_TIER_AGENCY_GROUP_WELSH, result.get(FIRST_TIER_AGENCY_GROUP_WELSH));
            assertEquals("", result.get(WITH_OPTIONAL_THE));
            assertEquals("", result.get(WITH_OPTIONAL_THE_WELSH));
            assertEquals(PHONE_IBC, result.get(HELPLINE_PHONE_NUMBER));
            assertEquals(PHONE_WELSH, result.get(PHONE_NUMBER_WELSH));
            assertEquals(PHONE_IBC, result.get(PHONE_NUMBER));
        } else {
            assertEquals(DWP_ACRONYM, result.get(FIRST_TIER_AGENCY_ACRONYM));
            assertEquals(DWP_FULL_NAME, result.get(FIRST_TIER_AGENCY_FULL_NAME));
            assertEquals(DWP_FULL_NAME_WELSH, result.get(FIRST_TIER_AGENCY_FULL_NAME_WELSH));
            assertEquals(DWP_FIRST_TIER_AGENCY_GROUP, result.get(FIRST_TIER_AGENCY_GROUP));
            assertEquals(DWP_FIRST_TIER_AGENCY_GROUP_WELSH, result.get(FIRST_TIER_AGENCY_GROUP_WELSH));
            assertEquals(THE_STRING, result.get(WITH_OPTIONAL_THE));
            assertEquals(THE_STRING_WELSH, result.get(WITH_OPTIONAL_THE_WELSH));
            assertEquals(PHONE, result.get(PHONE_NUMBER));
            assertEquals(PHONE_WELSH, result.get(PHONE_NUMBER_WELSH));
        }

        assertEquals("29 July 2018", result.get(APPEAL_RESPOND_DATE));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/expenses", result.get(CLAIMING_EXPENSES_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/abouthearing", result.get(HEARING_INFO_LINK_LITERAL));
        assertNull(result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
        assertEquals(EMPTY, result.get(JOINT));
        assertEquals(EMPTY, result.get(JOINT_WELSH));
        assertNull(result.get(JOINT_PARTY.name()));

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

    @ParameterizedTest
    @CsvSource({", SSCS1", ", SSCS2", ", SSCS5", ", SSCS8", ","})
    public void givenFormTypeWithNoBenefitType_customisePersonalisation(String benefitType, FormType formType) {
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
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
        assertEquals(PHONE, result.get(PHONE_NUMBER));
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(PHONE, result.get(PHONE_NUMBER));
    }

    @ParameterizedTest
    @CsvSource({"readyToList,0300 790 6234", ",telephone"})
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(phone, result.get(PHONE_NUMBER));
    }

    @Test
    public void appealRefWillReturnCcdCaseIdWhenCaseReferenceIsNotSet() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference(null)
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .build();

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(false, result.get(IS_GRANTED));
    }

    @Test
    public void appealRefWillReturnCcdCaseIdWhenCreatedInGapsFromReadyToList() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("readyToList")
            .build();

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(CASE_ID, result.get(APPEAL_REF));
        assertEquals(CASE_ID, result.get(CASE_REFERENCE_ID));
    }

    @Test
    public void appealRefWillReturnCaseReferenceWhenCreatedInGapsFromValidAppeal() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("validAppeal")
            .build();

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(response).notificationEventType(EVIDENCE_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("1 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
        assertNull(result.get(EVIDENCE_RECEIVED_DATE_LITERAL_WELSH), "Welsh evidence received date not set");
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(response).notificationEventType(EVIDENCE_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("1 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
        assertEquals(getWelshDate().apply(result.get(EVIDENCE_RECEIVED_DATE_LITERAL), dateTimeFormatter), result.get(EVIDENCE_RECEIVED_DATE_LITERAL_WELSH), "Welsh evidence received date not set");
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

        Map<String, Object> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);

        assertNull(result.get(APPEAL_RESPOND_DATE_WELSH), "Welsh date is not set ");
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

        Map<String, Object> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);
        assertEquals(getWelshDate().apply(result.get(APPEAL_RESPOND_DATE), dateTimeFormatter), result.get(APPEAL_RESPOND_DATE_WELSH), "Welsh date is set ");
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

        Map<String, Object> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);
        assertEquals(getWelshDate().apply(result.get(APPEAL_RESPOND_DATE), dateTimeFormatter), result.get(APPEAL_RESPOND_DATE_WELSH), "Welsh date is set ");
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

        Map<String, Object> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);
        assertNull(result.get(APPEAL_RESPOND_DATE_WELSH), "Welsh date is set ");
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

        Map<String, Object> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);

        assertEquals(getWelshDate().apply(result.get(APPEAL_RESPOND_DATE), dateTimeFormatter), result.get(APPEAL_RESPOND_DATE_WELSH), "Welsh date is set ");
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

        Map<String, Object> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);

        assertNull(result.get(APPEAL_RESPOND_DATE_WELSH), "Welsh date is not set ");
        assertEquals("12 August 2018", result.get(APPEAL_RESPOND_DATE));
    }

    @ParameterizedTest
    @MethodSource("appealResponseDate")
    public void givenDigitalCaseWithNoDateSentToDwp_thenUseTodaysDateForAppealRespondDate(
        Appeal appeal, int responsePeriod) {

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .caseReference("SC/1234/5")
            .appeal(appeal)
            .createdInGapsFrom("readyToList")
            .build();

        Map<String, Object> result = personalisation.setEventData(new HashMap<>(), response, APPEAL_RECEIVED);

        assertThat(result.get(APPEAL_RESPOND_DATE))
            .isEqualTo(LocalDate.now()
                .plusDays(responsePeriod)
                .format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT)));
    }

    private static Stream<Arguments> appealResponseDate() {
        return Stream.of(
            Arguments.of(Appeal.builder()
                .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                .build(), MAX_DWP_RESPONSE_DAYS_CHILD_SUPPORT),
            Arguments.of(null, MAX_DWP_RESPONSE_DAYS),
            Arguments.of(Appeal.builder().build(), MAX_DWP_RESPONSE_DAYS),
            Arguments.of(Appeal.builder().benefitType(BenefitType.builder().build()).build(), MAX_DWP_RESPONSE_DAYS),
            Arguments.of(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.ESA.getShortName()).build()).build(), MAX_DWP_RESPONSE_DAYS)
        );
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
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

        Map<String, Object> result = personalisation.setEventData(new HashMap<>(), response, JUDGE_DECISION_APPEAL_TO_PROCEED);

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

        Map<String, Object> result = personalisation.setEventData(new HashMap<>(), response, TCW_DECISION_APPEAL_TO_PROCEED);

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

        Map<String, Object> result = personalisation.setEvidenceReceivedNotificationData(new HashMap<>(), response, EVIDENCE_RECEIVED);

        assertEquals("1 July 2018", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
    }

    @Test
    public void setEvidenceReceivedEventDataWhenEvidenceIsEmpty() {
        Evidence evidence = Evidence.builder().documents(null).build();

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .evidence(evidence)
            .build();

        Map<String, Object> result = personalisation.setEvidenceReceivedNotificationData(new HashMap<>(), response, EVIDENCE_RECEIVED);

        assertEquals("", result.get(EVIDENCE_RECEIVED_DATE_LITERAL));
        assertEquals("", result.get(EVIDENCE_RECEIVED_DATE_LITERAL_WELSH));
    }

    @ParameterizedTest
    @MethodSource("generateHearingNotificationTypeAndSubscriptionsScenarios")
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(response).notificationEventType(hearingNotificationEventType).build(),
            new SubscriptionWithType(subscriptions.getAppellantSubscription(), subscriptionType, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(hearingDate.toString(), result.get(HEARING_DATE_LITERAL));
        assertEquals("12:00 PM", result.get(HEARING_TIME).toString());
        assertEquals("The venue, 12 The Road Avenue, Village, Aberdeen, Aberdeenshire, TS3 3ST", result.get(VENUE_ADDRESS_LITERAL));
        assertEquals("http://www.googlemaps.com/aberdeenvenue", result.get(VENUE_MAP_LINK_LITERAL));
        assertEquals("in 7 days", result.get(DAYS_TO_HEARING_LITERAL));
    }

    @ParameterizedTest
    @MethodSource("generateHearingNotificationTypeAndSubscriptionsScenarios")
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(response).notificationEventType(hearingNotificationEventType).build(),
            new SubscriptionWithType(subscriptions.getAppellantSubscription(), subscriptionType, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(LocalDateToWelshStringConverter.convert(hearingDate), result.get(HEARING_DATE_WELSH), "Welsh hearing date is not set");
        assertEquals(hearingDate.toString(), result.get(HEARING_DATE_LITERAL));
        assertEquals("12:00 PM", result.get(HEARING_TIME).toString().toUpperCase(Locale.getDefault()));
        assertEquals("The venue, 12 The Road Avenue, Village, Aberdeen, Aberdeenshire, TS3 3ST", result.get(VENUE_ADDRESS_LITERAL));
        assertEquals("http://www.googlemaps.com/aberdeenvenue", result.get(VENUE_MAP_LINK_LITERAL));
        assertEquals("in 7 days", result.get(DAYS_TO_HEARING_LITERAL));
    }

    @SuppressWarnings({"Indentation", "unused"})
    private static Object[] generateHearingNotificationTypeAndSubscriptionsScenarios() {
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(HEARING_BOOKED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));
        assertEquals(LocalDateToWelshStringConverter.convert(LocalDate.now()), result.get(CURRENT_DATE_WELSH), "Welsh current date is set");
        assertEquals(getWelshDate().apply(result.get(DECISION_POSTED_RECEIVE_DATE), dateTimeFormatter), result.get(DECISION_POSTED_RECEIVE_DATE_WELSH), "Welsh decision posted receive date");
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(HEARING_BOOKED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));
        assertNull(result.get(CURRENT_DATE_WELSH), "Welsh current date is not set");
        assertNull(result.get(DECISION_POSTED_RECEIVE_DATE_WELSH), "Welsh decision posted receive date is not set");
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
    public void shouldReturnOverrideFieldsDirectionHearingForFaceToFaceHearingBookedNotification() {
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
                .overrideFields(OverrideFields.builder().hmcHearingType(HmcHearingType.DIRECTION_HEARINGS).build())
                .build())
            .build();

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(HEARING_BOOKED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertThat(result.get(HMC_HEARING_TYPE_LITERAL)).isNotNull();
        assertThat(result.get(HMC_HEARING_TYPE_LITERAL)).isEqualTo("BBA3-DIR");
    }

    @Test
    public void shouldReturnOverrideFieldsSubstantiveHearingForFaceToFaceHearingBookedNotification() {
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
                .overrideFields(OverrideFields.builder().hmcHearingType(HmcHearingType.SUBSTANTIVE).build())
                .build())
            .build();

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(HEARING_BOOKED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertThat(result.get(HMC_HEARING_TYPE_LITERAL)).isNotNull();
        assertThat(result.get(HMC_HEARING_TYPE_LITERAL)).isEqualTo("BBA3-SUB");
    }

    @Test
    public void shouldReturnDirectionHearingForFaceToFaceHearingBookedNotification() {
        LocalDateTime hearingDate = LocalDateTime.now().plusDays(1);

        Hearing hearing = createListAssistHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .hmcHearingType(HmcHearingType.DIRECTION_HEARINGS)
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

        assertThat(result.get(HMC_HEARING_TYPE_LITERAL)).isNotNull();
        assertThat(result.get(HMC_HEARING_TYPE_LITERAL)).isEqualTo("BBA3-DIR");
    }

    @Test
    public void shouldReturnSubstantiveAsNullHearingForFaceToFaceHearingBookedNotification() {
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

        assertThat(result.get(HMC_HEARING_TYPE_LITERAL)).isNotNull();
        assertThat(result.get(HMC_HEARING_TYPE_LITERAL)).isEqualTo("BBA3-SUB");
    }

    @Test
    public void shouldReturnSubstantiveHearingForFaceToFaceHearingBookedNotification() {
        LocalDateTime hearingDate = LocalDateTime.now().plusDays(1);

        Hearing hearing = createListAssistHearing(hearingDate);

        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(hearing);

        SscsCaseData response = SscsCaseData.builder()
            .hmcHearingType(HmcHearingType.SUBSTANTIVE)
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

        assertThat(result.get(HMC_HEARING_TYPE_LITERAL)).isNotNull();
        assertThat(result.get(HMC_HEARING_TYPE_LITERAL)).isEqualTo("BBA3-SUB");
    }

    @Test
    public void handleNullEventWhenPopulatingEventData() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .build();

        Map<String, Object> result = personalisation.setEventData(new HashMap<>(), response, POSTPONEMENT);

        assertEquals(new HashMap<>(), result);
    }

    @Test
    public void handleEmptyEventsWhenPopulatingEventData() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .events(Collections.emptyList())
            .build();

        Map<String, Object> result = personalisation.setEventData(new HashMap<>(), response, POSTPONEMENT);

        assertEquals(new HashMap<>(), result);
    }

    @Test
    public void shouldPopulateRegionalProcessingCenterFromCcdCaseIfItsPresent() {
        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("LIVERPOOL").address1(ADDRESS1).address2(ADDRESS2).address3(ADDRESS3).address4(ADDRESS4).city(CITY).postcode(POSTCODE).build();

        SscsCaseData response = SscsCaseData.builder().regionalProcessingCenter(rpc).build();

        Map<String, Object> result = personalisation.setEvidenceProcessingAddress(new HashMap<>(), response);

        verify(regionalProcessingCenterService, never()).getByScReferenceCode(anyString());

        assertEquals(ADDRESS1, result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(ADDRESS2, result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals(ADDRESS3, result.get(ADDRESS_LINE_LITERAL));
        assertEquals(ADDRESS4, result.get(TOWN_LITERAL));
        assertEquals(CITY, result.get(COUNTY_LITERAL));
        assertEquals(POSTCODE, result.get(POSTCODE_LITERAL));
    }

    @ParameterizedTest
    @CsvSource({
        "false, yes, scottishLine3, scottishPostcode, true",
        "false, no, line3, postcode, true",
        "false, yes, line3, postcode, false",
        "false, no, line3, postcode, false",
        "true, yes, scottishLine3, scottishPostcode, true",
        "true, no, ibcLine3, ibcPostcode, true",
        "true, yes, ibcLine3, ibcPostcode, false",
        "true, no, ibcLine3, ibcPostcode, false"
    })
    public void shouldPopulateSendEvidenceAddressToDigitalAddressWhenOnTheDigitalJourney(boolean isIbc, String isScottish, String expectedLine3, String expectedPostcode, boolean scottishPoBoxFeature) {

        SscsCaseData response = SscsCaseData.builder()
            .createdInGapsFrom(EventType.READY_TO_LIST.getCcdType())
            .isScottishCase(isScottish)
            .benefitCode(isIbc ? "093" : null)
            .build();

        evidenceAddress.setScottishPoBoxFeatureEnabled(scottishPoBoxFeature);

        Map<String, Object> result = personalisation.setEvidenceProcessingAddress(new HashMap<>(), response);

        assertEquals(evidenceAddressLine1, result.get(REGIONAL_OFFICE_NAME_LITERAL));
        assertEquals(isIbc ? evidenceIbcAddressLine2 : evidenceAddressLine2, result.get(SUPPORT_CENTRE_NAME_LITERAL));
        assertEquals(expectedLine3, result.get(ADDRESS_LINE_LITERAL));
        assertEquals(evidenceAddressTown, result.get(TOWN_LITERAL));
        assertEquals(evidenceAddressCounty, result.get(COUNTY_LITERAL));
        assertEquals(expectedPostcode, result.get(POSTCODE_LITERAL));
        assertEquals(isIbc ? PHONE_IBC : evidenceAddressTelephone, result.get(PHONE_NUMBER));
        assertEquals(evidenceAddressTelephoneWelsh, result.get(PHONE_NUMBER_WELSH));
    }

    @Test
    public void shouldNotPopulateRegionalProcessingCenterIfRpcCannotBeFound() {

        SscsCaseData response = SscsCaseData.builder().regionalProcessingCenter(null).build();

        when(regionalProcessingCenterService.getByScReferenceCode("SC/1234/5")).thenReturn(null);

        Map<String, Object> result = personalisation.setEvidenceProcessingAddress(new HashMap<>(), response);

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

        Map<String, Object> values = new HashMap<>();
        personalisation.setHearingContactDate(values, wrapper);

        assertEquals("13 June 2018", values.get(HEARING_CONTACT_DATE));
    }

    @Test
    public void shouldNotPopulateHearingContactDateFromCcdCaseIfNotPresent() {

        NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder().newSscsCaseData(SscsCaseData.builder().build()).build();

        when(hearingContactDateExtractor.extract(wrapper)).thenReturn(Optional.empty());

        Map<String, Object> values = new HashMap<>();
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
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

        OtherParty otherParty = sscsCaseData.getOtherParties().getFirst().getValue();
        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder()
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
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
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("readyToList")
            .build();

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("http://myalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals("http://myalink.com/claimingExpenses", result.get(CLAIMING_EXPENSES_LINK_LITERAL));
        assertEquals("http://myalink.com/evidenceSubmission", result.get(SUBMIT_EVIDENCE_INFO_LINK_LITERAL));
        assertEquals("http://myalink.com/evidenceSubmission", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://myalink.com/hearingInfo", result.get(HEARING_INFO_LINK_LITERAL));
    }

    @Test
    public void trackYourAppealWillReturnTyaLinkWhenCreatedInGapsFromIsNotReadyToList() {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .subscriptions(subscriptions)
            .createdInGapsFrom("validAppeal")
            .build();

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("http://tyalink.com/GLSCRR", result.get(TRACK_APPEAL_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/expenses", result.get(CLAIMING_EXPENSES_LINK_LITERAL));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_INFO_LINK_LITERAL));
        assertEquals("http://link.com/GLSCRR", result.get(SUBMIT_EVIDENCE_LINK_LITERAL));
        assertEquals("http://link.com/progress/GLSCRR/abouthearing", result.get(HEARING_INFO_LINK_LITERAL));
    }

    @ParameterizedTest
    @CsvSource({"GRANTED", "REFUSED"})
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(REVIEW_CONFIDENTIALITY_REQUEST).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals("Jeff Stelling", result.get(OTHER_PARTY_NAME));
        assertEquals(requestOutcome.getValue(), result.get(CONFIDENTIALITY_OUTCOME));
    }

    @ParameterizedTest
    @CsvSource({"GRANTED", "REFUSED"})
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(REVIEW_CONFIDENTIALITY_REQUEST).build(),
            new SubscriptionWithType(subscriptions.getJointPartySubscription(),
                JOINT_PARTY, response.getJointParty(), response.getJointParty()));

        assertEquals(name.getFullNameNoTitle(), result.get(OTHER_PARTY_NAME));
        assertEquals(requestOutcome.getValue(), result.get(CONFIDENTIALITY_OUTCOME));
    }

    @ParameterizedTest
    @CsvSource({"yes, 0300 790 6234", "no, 0300 123 1142"})
    public void setHelplineTelephoneNumber_relevantToTheCaseCountry(String isScottish, String helpLineTelephone) {
        SscsCaseData response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder().name(name).build())
                .build())
            .isScottishCase(isScottish)
            .build();

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(VALID_APPEAL_CREATED).build(),
            new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPOINTEE,
                response.getAppeal().getAppellant(), response.getAppeal().getAppellant().getAppointee()));

        assertThat(result)
            .containsEntry(NAME, "Appointee Name")
            .containsEntry(REPRESENTEE_NAME, "Harry Kane")
            .containsEntry(PARTY_TYPE, "Appellant")
            .containsEntry(ENTITY_TYPE, "Appointee");
    }


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

        Map<String, Object> result = personalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("""
                Language interpreter: Required
                
                Sign interpreter: Required
                
                Hearing loop: Required
                
                Disabled access: Required
                
                Any other arrangements: Other""",
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

        Map<String, Object> result = personalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("""
                Language interpreter: Required
                
                Sign interpreter: Required
                
                Hearing loop: Required
                
                Disabled access: Required
                
                Any other arrangements: Other""",
            result.get(HEARING_ARRANGEMENT_DETAILS_LITERAL));

        assertEquals("""
                Dehonglydd iaith arwyddion: Gofynnol
                
                Dehonglydd iaith arwyddion: Gofynnol
                
                Dolen glyw: Gofynnol
                
                Mynediad i bobl anab: Gofynnol
                
                Unrhyw drefniadau eraill: Other""",
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(APPEAL_RECEIVED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertEquals(true, result.get(IS_GRANTED));
    }

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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
                .notificationEventType(REVIEW_AND_SET_ASIDE).build(),
            new SubscriptionWithType(subscriptions.getAppellantSubscription(),
                APPOINTEE, response.getAppeal().getAppellant(),
                response.getAppeal().getAppellant().getAppointee()));

        assertThat(result)
            .doesNotContainKey(DECISION_DATE_LITERAL);
    }

    @Test
    public void whenDwpStateIsLtaGranted_setIsGrantedToTrue() {
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(LIBERTY_TO_APPLY_GRANTED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertTrue((boolean) result.get(IS_GRANTED));
    }

    @Test
    public void whenDwpStateIsLtaRefused_setIsGrantedToFalse() {
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(LIBERTY_TO_APPLY_REFUSED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertFalse((boolean) result.get(IS_GRANTED));
    }

    @Test
    public void whenDwpStateIsCorrectionIssued_setIsGrantedToTrue() {
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(LIBERTY_TO_APPLY_GRANTED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertTrue((boolean) result.get(IS_GRANTED));
    }

    @Test
    public void whenPostHearingsIsTrueAndFinalDecisionIsSet_setFinalDecision() {
        ReflectionTestUtils.setField(personalisation, "isPostHearingsEnabled", true);
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
            .notificationEventType(CORRECTION_GRANTED).build(), new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT, response.getAppeal().getAppellant(), response.getAppeal().getAppellant()));

        assertTrue((boolean) result.get(IS_GRANTED));
        assertEquals(result.get(FINAL_DECISION_DATE), LocalDate.now().format(DateTimeFormatter.ofPattern(FINAL_DECISION_DATE_FORMAT)));
    }

    @Test
    public void whenPostHearingsIsTrueAndFinalDecisionIsNotSet_dontSetFinalDecision() {
        ReflectionTestUtils.setField(personalisation, "isPostHearingsEnabled", true);
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

        Map<String, Object> result = personalisation.create(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(response)
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
