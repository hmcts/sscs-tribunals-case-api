package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.CcdNotificationsPdfService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.SaveCorrespondenceAsyncService;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_it.properties")
class SaveCorrespondenceAsyncServiceIt {

    private static final String NOTIFICATION_ID = "123";
    private static final String CCD_ID = "1776543211234";

    @Autowired
    private SaveCorrespondenceAsyncService saveCorrespondenceAsyncService;

    @MockitoBean
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(SaveCorrespondenceAsyncService.class);

    private Correspondence correspondence;

    @BeforeEach
    void setup() {
        correspondence = Correspondence.builder()
            .value(CorrespondenceDetails.builder()
                .correspondenceType(CorrespondenceType.Letter)
                .to("Mr Tester")
                .build())
            .build();
    }

    @Test
    void retriesSaveLetterAndRecoversWhenPdfRetrievalFails() throws NotificationClientException {
        NotificationClient client = mock(NotificationClient.class);
        when(client.getPdfForLetter(NOTIFICATION_ID)).thenThrow(new NotificationClientException("500 ServerError"));

        saveCorrespondenceAsyncService.saveLetter(client, NOTIFICATION_ID, correspondence, CCD_ID);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(client, times(3)).getPdfForLetter(NOTIFICATION_ID);
            verify(ccdNotificationsPdfService)
                    .notifyFailedToRetrieveCorrespondence(Long.valueOf(CCD_ID), NOTIFICATION_ID, CorrespondenceType.Letter);
        });
    }

    @Test
    void retriesSaveBulkPrintLetterAndRecoversWhenUploadFails() {
        doThrow(new RuntimeException("500 ServerError")).when(ccdNotificationsPdfService)
            .mergeLetterCorrespondenceIntoCcdV2(any(byte[].class), any(), any(), any());

        saveCorrespondenceAsyncService.saveLetter(new byte[]{}, correspondence, CCD_ID);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(ccdNotificationsPdfService, times(3)).mergeLetterCorrespondenceIntoCcdV2(
                any(byte[].class), any(), any(), any());
            verify(ccdNotificationsPdfService)
                .notifyFailedToRetrieveCorrespondence(Long.valueOf(CCD_ID), null, CorrespondenceType.Letter);
            logCapture.assertLogContains("Failed saving Letter correspondence into ccd for case id " + CCD_ID
                + " after retries exhausted, notification was sent but will not appear on the Notifications Sent tab.", Level.ERROR);
        });
    }

    @Test
    void retriesSaveLettersToReasonableAdjustmentAndRecoversWhenUploadFails() {
        doThrow(new RuntimeException("500 ServerError")).when(ccdNotificationsPdfService)
            .mergeReasonableAdjustmentsCorrespondenceIntoCcdV2(any(byte[].class), any(), any(), any());

        saveCorrespondenceAsyncService.saveLettersToReasonableAdjustment(new byte[]{}, correspondence, CCD_ID, SubscriptionType.APPELLANT);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(ccdNotificationsPdfService, times(3)).mergeReasonableAdjustmentsCorrespondenceIntoCcdV2(
                any(byte[].class), any(), any(), any());
            verify(ccdNotificationsPdfService)
                .notifyFailedToRetrieveCorrespondence(Long.valueOf(CCD_ID), null, CorrespondenceType.Letter);
        });
    }

    @Test
    void retriesSaveEmailOrSmsAndRecoversWhenUploadFails() {
        doThrow(new RuntimeException("500 ServerError")).when(ccdNotificationsPdfService)
            .mergeCorrespondenceIntoCcdV2(any(), any());
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId(CCD_ID).build();
        Correspondence emailCorrespondence = Correspondence.builder()
            .value(CorrespondenceDetails.builder().correspondenceType(CorrespondenceType.Email).to("Mr Tester").build())
            .build();

        saveCorrespondenceAsyncService.saveEmailOrSms(NOTIFICATION_ID, emailCorrespondence, sscsCaseData);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(ccdNotificationsPdfService, times(3))
                .mergeCorrespondenceIntoCcdV2(eq(Long.valueOf(CCD_ID)), eq(emailCorrespondence));
            verify(ccdNotificationsPdfService)
                .notifyFailedToRetrieveCorrespondence(Long.valueOf(CCD_ID), NOTIFICATION_ID, CorrespondenceType.Email);
            logCapture.assertLogContains("Failed saving Email correspondence into ccd for case id " + CCD_ID
                + " after retries exhausted, notification id " + NOTIFICATION_ID, Level.ERROR);
        });
    }
}
