package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationSender.DATE_TIME_FORMATTER;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationSender.ZONE_ID_LONDON;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationTestRecipients;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.service.notify.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@RunWith(JUnitParamsRunner.class)
public class NotificationSenderTest {
    public static final String CCD_CASE_ID = "78980909090099";
    public static final SscsCaseData SSCS_CASE_DATA = SscsCaseData.builder().build();
    public static final String SMS_SENDER = "sms-sender";
    private NotificationSender notificationSender;
    private String templateId;
    private Map<String, Object> personalisation;
    private String reference;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private NotificationClient testNotificationClient;

    @Mock
    private NotificationTestRecipients blacklist;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Mock
    private SendSmsResponse sendSmsResponse;

    @Mock
    private LetterResponse letterResponse;

    @Mock
    private SendLetterResponse sendLetterResponse;

    @Mock
    private MarkdownTransformationService markdownTransformationService;

    @Mock
    private SaveCorrespondenceAsyncService saveCorrespondenceAsyncService;

    @Captor
    private ArgumentCaptor<Correspondence> correspondenceArgumentCaptor;

    @BeforeEach
    public void setUp() {
        templateId = "templateId";
        personalisation = Collections.emptyMap();
        reference = "reference";

        final Boolean saveCorrespondence = false;
        notificationSender = new NotificationSender(notificationClient, testNotificationClient, blacklist, markdownTransformationService, saveCorrespondenceAsyncService, saveCorrespondence);
    }

