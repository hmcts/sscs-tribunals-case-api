package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.LetterType;
import uk.gov.hmcts.reform.sscs.service.CcdNotificationsPdfService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@ExtendWith(MockitoExtension.class)
public class SaveCorrespondenceAsyncServiceTest {
    private static final String NOTIFICATION_ID = "123";
    private static final String CCD_ID = "82828";

    private SaveCorrespondenceAsyncService service;
    private Correspondence correspondence;

    @Mock
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    @Mock
    private NotificationClient notificationClient;

    @RegisterExtension
    private final LogCaptureExtension logCapture =
            new LogCaptureExtension(SaveCorrespondenceAsyncService.class);

    @BeforeEach
    public void setup() {
        service = new SaveCorrespondenceAsyncService(ccdNotificationsPdfService);
        correspondence =
                Correspondence.builder().value(CorrespondenceDetails.builder().to("Mr Blobby").build()).build();
    }

    @Test
    public void willGetLetterFromNotifyAndUploadIntoCcd() throws NotificationClientException {
        byte[] bytes = "%PDF bytes".getBytes();
        when(notificationClient.getPdfForLetter(eq(NOTIFICATION_ID))).thenReturn(bytes);

        service.saveLetter(notificationClient, NOTIFICATION_ID, correspondence, CCD_ID);

        verify(notificationClient).getPdfForLetter(eq(NOTIFICATION_ID));
        verify(ccdNotificationsPdfService)
                .mergeLetterCorrespondenceIntoCcdV2(any(), eq(Long.valueOf(CCD_ID)), eq(correspondence));
    }

    @Test
    public void willSaveSentLetterToCaseUploadsPdf() {
        byte[] bytes = "%PDF bytes".getBytes();

        service.saveLetter(bytes, correspondence, CCD_ID);

        verify(ccdNotificationsPdfService)
                .mergeLetterCorrespondenceIntoCcdV2(eq(bytes), eq(Long.valueOf(CCD_ID)), eq(correspondence), eq("Bulk Print"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"400 PDFNotReadyError", "400 BadRequestError"})
    public void notificationClientExceptionIsReThrown(String message) throws NotificationClientException {
        when(notificationClient.getPdfForLetter(eq(NOTIFICATION_ID)))
                .thenThrow(new NotificationClientException(message));
        assertThrows(NotificationClientException.class,
            () -> service.saveLetter(notificationClient, NOTIFICATION_ID, correspondence, CCD_ID));
    }

    @Test
    public void recoverWillConsumeThrowableForSaveLetter() {
        correspondence = Correspondence.builder().value(CorrespondenceDetails.builder()
                .correspondenceType(CorrespondenceType.Letter).to("Mr Tester").build())
                .build();

        service.recoverSaveLetter(new NotificationClientException("500 ServerError"),
                notificationClient, NOTIFICATION_ID, correspondence, CCD_ID);

        verify(ccdNotificationsPdfService).notifyFailedToRetrieveCorrespondence(Long.valueOf(CCD_ID), NOTIFICATION_ID, CorrespondenceType.Letter);
    }

    @Test
    public void recoverWillConsumeThrowableForSaveBulkPrintLetter() {
        correspondence = Correspondence.builder().value(CorrespondenceDetails.builder()
                .correspondenceType(CorrespondenceType.Letter).to("Mr Tester").build())
                .build();

        service.recoverSaveBulkPrintLetter(new RuntimeException("500 ServerError"),
                new byte[]{}, correspondence, CCD_ID);

        verify(ccdNotificationsPdfService).notifyFailedToRetrieveCorrespondence(Long.valueOf(CCD_ID), null, CorrespondenceType.Letter);
    }

    @Test
    public void recoverWillConsumeThrowableForEmailOrSms() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId(CCD_ID).build();
        correspondence = Correspondence.builder().value(CorrespondenceDetails.builder()
                .correspondenceType(CorrespondenceType.Email).to("Mr Tester").build())
                .build();

        service.recoverSaveEmailOrSms(new NotificationClientException("500 ServerError"), NOTIFICATION_ID, correspondence, sscsCaseData);

        verify(ccdNotificationsPdfService).notifyFailedToRetrieveCorrespondence(Long.valueOf(CCD_ID), NOTIFICATION_ID, CorrespondenceType.Email);
        logCapture.assertLogContains("Failed saving Email correspondence into ccd for case id " + CCD_ID + " after retries exhausted, notification id " + NOTIFICATION_ID + ", "
                + "notification was sent but will not appear on the Notifications Sent tab.", Level.ERROR);

    }

    @Test
    public void recoverWillConsumeThrowableForSaveLettersToReasonableAdjustment() {
        correspondence = Correspondence.builder().value(CorrespondenceDetails.builder()
                .correspondenceType(CorrespondenceType.Letter).to("Mr Tester").build())
                .build();

        service.recoverSaveLettersToReasonableAdjustment(new NotificationClientException("500 ServerError"),
                new byte[]{}, correspondence, CCD_ID, SubscriptionType.APPELLANT);

        verify(ccdNotificationsPdfService).notifyFailedToRetrieveCorrespondence(Long.valueOf(CCD_ID), null, CorrespondenceType.Letter);
    }

    @ParameterizedTest
    @CsvSource({"APPELLANT, APPELLANT", "REPRESENTATIVE, REPRESENTATIVE", "APPOINTEE, APPOINTEE",
                "JOINT_PARTY, JOINT_PARTY", "OTHER_PARTY, OTHER_PARTY"})
    public void willUploadPdfFormatLettersDirectlyIntoCcd(SubscriptionType subscriptionType, LetterType letterType) {
        service.saveLettersToReasonableAdjustment(new byte[]{}, correspondence, CCD_ID, subscriptionType);

        verify(ccdNotificationsPdfService).mergeReasonableAdjustmentsCorrespondenceIntoCcdV2(
                any(byte[].class), eq(Long.valueOf(CCD_ID)), eq(correspondence), eq(letterType)
        );
    }

    @Test
    public void willSaveEmailOrSmsDirectlyIntoCcd() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId(CCD_ID).build();
        correspondence = Correspondence.builder().value(CorrespondenceDetails.builder()
                .correspondenceType(CorrespondenceType.Email).to("Mr Blobby").build())
                .build();

        service.saveEmailOrSms(NOTIFICATION_ID, correspondence, sscsCaseData);

        verify(ccdNotificationsPdfService).mergeCorrespondenceIntoCcdV2(any(Long.class), eq(correspondence));
    }
}
