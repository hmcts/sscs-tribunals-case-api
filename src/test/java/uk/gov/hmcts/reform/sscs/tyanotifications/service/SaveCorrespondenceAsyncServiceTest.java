package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

        service.saveSentLetterToCase(bytes, correspondence, CCD_ID);

        verify(ccdNotificationsPdfService)
                .mergeLetterCorrespondenceIntoCcdV2(any(), eq(Long.valueOf(CCD_ID)), eq(correspondence));
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
    public void recoverWillConsumeThrowable() {
        service.getBackendResponseFallback(new NotificationClientException("400 BadRequestError"));
    }

    @ParameterizedTest
    @CsvSource({"APPELLANT, APPELLANT", "REPRESENTATIVE, REPRESENTATIVE", "APPOINTEE, APPOINTEE",
            "JOINT_PARTY, JOINT_PARTY", "OTHER_PARTY, OTHER_PARTY"})
    public void willUploadPdfFormatLettersDirectlyIntoCcd(SubscriptionType subscriptionType, LetterType letterType) {
        service.saveLetter(new byte[]{}, correspondence, CCD_ID, subscriptionType);

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

        service.saveEmailOrSms(correspondence, sscsCaseData);

        verify(ccdNotificationsPdfService).mergeCorrespondenceIntoCcdV2(any(Long.class), eq(correspondence));
    }
}