    @Test
    public void sendEmailToTestSenderIfMatchesPattern() throws NotificationClientException {
        String emailAddress = "test123@hmcts.net";
        when(testNotificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
            .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendEmailToNormalSender() throws NotificationClientException {
        String emailAddress = "random@example.com";
        when(notificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
            .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendEmailToTestSenderIfOnBlacklist() throws NotificationClientException {
        String emailAddress = "random@example.com";
        when(blacklist.getEmails()).thenReturn(Collections.singletonList(emailAddress));
        when(testNotificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
            .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendSmsToNormalSender() throws NotificationClientException {
        String phoneNumber = "07777777777";
        when(notificationClient.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER))
            .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER, NotificationEventType.APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER);
    }

    @Test
    public void sendSmsToTestSenderIfOnBlacklist() throws NotificationClientException {
        String phoneNumber = "07777777777";
        when(blacklist.getSms()).thenReturn(Collections.singletonList(phoneNumber));
        when(testNotificationClient.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER))
            .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER, NotificationEventType.APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER);
    }

    @Test
    public void sendBundledLetterToNormalSender() throws IOException, NotificationClientException {
        String postcode = "LN8 4DX";

        when(notificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));
        notificationSender.sendBundledLetter(postcode, sampleDirectionCoversheet, NotificationEventType.APPEAL_RECEIVED, "Bob Squires", CCD_CASE_ID);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    public void sendBundledLetterToSenderIfOnBlacklist() throws IOException, NotificationClientException {
        String postcode = "TS1 1ST";

        when(blacklist.getPostcodes()).thenReturn(Collections.singletonList(postcode));
        when(testNotificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));
        notificationSender.sendBundledLetter(postcode, sampleDirectionCoversheet, NotificationEventType.APPEAL_RECEIVED, "Bob Squires", CCD_CASE_ID);

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    public void sendLetterToNormalSender() throws IOException, NotificationClientException {
        String postcode = "LN8 4DX";

        when(notificationClient.sendLetter(any(), any(), any())).thenReturn(sendLetterResponse);
        when(sendLetterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        Address address = Address.builder().line1("1 Appellant Ave").town("Sometown").county("Somecounty").postcode(postcode).build();
        notificationSender.sendLetter(templateId, address, personalisation, NotificationEventType.APPEAL_RECEIVED, "Bob Squires", CCD_CASE_ID);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendLetter(any(), any(), any());
    }

    @Test
    public void sendLetterToSenderIfOnBlacklist() throws IOException, NotificationClientException {
        String postcode = "TS1 1ST";

        when(blacklist.getPostcodes()).thenReturn(Collections.singletonList(postcode));
        when(testNotificationClient.sendLetter(any(), any(), any())).thenReturn(sendLetterResponse);
        when(sendLetterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        Address address = Address.builder().line1("1 Appellant Ave").town("Sometown").county("Somecounty").postcode(postcode).build();
        notificationSender.sendLetter(templateId, address, personalisation, NotificationEventType.APPEAL_RECEIVED, "Bob Squires", CCD_CASE_ID);

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendLetter(any(), any(), any());
    }

    @Test
    public void whenAnEmailIsSentWillSaveEmailNotificationInCcd() throws NotificationClientException {

        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);

        String emailAddress = "random@example.com";
        when(notificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
            .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
        verify(markdownTransformationService).toHtml(eq(null));

        Correspondence expectedCorrespondence = Correspondence.builder().value(CorrespondenceDetails.builder()
            .to(emailAddress)
            .from("")
            .correspondenceType(CorrespondenceType.Email)
            .eventType(NotificationEventType.APPEAL_RECEIVED.getId())
            .sentOn("this field is ignored")
            .build()).build();
        verify(saveCorrespondenceAsyncService).saveEmailOrSms(argThat((Correspondence arg) -> EqualsBuilder.reflectionEquals(arg.getValue(), expectedCorrespondence.getValue(), "sentOn")), eq(SSCS_CASE_DATA));
    }

    @Test
    public void whenAnEmailIsSentAndEmailResponseIsNullThenWillNotSaveEmailNotificationInCcd() throws NotificationClientException {

        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);

        String emailAddress = "random@example.com";
        when(notificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
            .thenReturn(null);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
        verifyNoMoreInteractions(markdownTransformationService);
        verifyNoMoreInteractions(saveCorrespondenceAsyncService);
    }

    @Test
    public void whenAnSmsIsSentWillSaveSmsNotificationInCcd() throws NotificationClientException {

        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);

        String smsNumber = "07999999000";
        when(notificationClient.sendSms(templateId, smsNumber, personalisation, reference, "Sender"))
            .thenReturn(sendSmsResponse);

        notificationSender.sendSms(templateId, smsNumber, personalisation, reference, "Sender", NotificationEventType.APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendSms(templateId, smsNumber, personalisation, reference, "Sender");
        verify(markdownTransformationService).toHtml(eq(null));

        Correspondence expectedCorrespondence = Correspondence.builder().value(CorrespondenceDetails.builder()
            .to(smsNumber)
            .from("")
            .subject("SMS correspondence")
            .correspondenceType(CorrespondenceType.Sms)
            .eventType(NotificationEventType.APPEAL_RECEIVED.getId())
            .sentOn("this field is ignored")
            .build()).build();
        verify(saveCorrespondenceAsyncService).saveEmailOrSms(argThat((Correspondence arg) -> EqualsBuilder.reflectionEquals(arg.getValue(), expectedCorrespondence.getValue(), "sentOn")), eq(SSCS_CASE_DATA));
    }

    @Test
    public void whenAnSmsIsSentAndSmsResponseIsNullThenWillNotSaveSmsNotificationInCcd() throws NotificationClientException {

        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);

        String smsNumber = "07999999000";
        when(notificationClient.sendSms(templateId, smsNumber, personalisation, reference, "Sender"))
            .thenReturn(null);

        notificationSender.sendSms(templateId, smsNumber, personalisation, reference, "Sender", NotificationEventType.APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendSms(templateId, smsNumber, personalisation, reference, "Sender");
        verifyNoMoreInteractions(markdownTransformationService);
        verifyNoMoreInteractions(saveCorrespondenceAsyncService);
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"null", "NotificationClientException"})
    public void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendEmail(String error) {
        assertThrows(NotificationClientException.class, () -> {
            String emailAddress = "test123@hmcts.net";
            Exception exception = (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
            doThrow(exception).when(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);

            notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED, SSCS_CASE_DATA);
        });
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"null", "NotificationClientException"})
    public void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendSms(String error) {
        assertThrows(NotificationClientException.class, () -> {
            String smsNumber = "07999999000";
            Exception exception = (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
            doThrow(exception).when(notificationClient).sendSms(templateId, smsNumber, personalisation, reference, "Sender");

            notificationSender.sendSms(templateId, smsNumber, personalisation, reference, "Sender", NotificationEventType.APPEAL_RECEIVED, SSCS_CASE_DATA);
        });
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"null", "NotificationClientException"})
    public void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendLetter(String error) {
        assertThrows(NotificationClientException.class, () -> {
            String postcode = "TS1 1ST";
            Address address = Address.builder().line1("1 Appellant Ave").town("Sometown").county("Somecounty").postcode(postcode).build();
            Exception exception = (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
            doThrow(exception).when(notificationClient).sendLetter(any(), any(), any());

            notificationSender.sendLetter(templateId, address, personalisation, NotificationEventType.APPEAL_RECEIVED, "Bob Squires", CCD_CASE_ID);
        });
    }

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"null", "NotificationClientException"})
    public void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendBundledLetter(String error) {
        assertThrows(NotificationClientException.class, () -> {
            Exception exception = (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
            doThrow(exception).when(notificationClient).sendPrecompiledLetterWithInputStream(any(), any());

            String postcode = "LN8 4DX";
            byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/direction-notice-coversheet-sample.pdf"));
            notificationSender.sendBundledLetter(postcode, sampleDirectionCoversheet, NotificationEventType.APPEAL_RECEIVED, "Bob Squires", CCD_CASE_ID);
        });
    }

    @Test
    public void saveLetterCorrespondence() {
        byte[] sampleLetter = "Letter".getBytes();
        notificationSender.saveLettersToReasonableAdjustment(sampleLetter, NotificationEventType.APPEAL_RECEIVED, "Bob Squires", CCD_CASE_ID, SubscriptionType.APPELLANT);

        verify(saveCorrespondenceAsyncService).saveLetter(eq(sampleLetter), correspondenceArgumentCaptor.capture(), eq(CCD_CASE_ID), eq(SubscriptionType.APPELLANT));
        Correspondence correspondence = correspondenceArgumentCaptor.getValue();
        assertNotNull(correspondence);
        assertEquals(CorrespondenceType.Letter, correspondence.getValue().getCorrespondenceType());
        assertEquals("Bob Squires", correspondence.getValue().getTo());
        assertEquals(NotificationEventType.APPEAL_RECEIVED.getId(), correspondence.getValue().getEventType());
        assertEquals(ReasonableAdjustmentStatus.REQUIRED, correspondence.getValue().getReasonableAdjustmentStatus());
    }

    @Test
    public void saveLetterCorrespondence_emptyLetter() throws NotificationClientException {
        notificationSender.saveLettersToReasonableAdjustment(null, NotificationEventType.APPEAL_RECEIVED, "Bob Squires", CCD_CASE_ID, SubscriptionType.APPELLANT);
        verifyNoInteractions(saveCorrespondenceAsyncService);
    }

    @Test
    public void recoverWillConsumeThrowable() {
        notificationSender.getBackendResponseFallback(new NotificationClientException("400 BadRequestError"));
    }

    @Test
    public void formatter_returnsCorrectYearAtEndOfYear() {
        final String dateFormat = LocalDateTime.of(2020, 12, 31, 12, 0, 0).atZone(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER);
        assertThat(dateFormat, is("31 Dec 2020 12:00"));
    }

    @Test
    public void formatter_returnsCorrectYearAtStartOfYear() {
        final String dateFormat = LocalDateTime.of(2021, 1, 1, 12, 0, 0).atZone(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER);
        assertThat(dateFormat, is("1 Jan 2021 12:00"));
    }
}
