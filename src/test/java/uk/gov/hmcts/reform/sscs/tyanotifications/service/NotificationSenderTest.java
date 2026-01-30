package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.pdfbox.io.IOUtils.toByteArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationSender.DATE_TIME_FORMATTER;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationSender.ZONE_ID_LONDON;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReasonableAdjustmentStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationTestRecipients;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendLetterResponse;
import uk.gov.service.notify.SendSmsResponse;

@ExtendWith(MockitoExtension.class)
public class NotificationSenderTest {

    private static final String SAMPLE_COVERSHEET = "pdf/direction-notice-coversheet-sample.pdf";
    private static final String LARGE_PDF = "pdf/eleven-page-test-document.pdf";
    public static final String CASE_D = "78980909090099";
    public static final SscsCaseData SSCS_CASE_DATA = SscsCaseData.builder().build();
    public static final String SMS_SENDER = "sms-sender";
    private final String templateId = "templateId";
    private final Map<String, Object> personalisation = Collections.emptyMap();
    private final String reference = "reference";

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
    private BulkPrintService bulkPrintService;
    @Mock
    private MarkdownTransformationService markdownTransformationService;
    @Mock
    private SaveCorrespondenceAsyncService saveCorrespondenceAsyncService;
    @Captor
    private ArgumentCaptor<Correspondence> correspondenceArgumentCaptor;

    private NotificationSender notificationSender;

