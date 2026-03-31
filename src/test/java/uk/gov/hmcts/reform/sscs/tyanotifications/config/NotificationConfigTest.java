package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType.ORAL;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.VALID_APPEAL_CREATED;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.env.Environment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Link;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;

class NotificationConfigTest {

    private final Environment env = mock(Environment.class);

    @ParameterizedTest
    @CsvSource({
        "emailTemplateName, notification.english.emailTemplateName.emailId, emailTemplateId, smsTemplateName, notification.english.smsTemplateName.smsId, smsTemplateId, letterTemplateName, notification.english.letterTemplateName.letterId, letterTemplateId, notification.english.letterTemplateName.docmosisId, docmosisTemplateId, docmosisTemplateId, validAppeal",
        "emailTemplateName, notification.english.oral.emailTemplateName.emailId, onlineEmailTemplateId, smsTemplateName, notification.english.oral.smsTemplateName.smsId, onlineSmsTemplateId, appealReceived, notification.english.oral.appealReceived.letterId, onlineLetterTemplateId, notification.english.oral.appealReceived.docmosisId, docmosisTemplateId, docmosisTemplateId, readyToList",
        "emailTemplateName, notification.english.oral.emailTemplateName.emailId, onlineEmailTemplateId, smsTemplateName, notification.english.oral.smsTemplateName.smsId, onlineSmsTemplateId, appealReceived, notification.english.oral.appealReceived.letterId, onlineLetterTemplateId, notification.english.oral.appealReceived.docmosisId, docmosisTemplateId, , validAppeal"
    })
    void getDefaultTemplate(String emailTemplateName, String emailTemplateKey, String emailTemplateId,
        String smsTemplateName, String smsTemplateKey, String smsTemplateId,
        String letterTemplateName, String letterTemplateKey, String letterTemplateId,
        String docmosisTemplateKey, String docmosisTemplateId, String expectedDocmosisTemplateId, String createdInGapsFrom) {

        when(env.getProperty(emailTemplateKey)).thenReturn(emailTemplateId);
        when(env.containsProperty(emailTemplateKey)).thenReturn(true);
        when(env.getProperty(smsTemplateKey)).thenReturn(smsTemplateId);
        when(env.containsProperty(smsTemplateKey)).thenReturn(true);
        when(env.getProperty(letterTemplateKey)).thenReturn(letterTemplateId);
        when(env.getProperty(docmosisTemplateKey)).thenReturn(docmosisTemplateId);
        when(env.containsProperty(letterTemplateKey)).thenReturn(true);

        CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(SscsCaseData.builder().appeal(Appeal.builder().hearingType(ORAL.name()).build()).build())
            .build());

        Template template = new NotificationConfig(env).getTemplate(emailTemplateName, smsTemplateName, letterTemplateName,
            letterTemplateName, Benefit.PIP, wrapper, createdInGapsFrom);

