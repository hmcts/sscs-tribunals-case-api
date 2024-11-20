package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType.ORAL;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.core.env.Environment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class NotificationConfigTest {

    private final Environment env = mock(Environment.class);

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({
        "emailTemplateName, notification.english.emailTemplateName.emailId, emailTemplateId, smsTemplateName, notification.english.smsTemplateName.smsId, smsTemplateId, letterTemplateName, notification.english.letterTemplateName.letterId, letterTemplateId, notification.english.letterTemplateName.docmosisId, docmosisTemplateId, docmosisTemplateId, validAppeal",
        "emailTemplateName, notification.english.oral.emailTemplateName.emailId, onlineEmailTemplateId, smsTemplateName, notification.english.oral.smsTemplateName.smsId, onlineSmsTemplateId, appealReceived, notification.english.oral.appealReceived.letterId, onlineLetterTemplateId, notification.english.oral.appealReceived.docmosisId, docmosisTemplateId, docmosisTemplateId, readyToList",
        "emailTemplateName, notification.english.oral.emailTemplateName.emailId, onlineEmailTemplateId, smsTemplateName, notification.english.oral.smsTemplateName.smsId, onlineSmsTemplateId, appealReceived, notification.english.oral.appealReceived.letterId, onlineLetterTemplateId, notification.english.oral.appealReceived.docmosisId, docmosisTemplateId, null, validAppeal"
    })
    public void getDefaultTemplate(String emailTemplateName, String emailTemplateKey, String emailTemplateId,
                                   String smsTemplateName, String smsTemplateKey, String smsTemplateId,
                                   String letterTemplateName, String letterTemplateKey, String letterTemplateId,
                                   String docmosisTemplateKey, String docmosisTemplateId, @Nullable String expectedDocmosisTemplateId, String createdInGapsFrom) {

        when(env.getProperty(emailTemplateKey)).thenReturn(emailTemplateId);
        when(env.containsProperty(emailTemplateKey)).thenReturn(true);
        when(env.getProperty(smsTemplateKey)).thenReturn(smsTemplateId);
        when(env.containsProperty(smsTemplateKey)).thenReturn(true);
        when(env.getProperty(letterTemplateKey)).thenReturn(letterTemplateId);
        when(env.getProperty(docmosisTemplateKey)).thenReturn(docmosisTemplateId);
        when(env.containsProperty(letterTemplateKey)).thenReturn(true);

        CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(SscsCaseData.builder().appeal(Appeal.builder().hearingType(ORAL.name()).build()).build()).build());

        Template template = new NotificationConfig(env).getTemplate(emailTemplateName, smsTemplateName, letterTemplateName, letterTemplateName, Benefit.PIP, wrapper, createdInGapsFrom);

        assertThat(template.getEmailTemplateId(), is(emailTemplateId));
        assertThat(template.getSmsTemplateId().size(), is(1));
        assertThat(template.getSmsTemplateId().get(0), is(smsTemplateId));
        assertThat(template.getLetterTemplateId(), is(letterTemplateId));
        assertThat(template.getDocmosisTemplateId(), is(expectedDocmosisTemplateId));
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({
        "emailTemplateName, notification.welsh.emailTemplateName.emailId, emailTemplateId, smsTemplateName, notification.welsh.smsTemplateName.smsId, smsTemplateId, letterTemplateName, notification.welsh.letterTemplateName.letterId, letterTemplateId, notification.welsh.letterTemplateName.docmosisId, docmosisTemplateId, docmosisTemplateId, validAppeal",
        "emailTemplateName, notification.welsh.oral.emailTemplateName.emailId, onlineEmailTemplateId, smsTemplateName, notification.welsh.oral.smsTemplateName.smsId, onlineSmsTemplateId, appealReceived, notification.welsh.oral.appealReceived.letterId, onlineLetterTemplateId, notification.welsh.oral.appealReceived.docmosisId, docmosisTemplateId, docmosisTemplateId, readyToList",
        "emailTemplateName, notification.welsh.oral.emailTemplateName.emailId, onlineEmailTemplateId, smsTemplateName, notification.welsh.oral.smsTemplateName.smsId, onlineSmsTemplateId, appealReceived, notification.welsh.oral.appealReceived.letterId, onlineLetterTemplateId, notification.welsh.oral.appealReceived.docmosisId, docmosisTemplateId, null, validAppeal"
    })
    public void getWelshTemplate(String emailTemplateName, String emailTemplateKey, String emailTemplateId,
                                 String smsTemplateName, String smsTemplateKey, String smsTemplateId,
                                 String letterTemplateName, String letterTemplateKey, String letterTemplateId,
                                 String docmosisTemplateKey, String docmosisTemplateId, @Nullable String expectedDocmosisTemplateId, String createdInGapsFrom) {
        String englishSmsTemplateId = "smsEnglishTemplateId";
        when(env.getProperty(emailTemplateKey)).thenReturn(emailTemplateId);
        when(env.containsProperty(emailTemplateKey)).thenReturn(true);
        when(env.getProperty(smsTemplateKey)).thenReturn(smsTemplateId);
        when(env.getProperty("notification.english.oral.smsTemplateName.smsId")).thenReturn(englishSmsTemplateId);
        when(env.containsProperty(smsTemplateKey)).thenReturn(true);
        when(env.getProperty(letterTemplateKey)).thenReturn(letterTemplateId);
        when(env.getProperty(docmosisTemplateKey)).thenReturn(docmosisTemplateId);
        when(env.containsProperty(letterTemplateKey)).thenReturn(true);

        CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(SscsCaseData.builder().languagePreferenceWelsh("Yes").appeal(Appeal.builder().hearingType(ORAL.name()).build()).build()).build());

        Template template = new NotificationConfig(env).getTemplate(emailTemplateName, smsTemplateName, letterTemplateName, letterTemplateName, Benefit.PIP, wrapper, createdInGapsFrom);

        assertThat(template.getEmailTemplateId(), is(emailTemplateId));
        assertThat(template.getSmsTemplateId().size(), is(2));
        assertThat(template.getSmsTemplateId().get(0), is(smsTemplateId));
        assertThat(template.getSmsTemplateId().get(1), is(englishSmsTemplateId));
        assertThat(template.getLetterTemplateId(), is(letterTemplateId));
        assertThat(template.getDocmosisTemplateId(), is(expectedDocmosisTemplateId));
    }

    @Test
    public void switchWelshTemplateToEnglishTemplateWhenSwitchFlagSet() {

        String letterTemplateKey = "notification.welsh.letterTemplateName.letterId";
        String letterTemplateId = "letterTemplateId";
        String switchedDocmosisTemplateKey = "notification.english.letterTemplateName.docmosisId";
        String docmosisTemplateId = "docmosisTemplateId";

        when(env.getProperty(letterTemplateKey)).thenReturn(letterTemplateId);
        when(env.getProperty(switchedDocmosisTemplateKey)).thenReturn(docmosisTemplateId);
        when(env.containsProperty(letterTemplateKey)).thenReturn(true);

        CcdNotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder().newSscsCaseData(SscsCaseData.builder().languagePreferenceWelsh("Yes").appeal(Appeal.builder().hearingType(ORAL.name()).build()).build()).build());
        wrapper.setSwitchLanguageType(true);

        Template template = new NotificationConfig(env).getTemplate("emailTemplateName", "smsTemplateName", "letterTemplateName", "letterTemplateName", Benefit.PIP, wrapper, "validAppeal");

        assertThat(template.getDocmosisTemplateId(), is(docmosisTemplateId));
    }
}