    private final NotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .notificationEventType(APPEAL_RECEIVED)
            .newSscsCaseData(SscsCaseData.builder().ccdCaseId(CASE_D)
                    .appeal(Appeal.builder()
                            .appellant(Appellant.builder()
                                    .address(Address.builder().postcode("LN8 4DX").build())
                                    .build())
                            .build())
                    .build())
            .build());

    @BeforeEach
    public void setUp() {
        final Boolean saveCorrespondence = false;
        notificationSender = new NotificationSender(
                notificationClient,
                testNotificationClient,
                bulkPrintService,
                blacklist,
                markdownTransformationService,
                saveCorrespondenceAsyncService,
                saveCorrespondence
        );
    }

    @Test
    public void sendEmailToTestSenderIfMatchesPattern() throws NotificationClientException {
        String emailAddress = "test123@hmcts.net";
        when(testNotificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
            .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender
                .sendEmail(templateId, emailAddress, personalisation, reference, APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendEmailToNormalSender() throws NotificationClientException {
        String emailAddress = "random@example.com";
        when(notificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
            .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender
                .sendEmail(templateId, emailAddress, personalisation, reference, APPEAL_RECEIVED, SSCS_CASE_DATA);

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

        notificationSender
                .sendEmail(templateId, emailAddress, personalisation, reference, APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendSmsToNormalSender() throws NotificationClientException {
        String phoneNumber = "07777777777";
        when(notificationClient.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER))
            .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendSms(
                templateId, phoneNumber, personalisation, reference, SMS_SENDER, APPEAL_RECEIVED, SSCS_CASE_DATA
        );

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

        notificationSender.sendSms(
                templateId, phoneNumber, personalisation, reference, SMS_SENDER, APPEAL_RECEIVED, SSCS_CASE_DATA
        );

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER);
    }

    @Test
    public void sendBundledLetterToNormalSender() throws IOException, NotificationClientException {
        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);
        when(notificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());
        byte[] sampleCoversheet =
                toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream(SAMPLE_COVERSHEET)));

        notificationSender.sendBundledLetter(wrapper, sampleCoversheet, "Bob Squires");

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendPrecompiledLetterWithInputStream(any(), any());
        verify(saveCorrespondenceAsyncService).saveLetter(any(byte[].class), any(Correspondence.class), eq(CASE_D));
    }

    @Test
    public void sendLargeBundledLetterToSender() throws IOException, NotificationClientException {
        when(notificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());
        byte[] largeLetter =
                toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream(LARGE_PDF)));

        notificationSender.sendBundledLetter(wrapper, largeLetter, "Bob Squires");

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    public void sendBundledLetterToSenderIfOnBlacklist() throws NotificationClientException {
        String postcode = "TS1 1ST";
        when(blacklist.getPostcodes()).thenReturn(Collections.singletonList(postcode));
        when(testNotificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());
        byte[] sampleDirectionCoversheet = "sampleDirectionCoversheet".getBytes();
        wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().setPostcode(postcode);

        notificationSender.sendBundledLetter(wrapper, sampleDirectionCoversheet, "Bob Squires");

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    public void sendLetterToNormalSender() throws NotificationClientException {
        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);
        when(notificationClient.sendLetter(any(), any(), any())).thenReturn(sendLetterResponse);
        when(sendLetterResponse.getNotificationId()).thenReturn(UUID.randomUUID());
        Address address = Address.builder()
                .line1("1 Appellant Ave").town("Sometown").county("Somecounty").postcode("LN8 4DX").build();

        notificationSender
                .sendLetter(templateId, address, personalisation, APPEAL_RECEIVED, "Bob Squires", CASE_D);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendLetter(any(), any(), any());
        verify(saveCorrespondenceAsyncService).saveLetter(eq(notificationClient), anyString(), any(Correspondence.class), eq(CASE_D));
    }

    @Test
    public void sendLetterToSenderIfOnBlacklist() throws NotificationClientException {
        String postcode = "TS1 1ST";
        when(blacklist.getPostcodes()).thenReturn(Collections.singletonList(postcode));
        when(testNotificationClient.sendLetter(any(), any(), any())).thenReturn(sendLetterResponse);
        when(sendLetterResponse.getNotificationId()).thenReturn(UUID.randomUUID());
        Address address = Address.builder()
                .line1("1 Appellant Ave").town("Sometown").county("Somecounty").postcode(postcode).build();

        notificationSender
                .sendLetter(templateId, address, personalisation, APPEAL_RECEIVED, "Bob Squires", CASE_D);

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

        notificationSender
                .sendEmail(templateId, emailAddress, personalisation, reference, APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
        verify(markdownTransformationService).toHtml(eq(null));

        Correspondence expected = Correspondence.builder().value(CorrespondenceDetails.builder()
            .to(emailAddress)
            .from("")
            .correspondenceType(CorrespondenceType.Email)
            .eventType(APPEAL_RECEIVED.getId())
            .sentOn("this field is ignored")
            .build()).build();
        verify(saveCorrespondenceAsyncService).saveEmailOrSms(argThat((Correspondence arg) ->
                reflectionEquals(arg.getValue(), expected.getValue(), "sentOn")), eq(SSCS_CASE_DATA));
    }

    @Test
    public void whenAnEmailIsSentAndEmailResponseIsNullThenWillNotSaveEmailNotificationInCcd()
            throws NotificationClientException {
        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);
        String emailAddress = "random@example.com";
        when(notificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
            .thenReturn(null);

        notificationSender
                .sendEmail(templateId, emailAddress, personalisation, reference, APPEAL_RECEIVED, SSCS_CASE_DATA);

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

        notificationSender.sendSms(
                templateId, smsNumber, personalisation, reference, "Sender", APPEAL_RECEIVED, SSCS_CASE_DATA
        );

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendSms(templateId, smsNumber, personalisation, reference, "Sender");
        verify(markdownTransformationService).toHtml(eq(null));

        Correspondence expected = Correspondence.builder().value(CorrespondenceDetails.builder()
            .to(smsNumber)
            .from("")
            .subject("SMS correspondence")
            .correspondenceType(CorrespondenceType.Sms)
            .eventType(APPEAL_RECEIVED.getId())
            .sentOn("this field is ignored")
            .build()).build();
        verify(saveCorrespondenceAsyncService).saveEmailOrSms(
                argThat(arg -> reflectionEquals(arg.getValue(), expected.getValue(), "sentOn")),
                eq(SSCS_CASE_DATA)
        );
    }

    @Test
    public void whenAnSmsIsSentAndSmsResponseIsNullThenWillNotSaveSmsNotificationInCcd()
            throws NotificationClientException {

        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);

        String smsNumber = "07999999000";
        when(notificationClient.sendSms(templateId, smsNumber, personalisation, reference, "Sender"))
            .thenReturn(null);

        notificationSender.sendSms(
                templateId, smsNumber, personalisation, reference, "Sender", APPEAL_RECEIVED, SSCS_CASE_DATA
        );

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendSms(templateId, smsNumber, personalisation, reference, "Sender");
        verifyNoMoreInteractions(markdownTransformationService);
        verifyNoMoreInteractions(saveCorrespondenceAsyncService);
    }

    @ParameterizedTest
    @CsvSource({"null", "NotificationClientException"})
    public void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendEmail(String error)
            throws NotificationClientException {
        String emailAddress = "test123@hmcts.net";
        Exception exception =
                (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
        doThrow(exception).when(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);

        assertThrows(NotificationClientException.class, () -> notificationSender
                .sendEmail(templateId, emailAddress, personalisation, reference, APPEAL_RECEIVED, SSCS_CASE_DATA));
    }

    @ParameterizedTest
    @CsvSource({"null", "NotificationClientException"})
    public void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendSms(String error) throws NotificationClientException {
        String smsNumber = "07999999000";
        Exception exception =
                (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
        doThrow(exception).when(notificationClient)
                .sendSms(templateId, smsNumber, personalisation, reference, "Sender");

        assertThrows(NotificationClientException.class, () -> notificationSender.sendSms(
                templateId, smsNumber, personalisation, reference, "Sender", APPEAL_RECEIVED, SSCS_CASE_DATA)
        );
    }

    @ParameterizedTest
    @CsvSource({"null", "NotificationClientException"})
    public void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendLetter(String error)
            throws NotificationClientException {
        String postcode = "TS1 1ST";
        Address address = Address.builder()
                .line1("1 Appellant Ave").town("Sometown").county("Somecounty").postcode(postcode).build();
        Exception exception =
                (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
        doThrow(exception).when(notificationClient).sendLetter(any(), any(), any());

        assertThrows(NotificationClientException.class, () -> notificationSender
                .sendLetter(templateId, address, personalisation, APPEAL_RECEIVED, "Bob Squires", CASE_D));
    }

    @ParameterizedTest
    @CsvSource({"null", "NotificationClientException"})
    public void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendBundledLetter(String error) throws NotificationClientException, IOException {
        Exception exception = (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
        doThrow(exception).when(notificationClient).sendPrecompiledLetterWithInputStream(any(), any());
        byte[] sampleDirectionCoversheet =
                toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream(SAMPLE_COVERSHEET)));

        assertThrows(NotificationClientException.class, () ->
                notificationSender.sendBundledLetter(wrapper, sampleDirectionCoversheet, "Bob Squires"));
    }

    @Test
    public void saveLetterCorrespondence() {
        byte[] sampleLetter = "Letter".getBytes();

        notificationSender
                .saveLettersToReasonableAdjustment(sampleLetter, APPEAL_RECEIVED, "Bob Squires", CASE_D, APPELLANT);

        verify(saveCorrespondenceAsyncService)
                .saveLettersToReasonableAdjustment(eq(sampleLetter), correspondenceArgumentCaptor.capture(), eq(CASE_D), eq(APPELLANT));
        Correspondence correspondence = correspondenceArgumentCaptor.getValue();
        assertNotNull(correspondence);
        assertEquals(CorrespondenceType.Letter, correspondence.getValue().getCorrespondenceType());
        assertEquals("Bob Squires", correspondence.getValue().getTo());
        assertEquals(APPEAL_RECEIVED.getId(), correspondence.getValue().getEventType());
        assertEquals(ReasonableAdjustmentStatus.REQUIRED, correspondence.getValue().getReasonableAdjustmentStatus());
    }

    @Test
    public void saveLetterCorrespondence_emptyLetter() {
        notificationSender
                .saveLettersToReasonableAdjustment(null, APPEAL_RECEIVED, "Bob Squires", CASE_D, APPELLANT);
        verifyNoInteractions(saveCorrespondenceAsyncService);
    }

    @Test
    public void recoverWillConsumeThrowable() {
        notificationSender.getBackendResponseFallback(new NotificationClientException("400 BadRequestError"));
    }

    @Test
    public void formatter_returnsCorrectYearAtEndOfYear() {
        final String dateFormat = LocalDateTime.of(2020, 12, 31, 12, 0, 0)
                .atZone(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER);
        assertThat(dateFormat, is("31 Dec 2020 12:00"));
    }

    @Test
    public void formatter_returnsCorrectYearAtStartOfYear() {
        final String dateFormat = LocalDateTime.of(2021, 1, 1, 12, 0, 0)
                .atZone(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER);
        assertThat(dateFormat, is("1 Jan 2021 12:00"));
    }
}
