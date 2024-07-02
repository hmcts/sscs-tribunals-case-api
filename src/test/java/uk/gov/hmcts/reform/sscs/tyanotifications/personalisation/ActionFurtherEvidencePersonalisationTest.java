package uk.gov.hmcts.reform.sscs.tyanotifications.personalisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.APPELLANT;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationConfiguration;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.properties.EvidenceProperties;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.MessageAuthenticationServiceImpl;

@ExtendWith(MockitoExtension.class)
class ActionFurtherEvidencePersonalisationTest {

    @Mock
    private NotificationConfig config;

    @Mock
    private MessageAuthenticationServiceImpl macService;

    @Mock
    private EvidenceProperties evidenceProperties;

    @Mock
    private EvidenceProperties.EvidenceAddress evidencePropertiesAddress;

    @Spy
    private PersonalisationConfiguration personalisationConfiguration;

    @Mock
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Mock
    private NotificationDateConverterUtil notificationDateConverterUtil;

    @InjectMocks
    ActionFurtherEvidencePersonalisation actionFurtherEvidencePersonalisation;

    NotificationSscsCaseDataWrapper responseWrapper;

    SubscriptionWithType subscriptionWithType;

    Subscriptions subscriptions;

    @BeforeEach
    void setUp() {
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://manageemails.com/mac").build());
        when(config.getTrackAppealLink()).thenReturn(Link.builder().linkUrl("http://tyalink.com/appeal_id").build());
        when(config.getEvidenceSubmissionInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/appeal_id").build());
        when(config.getManageEmailsLink()).thenReturn(Link.builder().linkUrl("http://link.com/manage-email-notifications/mac").build());
        when(config.getClaimingExpensesLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/expenses").build());
        when(config.getHearingInfoLink()).thenReturn(Link.builder().linkUrl("http://link.com/progress/appeal_id/abouthearing").build());
        when(config.getOnlineHearingLinkWithEmail()).thenReturn(Link.builder().linkUrl("http://link.com/onlineHearing?email={email}").build());
        when(macService.generateToken("GLSCRR", PIP.name())).thenReturn("ZYX");
        when(evidenceProperties.getAddress()).thenReturn(evidencePropertiesAddress);
        when(hearingContactDateExtractor.extract(any())).thenReturn(Optional.empty());
        when(notificationDateConverterUtil.toEmailDate(any(LocalDate.class))).thenReturn("1 January 2018");

        Map<String, String> englishMap = new HashMap<>();
        Map<String, String> welshMap = new HashMap<>();

        Map<LanguagePreference, Map<String, String>> personalisations = new HashMap<>();
        personalisations.put(LanguagePreference.ENGLISH, englishMap);
        personalisations.put(LanguagePreference.WELSH, welshMap);

        personalisationConfiguration.setPersonalisation(personalisations);

        SscsCaseData caseData = buildCaseData();

        DynamicList sender = new DynamicList(new DynamicListItem("appellant", "Appellant (or Appointee)"), new ArrayList<>());
        caseData.setOriginalSender(sender);

        responseWrapper = NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(caseData)
            .build();

        Subscription subscription = Subscription.builder()
            .tya("GLSCRR")
            .email("test@email.com")
            .mobile("07983495065")
            .subscribeEmail("Yes")
            .subscribeSms("No")
            .build();

        subscriptions = Subscriptions.builder().appellantSubscription(subscription).jointPartySubscription(subscription).build();

        subscriptionWithType = new SubscriptionWithType(subscriptions.getAppellantSubscription(), APPELLANT,
            caseData.getAppeal().getAppellant(), caseData.getAppeal().getAppellant());
    }

    @ParameterizedTest
    @MethodSource("furtherEvidenceVariations")
    void whenEventIsActionFurtherEvidence_shouldProvideCorrectPersionalisationOptions(NotificationEventType notificationEventType, PostHearingRequestType postHearingRequestType,
                                                                                      DocumentType documentType) {
        PostHearing postHearing = PostHearing.builder().requestType(postHearingRequestType).build();
        responseWrapper.getNewSscsCaseData().setPostHearing(postHearing);
        responseWrapper.setNotificationEventType(notificationEventType);

        var result = actionFurtherEvidencePersonalisation.create(responseWrapper, subscriptionWithType);

        var senderName = responseWrapper.getNewSscsCaseData().getAppeal().getAppellant().getName().getFullNameNoTitle();

        assertThat(documentType.getLabel()).isEqualTo(result.get(DOCUMENT_TYPE_NAME));
        assertThat(senderName).isEqualTo(result.get(SENDER_NAME));
        assertThat(postHearingRequestType).isEqualTo(result.get(FURTHER_EVIDENCE_ACTION));
    }

    private static Stream<Arguments> furtherEvidenceVariations() {
        return Stream.of(
            arguments(NotificationEventType.CORRECTION_REQUEST, PostHearingRequestType.CORRECTION, DocumentType.CORRECTION_APPLICATION),
            arguments(NotificationEventType.LIBERTY_TO_APPLY_REQUEST, PostHearingRequestType.LIBERTY_TO_APPLY, DocumentType.LIBERTY_TO_APPLY_APPLICATION),
            arguments(NotificationEventType.SET_ASIDE_REQUEST, PostHearingRequestType.SET_ASIDE, DocumentType.SET_ASIDE_APPLICATION),
            arguments(NotificationEventType.STATEMENT_OF_REASONS_REQUEST, PostHearingRequestType.STATEMENT_OF_REASONS, DocumentType.STATEMENT_OF_REASONS_APPLICATION),
            arguments(NotificationEventType.PERMISSION_TO_APPEAL_REQUEST, PostHearingRequestType.PERMISSION_TO_APPEAL, DocumentType.PERMISSION_TO_APPEAL_APPLICATION)
        );
    }
}
