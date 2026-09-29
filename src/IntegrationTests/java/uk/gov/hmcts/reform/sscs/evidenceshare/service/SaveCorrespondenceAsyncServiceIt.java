package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.LetterType;
import uk.gov.hmcts.reform.sscs.service.CcdNotificationsPdfService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.SaveCorrespondenceAsyncService;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_es_it.properties", properties = {
    "letter-async.maxAttempts=3",
    "letter-async.delay=1",
    "letter-async.multiplier=1",
    "letter-async.maxDelay=1",
    "letter-async.initialDelay=0"
})
public class SaveCorrespondenceAsyncServiceIt {

    private static final String NOTIFICATION_ID = "123";
    private static final String CCD_ID = "82828";

    @Autowired
    private SaveCorrespondenceAsyncService saveCorrespondenceAsyncService;

    @MockitoBean
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    private Correspondence correspondence;

    @Before
    public void setup() {
        correspondence = Correspondence.builder()
            .value(CorrespondenceDetails.builder()
                .correspondenceType(CorrespondenceType.Letter)
                .to("Mr Blobby")
                .build())
            .build();
    }

    @Test
    public void retriesSaveLetterAndRecoversAfterConfiguredMaxAttemptsAreExceeded() throws NotificationClientException {
        NotificationClient client = mock(NotificationClient.class);
        when(client.getPdfForLetter(NOTIFICATION_ID)).thenThrow(new NotificationClientException("500 ServerError"));

        saveCorrespondenceAsyncService.saveLetter(client, NOTIFICATION_ID, correspondence, CCD_ID);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            verify(client, times(3)).getPdfForLetter(NOTIFICATION_ID));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            verify(ccdNotificationsPdfService)
                .notifyFailedToRetrieveCorrespondence(Long.valueOf(CCD_ID), NOTIFICATION_ID, CorrespondenceType.Letter));
    }

    @Test
    public void savesLettersToReasonableAdjustmentSuccessfully() {
        byte[] bytes = "%PDF bytes".getBytes();

        saveCorrespondenceAsyncService.saveLettersToReasonableAdjustment(bytes, correspondence, CCD_ID, SubscriptionType.APPELLANT);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            verify(ccdNotificationsPdfService).mergeReasonableAdjustmentsCorrespondenceIntoCcdV2(
                eq(bytes), eq(Long.valueOf(CCD_ID)), eq(correspondence), eq(LetterType.APPELLANT)));
    }

    @Test
    public void retriesSaveLettersToReasonableAdjustmentAndRecoversAfterConfiguredMaxAttemptsAreExceeded() {
        doThrow(new RuntimeException("boom")).when(ccdNotificationsPdfService)
            .mergeReasonableAdjustmentsCorrespondenceIntoCcdV2(any(byte[].class), any(), any(), any());

        saveCorrespondenceAsyncService.saveLettersToReasonableAdjustment(new byte[]{}, correspondence, CCD_ID, SubscriptionType.APPELLANT);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            verify(ccdNotificationsPdfService, times(3)).mergeReasonableAdjustmentsCorrespondenceIntoCcdV2(
                any(byte[].class), any(), any(), any()));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            verify(ccdNotificationsPdfService)
                .notifyFailedToRetrieveCorrespondence(Long.valueOf(CCD_ID), null, CorrespondenceType.Letter));
    }

    @Test
    public void retriesSaveEmailOrSmsAndRecoversAfterMaxAttemptsAreExceeded() {
        doThrow(new RuntimeException("boom")).when(ccdNotificationsPdfService)
            .mergeCorrespondenceIntoCcdV2(any(), any());
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId(CCD_ID).build();
        Correspondence emailCorrespondence = Correspondence.builder()
            .value(CorrespondenceDetails.builder().correspondenceType(CorrespondenceType.Email).to("Mr Blobby").build())
            .build();

        saveCorrespondenceAsyncService.saveEmailOrSms(NOTIFICATION_ID, emailCorrespondence, sscsCaseData);

        verify(ccdNotificationsPdfService, times(5))
            .mergeCorrespondenceIntoCcdV2(eq(Long.valueOf(CCD_ID)), eq(emailCorrespondence));
        verify(ccdNotificationsPdfService)
            .notifyFailedToRetrieveCorrespondence(Long.valueOf(CCD_ID), NOTIFICATION_ID, CorrespondenceType.Email);
    }
}
