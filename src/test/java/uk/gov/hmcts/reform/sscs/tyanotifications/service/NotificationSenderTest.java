package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.pdfbox.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationSender.DATE_TIME_FORMATTER;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationSender.ZONE_ID_LONDON;

import ch.qos.logback.classic.Level;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReasonableAdjustmentStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.PdfException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.helper.PdfHelper;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationTestRecipients;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendLetterResponse;
import uk.gov.service.notify.SendSmsResponse;

@ExtendWith(MockitoExtension.class)
class NotificationSenderTest {

    @RegisterExtension
    private final LogCaptureExtension logCapture =
        new LogCaptureExtension(NotificationSender.class);

    private static final String SAMPLE_COVERSHEET = "pdf/direction-notice-coversheet-sample.pdf";
    private static final String LARGE_PDF = "pdf/eleven-page-test-document.pdf";
    private static final String CASE_D = "78980909090099";
    private static final SscsCaseData SSCS_CASE_DATA = SscsCaseData.builder().build();
    private static final String SMS_SENDER = "sms-sender";
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
    void setUp() {
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
    void sendEmailToTestSenderIfMatchesPattern() throws NotificationClientException {
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
    void sendEmailToNormalSender() throws NotificationClientException {
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
    void sendEmailToTestSenderIfOnBlacklist() throws NotificationClientException {
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
    void sendSmsToNormalSender() throws NotificationClientException {
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
    void sendSmsToTestSenderIfOnBlacklist() throws NotificationClientException {
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
    void sendBundledLetterToNormalSender() throws IOException, NotificationClientException {
        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);
        when(notificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());
        byte[] sampleCoversheet =
            toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream(SAMPLE_COVERSHEET)));

        notificationSender.sendBundledLetter(wrapper, sampleCoversheet, "Bob Squires");

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendPrecompiledLetterWithInputStream(any(), any());
        verify(saveCorrespondenceAsyncService)
            .saveLetter(eq(notificationClient), anyString(), any(Correspondence.class), eq(CASE_D));
    }

    @Test
    void sendLargeBundledLetterToSender() throws IOException, NotificationClientException {
        when(notificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());
        byte[] largeLetter =
            toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream(LARGE_PDF)));

        notificationSender.sendBundledLetter(wrapper, largeLetter, "Bob Squires");

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    void sendBundledLetterToSenderIfOnBlacklist() throws NotificationClientException {
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
    void sendLetterToNormalSender() throws NotificationClientException {
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
    void sendLetterToSenderIfOnBlacklist() throws NotificationClientException {
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
    void whenAnEmailIsSentWillSaveEmailNotificationInCcd() throws NotificationClientException {
        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);
        String emailAddress = "random@example.com";
        when(notificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
            .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender
            .sendEmail(templateId, emailAddress, personalisation, reference, APPEAL_RECEIVED, SSCS_CASE_DATA);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
        verify(markdownTransformationService).toHtml(null);

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
    void whenAnEmailIsSentAndEmailResponseIsNullThenWillNotSaveEmailNotificationInCcd()
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
    void whenAnSmsIsSentWillSaveSmsNotificationInCcd() throws NotificationClientException {
        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);

        String smsNumber = "07999999000";
        when(notificationClient.sendSms(templateId, smsNumber, personalisation, reference, "Sender"))
            .thenReturn(sendSmsResponse);

        notificationSender.sendSms(
            templateId, smsNumber, personalisation, reference, "Sender", APPEAL_RECEIVED, SSCS_CASE_DATA
        );

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendSms(templateId, smsNumber, personalisation, reference, "Sender");
        verify(markdownTransformationService).toHtml(null);

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
    void whenAnSmsIsSentAndSmsResponseIsNullThenWillNotSaveSmsNotificationInCcd()
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
    void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendEmail(String error)
        throws NotificationClientException {
        String emailAddress = "test123@hmcts.net";
        Exception exception =
            (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
        doThrow(exception).when(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);

        assertThatThrownBy(() -> notificationSender
            .sendEmail(templateId, emailAddress, personalisation, reference, APPEAL_RECEIVED, SSCS_CASE_DATA))
            .isInstanceOf(NotificationClientException.class);
    }

    @ParameterizedTest
    @CsvSource({"null", "NotificationClientException"})
    void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendSms(String error) throws NotificationClientException {
        String smsNumber = "07999999000";
        Exception exception =
                (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
        doThrow(exception).when(notificationClient)
                .sendSms(templateId, smsNumber, personalisation, reference, "Sender");

        assertThatThrownBy(() -> notificationSender.sendSms(
            templateId, smsNumber, personalisation, reference, "Sender", APPEAL_RECEIVED, SSCS_CASE_DATA))
            .isInstanceOf(NotificationClientException.class);
    }

    @ParameterizedTest
    @CsvSource({"null", "NotificationClientException"})
    void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendLetter(String error)
        throws NotificationClientException {
        String postcode = "TS1 1ST";
        Address address = Address.builder()
                .line1("1 Appellant Ave").town("Sometown").county("Somecounty").postcode(postcode).build();
        Exception exception =
                (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
        doThrow(exception).when(notificationClient).sendLetter(any(), any(), any());

        assertThatThrownBy(() -> notificationSender
            .sendLetter(templateId, address, personalisation, APPEAL_RECEIVED, "Bob Squires", CASE_D))
            .isInstanceOf(NotificationClientException.class);
    }

    @ParameterizedTest
    @CsvSource({"null", "NotificationClientException"})
    void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendBundledLetter(String error) throws NotificationClientException, IOException {
        Exception exception = (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
        doThrow(exception).when(notificationClient).sendPrecompiledLetterWithInputStream(any(), any());
        byte[] sampleDirectionCoversheet =
                toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream(SAMPLE_COVERSHEET)));

        assertThatThrownBy(() ->
            notificationSender.sendBundledLetter(wrapper, sampleDirectionCoversheet, "Bob Squires"))
            .isInstanceOf(NotificationClientException.class);
    }

    @Test
    void saveLetterCorrespondence() {
        byte[] sampleLetter = "Letter".getBytes();

        notificationSender
                .saveLettersToReasonableAdjustment(sampleLetter, APPEAL_RECEIVED, "Bob Squires", CASE_D, APPELLANT);

        verify(saveCorrespondenceAsyncService)
            .saveLettersToReasonableAdjustment(eq(sampleLetter), correspondenceArgumentCaptor.capture(), eq(CASE_D),
                eq(APPELLANT));
        Correspondence correspondence = correspondenceArgumentCaptor.getValue();
        assertThat(correspondence).isNotNull();
        assertThat(correspondence.getValue().getCorrespondenceType()).isEqualTo(CorrespondenceType.Letter);
        assertThat(correspondence.getValue().getTo()).isEqualTo("Bob Squires");
        assertThat(correspondence.getValue().getEventType()).isEqualTo(APPEAL_RECEIVED.getId());
        assertThat(correspondence.getValue().getReasonableAdjustmentStatus()).isEqualTo(ReasonableAdjustmentStatus.REQUIRED);
    }

    @Test
    void saveLetterCorrespondence_emptyLetter() {
        notificationSender
            .saveLettersToReasonableAdjustment(null, APPEAL_RECEIVED, "Bob Squires", CASE_D, APPELLANT);
        verifyNoInteractions(saveCorrespondenceAsyncService);
    }

    @Test
    void recoverWillConsumeThrowable() {
        notificationSender.getBackendResponseFallback(new NotificationClientException("400 BadRequestError"));
        logCapture.assertLogContains("Failed sending.....", Level.ERROR);
    }

    @Test
    void formatter_returnsCorrectYearAtEndOfYear() {
        final String dateFormat = LocalDateTime.of(2020, 12, 31, 12, 0, 0)
                .atZone(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER);
        assertThat(dateFormat).isEqualTo("31 Dec 2020 12:00");
    }

    @Test
    void formatter_returnsCorrectYearAtStartOfYear() {
        final String dateFormat = LocalDateTime.of(2021, 1, 1, 12, 0, 0)
                                               .atZone(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER);
        assertThat(dateFormat).isEqualTo("1 Jan 2021 12:00");
    }

    @Test
    void sendBundledLetter_notificationWrapper_doesNothingWhenContentIsNull() throws NotificationClientException {
        notificationSender.sendBundledLetter(wrapper, null, "Bob Squires");

        verifyNoInteractions(notificationClient, testNotificationClient, bulkPrintService, saveCorrespondenceAsyncService);
    }

    @Test
    void sendBundledLetter_notificationWrapper_sendsToBulkPrintForIssueFinalDecisionWhenPageLimitExceeded()
        throws IOException, NotificationClientException {
        final NotificationWrapper issueFinalDecisionWrapper = buildWrapperForEvent(ISSUE_FINAL_DECISION);
        final byte[] largeLetter =
            toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream(LARGE_PDF)));

        notificationSender.sendBundledLetter(issueFinalDecisionWrapper, largeLetter, "Bob Squires");

        verify(bulkPrintService).sendToBulkPrint(any(), any(), eq("Bob Squires"));
        verifyNoInteractions(notificationClient, testNotificationClient);
    }

    @Test
    void sendBundledLetter_notificationWrapper_savesLetterWithContentWhenSentViaBulkPrint()
        throws IOException, NotificationClientException {
        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);
        final NotificationWrapper issueFinalDecisionWrapper = buildWrapperForEvent(ISSUE_FINAL_DECISION);
        final byte[] largeLetter =
            toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream(LARGE_PDF)));

        notificationSender.sendBundledLetter(issueFinalDecisionWrapper, largeLetter, "Bob Squires");

        verify(saveCorrespondenceAsyncService).saveLetter(eq(largeLetter), any(Correspondence.class), eq(CASE_D));
    }

    @Test
    void usesTestClientWhenWildcardPostcodeConfigured() throws IOException, NotificationClientException {
        when(blacklist.getPostcodes()).thenReturn(List.of("*"));
        when(testNotificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());
        final byte[] sampleCoversheet =
            toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream(SAMPLE_COVERSHEET)));

        notificationSender.sendBundledLetter(wrapper, sampleCoversheet, "Bob Squires");

        verify(testNotificationClient).sendPrecompiledLetterWithInputStream(any(), any());
        verifyNoInteractions(notificationClient);
    }

    @Test
    void sendBundledLetter_eventType_doesNothingWhenPdfsIsEmpty() {
        notificationSender.sendBundledLetter(
            EventType.ISSUE_GENERIC_LETTER, SSCS_CASE_DATA, List.of(), "Bob Squires");

        verifyNoInteractions(notificationClient, testNotificationClient, bulkPrintService, saveCorrespondenceAsyncService);
    }

    @Test
    void sendBundledLetter_eventType_sendsViaGovNotifyWhenWithinPageLimit() throws NotificationClientException {
        final byte[] content = "pdf-content".getBytes();
        final List<Pdf> pdfs = List.of(new Pdf(content, "letter.pdf"));
        final SscsCaseData caseData = buildCaseDataWithPostcode("LN8 4DX");
        when(notificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        final UUID uuid = UUID.randomUUID();
        when(letterResponse.getNotificationId()).thenReturn(uuid);

        try (final MockedStatic<PdfHelper> pdfHelper = mockStatic(PdfHelper.class)) {
            pdfHelper.when(() -> PdfHelper.buildBundledLetterFromPdfs(any())).thenReturn(content);
            pdfHelper.when(() -> PdfHelper.getPhysicalPageCount(any())).thenReturn(5);

            notificationSender.sendBundledLetter(
                EventType.ISSUE_GENERIC_LETTER, caseData, pdfs, "Bob Squires");
        }

        verify(notificationClient).sendPrecompiledLetterWithInputStream(anyString(), any());
        verify(saveCorrespondenceAsyncService)
            .saveLetter(eq(notificationClient), anyString(), any(Correspondence.class), eq(CASE_D));
        verifyNoInteractions(testNotificationClient, bulkPrintService);

        logCapture.assertLogContains("Sending %s Letter for case id %s via Gov Notify because it is less than or equal to 10 pages (%s pages).".formatted("issueGenericLetter", caseData.getCcdCaseId(), 5), Level.INFO);
        logCapture.assertLogContains("Letter Notification sent via Gov Notify for Letter %s, case id %s and Gov Notify id %s".formatted("issueGenericLetter", caseData.getCcdCaseId(), uuid.toString()), Level.INFO);
        logCapture.assertLogContains("Letter Notification recorded in CCD for Letter issueGenericLetter, case id %s and Gov Notify id %s".formatted(caseData.getCcdCaseId(), uuid.toString()), Level.INFO);
    }

    @Test
    void sendBundledLetter_eventType_sendsToBulkPrintWhenExceedsPageLimit() {
        final byte[] content = "pdf-content".getBytes();
        final List<Pdf> pdfs = List.of(new Pdf(content, "letter.pdf"));
        final SscsCaseData caseData = buildCaseDataWithPostcode("LN8 4DX");

        final UUID uuid = UUID.randomUUID();
        try (final MockedStatic<PdfHelper> pdfHelper = mockStatic(PdfHelper.class)) {
            pdfHelper.when(() -> PdfHelper.buildBundledLetterFromPdfs(any())).thenReturn(content);
            pdfHelper.when(() -> PdfHelper.getPhysicalPageCount(any())).thenReturn(11);
            when(bulkPrintService.sendToBulkPrint(any(), any(), any())).thenReturn(Optional.of(uuid));

            notificationSender.sendBundledLetter(
                EventType.ISSUE_GENERIC_LETTER, caseData, pdfs, "Bob Squires");
        }

        verify(bulkPrintService).sendToBulkPrint(pdfs, caseData, "Bob Squires");
        verifyNoInteractions(notificationClient, testNotificationClient);
        verify(saveCorrespondenceAsyncService).saveLetter(eq(content), any(Correspondence.class), eq(CASE_D));

        logCapture.assertLogContains("Sending %s Letter for case id %s via BulkPrint because it exceeds 10 pages (%s pages).".formatted("issueGenericLetter", caseData.getCcdCaseId(), 11), Level.INFO);
        logCapture.assertLogContains("Letter Notification sent via Bulk Print for Letter %s, case id %s and Bulk print id %s".formatted("issueGenericLetter", caseData.getCcdCaseId(), uuid.toString()), Level.INFO);
        logCapture.assertLogContains("Letter Notification recorded in CCD for Letter issueGenericLetter, case id %s and Bulk print id %s".formatted(caseData.getCcdCaseId(), uuid.toString()), Level.INFO);
    }

    @Test
    void sendBundledLetter_eventType_doesNotSaveToCcdWhenBulkPrintIdNotReturned() {
        final byte[] content = "pdf-content".getBytes();
        final List<Pdf> pdfs = List.of(new Pdf(content, "letter.pdf"));
        final SscsCaseData caseData = buildCaseDataWithPostcode("LN8 4DX");

        try (final MockedStatic<PdfHelper> pdfHelper = mockStatic(PdfHelper.class)) {
            pdfHelper.when(() -> PdfHelper.buildBundledLetterFromPdfs(any())).thenReturn(content);
            pdfHelper.when(() -> PdfHelper.getPhysicalPageCount(any())).thenReturn(11);
            when(bulkPrintService.sendToBulkPrint(any(), any(), any())).thenReturn(Optional.empty());

            notificationSender.sendBundledLetter(
                EventType.ISSUE_GENERIC_LETTER, caseData, pdfs, "Bob Squires");
        }

        verify(bulkPrintService).sendToBulkPrint(pdfs, caseData, "Bob Squires");
        verifyNoInteractions(notificationClient, testNotificationClient, saveCorrespondenceAsyncService);

        logCapture.assertLogContains(
            "Failed to send to bulk print for case %s. No print id returned".formatted(caseData.getCcdCaseId()),
            Level.ERROR);
    }

    @Test
    void sendBundledLetter_eventType_doesNotSaveToCcdWhenGovNotifyIdNotReturned() throws NotificationClientException {
        final byte[] content = "pdf-content".getBytes();
        final List<Pdf> pdfs = List.of(new Pdf(content, "letter.pdf"));
        final SscsCaseData caseData = buildCaseDataWithPostcode("LN8 4DX");
        when(notificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(null);

        try (final MockedStatic<PdfHelper> pdfHelper = mockStatic(PdfHelper.class)) {
            pdfHelper.when(() -> PdfHelper.buildBundledLetterFromPdfs(any())).thenReturn(content);
            pdfHelper.when(() -> PdfHelper.getPhysicalPageCount(any())).thenReturn(5);

            notificationSender.sendBundledLetter(
                EventType.ISSUE_GENERIC_LETTER, caseData, pdfs, "Bob Squires");
        }

        verify(notificationClient).sendPrecompiledLetterWithInputStream(anyString(), any());
        verifyNoInteractions(testNotificationClient, bulkPrintService, saveCorrespondenceAsyncService);

        logCapture.assertLogContains(
            "Failed to send letter for case %s. Gov notify id is null".formatted(caseData.getCcdCaseId()),
            Level.ERROR);
    }

    @Test
    void sendBundledLetter_eventType_wrapsPdfExceptionWhenPageCountCalculationFails() {
        final List<Pdf> pdfs = List.of(new Pdf("pdf-content".getBytes(), "letter.pdf"));
        final SscsCaseData caseData = buildCaseDataWithPostcode("LN8 4DX");
        final PdfException pdfException = new PdfException("boom", new IOException("broken"));

        try (final MockedStatic<PdfHelper> pdfHelper = mockStatic(PdfHelper.class)) {
            pdfHelper.when(() -> PdfHelper.getPhysicalPageCount(any())).thenThrow(pdfException);

            assertThatThrownBy(() -> notificationSender.sendBundledLetter(
                EventType.ISSUE_GENERIC_LETTER, caseData, pdfs, "Bob Squires"))
                .isInstanceOf(NotificationServiceException.class)
                .hasCause(pdfException);
        }

        verifyNoInteractions(notificationClient, testNotificationClient, bulkPrintService, saveCorrespondenceAsyncService);
        logCapture.assertLogContains(
            "Failed to calculate the number of pages contained in the letter %s for case id %s and notification %s"
                .formatted(pdfException.getMessage(), caseData.getCcdCaseId(), EventType.ISSUE_GENERIC_LETTER.getCcdType()),
            Level.ERROR);
    }

    @Test
    void sendBundledLetter_eventType_wrapsNotificationClientExceptionFromGovNotify() throws NotificationClientException {
        final byte[] content = "pdf-content".getBytes();
        final List<Pdf> pdfs = List.of(new Pdf(content, "letter.pdf"));
        final SscsCaseData caseData = buildCaseDataWithPostcode("LN8 4DX");
        final NotificationClientException notifyException = new NotificationClientException("boom");
        when(notificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenThrow(notifyException);

        try (final MockedStatic<PdfHelper> pdfHelper = mockStatic(PdfHelper.class)) {
            pdfHelper.when(() -> PdfHelper.buildBundledLetterFromPdfs(any())).thenReturn(content);
            pdfHelper.when(() -> PdfHelper.getPhysicalPageCount(any())).thenReturn(5);

            assertThatThrownBy(() -> notificationSender.sendBundledLetter(
                EventType.ISSUE_GENERIC_LETTER, caseData, pdfs, "Bob Squires"))
                .isInstanceOf(NotificationServiceException.class)
                .hasCause(notifyException);
        }

        verifyNoInteractions(testNotificationClient, bulkPrintService, saveCorrespondenceAsyncService);
        logCapture.assertLogContains(
            "Error sending notification for case id: %s".formatted(caseData.getCcdCaseId()),
            Level.ERROR);
    }

    private NotificationWrapper buildWrapperForEvent(final NotificationEventType eventType) {
        return new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
            .notificationEventType(eventType)
            .newSscsCaseData(SscsCaseData.builder()
                .ccdCaseId(CASE_D)
                .appeal(Appeal.builder()
                    .appellant(Appellant.builder()
                        .address(Address.builder().postcode("LN8 4DX").build())
                        .build())
                    .build())
                .build())
            .build());
    }

    private SscsCaseData buildCaseDataWithPostcode(final String postcode) {
        return SscsCaseData.builder()
            .ccdCaseId(CASE_D)
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .address(Address.builder().postcode(postcode).build())
                    .build())
                .build())
            .build();
    }
}