        assertThat(template.getEmailTemplateId()).isEqualTo(emailTemplateId);
        assertThat(template.getSmsTemplateId()).hasSize(1);
        assertThat(template.getSmsTemplateId().getFirst()).isEqualTo(smsTemplateId);
        assertThat(template.getLetterTemplateId()).isEqualTo(letterTemplateId);
        assertThat(template.getDocmosisTemplateId()).isEqualTo(
            expectedDocmosisTemplateId != null && expectedDocmosisTemplateId.isEmpty() ? null : expectedDocmosisTemplateId);
    }

    @ParameterizedTest
    @CsvSource({
        "emailTemplateName, notification.welsh.emailTemplateName.emailId, emailTemplateId, smsTemplateName, notification.welsh.smsTemplateName.smsId, smsTemplateId, letterTemplateName, notification.welsh.letterTemplateName.letterId, letterTemplateId, notification.welsh.letterTemplateName.docmosisId, docmosisTemplateId, docmosisTemplateId, validAppeal",
        "emailTemplateName, notification.welsh.oral.emailTemplateName.emailId, onlineEmailTemplateId, smsTemplateName, notification.welsh.oral.smsTemplateName.smsId, onlineSmsTemplateId, appealReceived, notification.welsh.oral.appealReceived.letterId, onlineLetterTemplateId, notification.welsh.oral.appealReceived.docmosisId, docmosisTemplateId, docmosisTemplateId, readyToList",
        "emailTemplateName, notification.welsh.oral.emailTemplateName.emailId, onlineEmailTemplateId, smsTemplateName, notification.welsh.oral.smsTemplateName.smsId, onlineSmsTemplateId, appealReceived, notification.welsh.oral.appealReceived.letterId, onlineLetterTemplateId, notification.welsh.oral.appealReceived.docmosisId, docmosisTemplateId, , validAppeal"
    })
    void getWelshTemplate(String emailTemplateName, String emailTemplateKey, String emailTemplateId,
        String smsTemplateName, String smsTemplateKey, String smsTemplateId,
        String letterTemplateName, String letterTemplateKey, String letterTemplateId,
        String docmosisTemplateKey, String docmosisTemplateId, String expectedDocmosisTemplateId, String createdInGapsFrom) {
        String englishSmsTemplateId = "smsEnglishTemplateId";
        when(env.getProperty(emailTemplateKey)).thenReturn(emailTemplateId);
        when(env.containsProperty(emailTemplateKey)).thenReturn(true);
        when(env.getProperty(smsTemplateKey)).thenReturn(smsTemplateId);
        when(env.getProperty("notification.english.oral.smsTemplateName.smsId")).thenReturn(englishSmsTemplateId);
        when(env.containsProperty(smsTemplateKey)).thenReturn(true);
        when(env.getProperty(letterTemplateKey)).thenReturn(letterTemplateId);
        when(env.getProperty(docmosisTemplateKey)).thenReturn(docmosisTemplateId);
        when(env.containsProperty(letterTemplateKey)).thenReturn(true);

        CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(SscsCaseData
                .builder()
                .languagePreferenceWelsh("Yes")
                .appeal(Appeal.builder().hearingType(ORAL.name()).build())
                .build())
            .build());

        Template template = new NotificationConfig(env).getTemplate(emailTemplateName, smsTemplateName, letterTemplateName,
            letterTemplateName, Benefit.PIP, wrapper, createdInGapsFrom);

        assertThat(template.getEmailTemplateId()).isEqualTo(emailTemplateId);
        assertThat(template.getSmsTemplateId()).hasSize(2);
        assertThat(template.getSmsTemplateId().get(0)).isEqualTo(smsTemplateId);
        assertThat(template.getSmsTemplateId().get(1)).isEqualTo(englishSmsTemplateId);
        assertThat(template.getLetterTemplateId()).isEqualTo(letterTemplateId);
        assertThat(template.getDocmosisTemplateId()).isEqualTo(
            expectedDocmosisTemplateId != null && expectedDocmosisTemplateId.isEmpty() ? null : expectedDocmosisTemplateId);
    }

    @Test
    void switchWelshTemplateToEnglishTemplateWhenSwitchFlagSet() {

        String letterTemplateKey = "notification.welsh.letterTemplateName.letterId";
        String letterTemplateId = "letterTemplateId";
        String switchedDocmosisTemplateKey = "notification.english.letterTemplateName.docmosisId";
        String docmosisTemplateId = "docmosisTemplateId";

        when(env.getProperty(letterTemplateKey)).thenReturn(letterTemplateId);
        when(env.getProperty(switchedDocmosisTemplateKey)).thenReturn(docmosisTemplateId);
        when(env.containsProperty(letterTemplateKey)).thenReturn(true);

        CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(SscsCaseData
                .builder()
                .languagePreferenceWelsh("Yes")
                .appeal(Appeal.builder().hearingType(ORAL.name()).build())
                .build())
            .build());
        wrapper.setSwitchLanguageType(true);

        Template template = new NotificationConfig(env).getTemplate("emailTemplateName", "smsTemplateName", "letterTemplateName",
            "letterTemplateName", Benefit.PIP, wrapper, "validAppeal");

        assertThat(template.getDocmosisTemplateId()).isEqualTo(docmosisTemplateId);
    }

    @Test
    void switchEnglishTemplateToWelshTemplateWhenSwitchFlagSet() {
        when(env.getProperty("notification.welsh.oral.letterTemplateName.letterId")).thenReturn("welshLetterTemplateId");
        when(env.getProperty("notification.welsh.oral.letterTemplateName.docmosisId")).thenReturn("welshDocmosisId");

        final CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(SscsCaseData.builder()
                .appeal(Appeal.builder().hearingType(ORAL.name()).build())
                .build())
            .build());
        wrapper.setSwitchLanguageType(true);

        final Template template = new NotificationConfig(env).getTemplate("emailTemplateName", "smsTemplateName", "letterTemplateName",
            "letterTemplateName", Benefit.PIP, wrapper, "validAppeal");

        assertThat(template.getLetterTemplateId()).isEqualTo("welshLetterTemplateId");
        assertThat(template.getDocmosisTemplateId()).isEqualTo("welshDocmosisId");
    }

    @Test
    void getTemplateUsesHearingRouteInLookup() {
        when(env.getProperty("notification.english.listAssist.oral.emailTemplateName.emailId")).thenReturn("routedEmailId");
        when(env.getProperty("notification.english.listAssist.oral.smsTemplateName.smsId")).thenReturn("routedSmsId");
        when(env.getProperty("notification.english.listAssist.oral.letterTemplateName.letterId")).thenReturn("routedLetterId");
        when(env.getProperty("notification.english.listAssist.oral.letterTemplateName.docmosisId")).thenReturn("routedDocmosisId");

        final CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(SscsCaseData.builder()
                .schedulingAndListingFields(SchedulingAndListingFields.builder()
                    .hearingRoute(HearingRoute.LIST_ASSIST)
                    .build())
                .appeal(Appeal.builder().hearingType(ORAL.name()).build())
                .build())
            .build());

        final Template template = new NotificationConfig(env).getTemplate("emailTemplateName", "smsTemplateName", "letterTemplateName",
            "letterTemplateName", Benefit.PIP, wrapper, "readyToList");

        assertThat(template.getEmailTemplateId()).isEqualTo("routedEmailId");
        assertThat(template.getSmsTemplateId()).hasSize(1);
        assertThat(template.getSmsTemplateId().getFirst()).isEqualTo("routedSmsId");
        assertThat(template.getLetterTemplateId()).isEqualTo("routedLetterId");
        assertThat(template.getDocmosisTemplateId()).isEqualTo("routedDocmosisId");
    }

    @Test
    void getTemplateReturnsNullDocmosisWhenNoDocmosisTemplateConfigured() {
        final CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(SscsCaseData.builder().appeal(Appeal.builder().hearingType(ORAL.name()).build()).build())
            .build());

        final Template template = new NotificationConfig(env).getTemplate("emailTemplateName", "smsTemplateName",
            "letterTemplateName", "letterTemplateName", Benefit.PIP, wrapper, "readyToList");

        assertThat(template.getDocmosisTemplateId()).isNull();
    }

    @Test
    void getTemplateReturnsEmptySmsListWhenNoSmsTemplateConfigured() {
        final CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(SscsCaseData.builder().appeal(Appeal.builder().hearingType(ORAL.name()).build()).build())
            .build());

        final Template template = new NotificationConfig(env).getTemplate("emailTemplateName", "smsTemplateName",
            "letterTemplateName", "letterTemplateName", Benefit.PIP, wrapper, "readyToList");

        assertThat(template.getSmsTemplateId()).isEmpty();
    }

    @Test
    void getTemplateReturnsEmptySmsSenderForNullBenefit() {
        final CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper
            .builder()
            .newSscsCaseData(SscsCaseData.builder().appeal(Appeal.builder().hearingType(ORAL.name()).build()).build())
            .build());

        final Template template = new NotificationConfig(env).getTemplate("emailTemplateName", "smsTemplateName",
            "letterTemplateName", "letterTemplateName", null, wrapper, "readyToList");

        assertThat(template.getSmsSenderTemplateId()).isEqualTo("");
    }

    @Test
    void getTemplateSetsNullDocmosisWhenValidAppealCreatedAndCmFlagDisabled() {
        when(env.getProperty("notification.english.oral.letterTemplateName.docmosisId")).thenReturn("docmosisId");

        final CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper
            .builder()
            .notificationEventType(VALID_APPEAL_CREATED)
            .newSscsCaseData(SscsCaseData.builder().appeal(Appeal.builder().hearingType(ORAL.name()).build()).build())
            .build());

        final Template template = new NotificationConfig(env).getTemplate("emailTemplateName", "smsTemplateName",
            "letterTemplateName", "letterTemplateName", Benefit.PIP, wrapper, "readyToList");

        assertThat(template.getDocmosisTemplateId()).isNull();
    }

    @Test
    void getTemplateSetsNullDocmosisWhenValidAppealCreatedAndCmFlagEnabledNonChildSupport() {
        when(env.getProperty("notification.english.oral.letterTemplateName.docmosisId")).thenReturn("docmosisId");

        final CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper
            .builder()
            .notificationEventType(VALID_APPEAL_CREATED)
            .newSscsCaseData(SscsCaseData.builder().appeal(Appeal.builder().hearingType(ORAL.name()).build()).build())
            .build());

        final NotificationConfig config = new NotificationConfig(env);
        setField(config, "cmOtherPartyConfidentialityEnabled", true);

        final Template template = config.getTemplate("emailTemplateName", "smsTemplateName",
            "letterTemplateName", "letterTemplateName", Benefit.PIP, wrapper, "readyToList");

        assertThat(template.getDocmosisTemplateId()).isNull();
    }

    @Test
    void getTemplateKeepsDocmosisWhenValidAppealCreatedAndCmFlagEnabledChildSupport() {
        when(env.getProperty("notification.english.oral.letterTemplateName.docmosisId")).thenReturn("docmosisId");

        final CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper
            .builder()
            .notificationEventType(VALID_APPEAL_CREATED)
            .newSscsCaseData(SscsCaseData.builder()
                .appeal(Appeal.builder()
                    .hearingType(ORAL.name())
                    .benefitType(BenefitType.builder().code("childSupport").build())
                    .build())
                .build())
            .build());

        final NotificationConfig config = new NotificationConfig(env);
        setField(config, "cmOtherPartyConfidentialityEnabled", true);

        final Template template = config.getTemplate("emailTemplateName", "smsTemplateName",
            "letterTemplateName", "letterTemplateName", Benefit.PIP, wrapper, "readyToList");

        assertThat(template.getDocmosisTemplateId()).isEqualTo("docmosisId");
    }

    @Test
    void getLinksAndStrings() {
        final NotificationConfig config = new NotificationConfig(env);
        setField(config, "manageEmailsLink", "http://manage");
        setField(config, "trackAppealLink", "http://track");
        setField(config, "evidenceSubmissionInfoLink", "http://evidence");
        setField(config, "claimingExpensesLink", "http://expenses");
        setField(config, "hearingInfoLink", "http://hearing");
        setField(config, "onlineHearingLink", "http://online");
        setField(config, "myaLink", "http://mya");
        setField(config, "myaEvidenceSubmissionInfoLink", "http://mya-evidence");
        setField(config, "myaHearingInfoLink", "http://mya-hearing");
        setField(config, "myaClaimingExpensesLink", "http://mya-expenses");
        setField(config, "helplineTelephone", "0300 123 1142");
        setField(config, "helplineTelephoneIbc", "0300 123 1143");
        setField(config, "helplineTelephoneScotland", "0300 790 6234");

        assertThat(config.getManageEmailsLink()).isEqualTo(Link.builder().linkUrl("http://manage").build());
        assertThat(config.getTrackAppealLink()).isEqualTo(Link.builder().linkUrl("http://track").build());
        assertThat(config.getEvidenceSubmissionInfoLink()).isEqualTo(Link.builder().linkUrl("http://evidence").build());
        assertThat(config.getClaimingExpensesLink()).isEqualTo(Link.builder().linkUrl("http://expenses").build());
        assertThat(config.getHearingInfoLink()).isEqualTo(Link.builder().linkUrl("http://hearing").build());
        assertThat(config.getOnlineHearingLinkWithEmail()).isEqualTo(Link.builder().linkUrl("http://online?email={email}").build());
        assertThat(config.getOnlineHearingLink()).isEqualTo("http://online");
        assertThat(config.getMyaLink()).isEqualTo(Link.builder().linkUrl("http://mya").build());
        assertThat(config.getMyaEvidenceSubmissionInfoLink()).isEqualTo(Link.builder().linkUrl("http://mya-evidence").build());
        assertThat(config.getMyaHearingInfoLink()).isEqualTo(Link.builder().linkUrl("http://mya-hearing").build());
        assertThat(config.getMyaClaimingExpensesLink()).isEqualTo(Link.builder().linkUrl("http://mya-expenses").build());
        assertThat(config.getHelplineTelephone()).isEqualTo("0300 123 1142");
        assertThat(config.getHelplineTelephoneIbc()).isEqualTo("0300 123 1143");
        assertThat(config.getHelplineTelephoneScotland()).isEqualTo("0300 790 6234");
    }
}
