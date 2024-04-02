package uk.gov.hmcts.reform.sscs.tyanotifications.service.scheduler;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.EVIDENCE_REMINDER;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.SYA_APPEAL_CREATED;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.RetryNotificationService;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public class CcdActionExecutorTest {

    private static final String JOB_GROUP = "group";
    private static final String JOB_ID = "1";
    private CcdActionExecutor ccdActionExecutor;

    @Mock
    private NotificationService notificationService;

    @Mock
    private IdamService idamService;

    @Mock
    private CcdService ccdService;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private RetryNotificationService retryNotificationService;

    private SscsCaseData newSscsCaseData;
    private SscsCaseDetails caseDetails;
    private NotificationSscsCaseDataWrapper wrapper;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        openMocks(this);

        Jackson2ObjectMapperBuilder objectMapperBuilder =
            new Jackson2ObjectMapperBuilder()
                .featuresToEnable(READ_ENUMS_USING_TO_STRING)
                .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
                .serializationInclusion(JsonInclude.Include.NON_ABSENT);

        final ObjectMapper mapper = objectMapperBuilder.createXmlMapper(false).build();
        mapper.registerModule(new JavaTimeModule());

        final SscsCaseCallbackDeserializer deserializer = new SscsCaseCallbackDeserializer(mapper);

        ccdActionExecutor = new CcdActionExecutor(notificationService, retryNotificationService, ccdService, updateCcdCaseService, idamService, deserializer);

        caseDetails = SscsCaseDetails.builder().id(456L).caseTypeId("123").state("appealCreated").build();

        newSscsCaseData = SscsCaseData.builder().ccdCaseId("456").sscsDeprecatedFields(SscsDeprecatedFields.builder().build()).sscsEsaCaseData(SscsEsaCaseData.builder().build()).pipSscsCaseData(SscsPipCaseData.builder().build()).build();
        caseDetails.setData(newSscsCaseData);

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void givenAReminderIsTriggered_thenActionExecutorShouldProcessTheJob() {
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);

        ccdActionExecutor.execute(JOB_ID, JOB_GROUP, EVIDENCE_REMINDER.getId(), "123456");

        verify(notificationService).manageNotificationAndSubscription(any(), eq(true));
        verify(updateCcdCaseService).updateCaseV2(eq(Long.valueOf(123456)), eq("evidenceReminder"), eq("CCD Case"), eq("Notification Service updated case"), any(), any(Consumer.class));
    }

    @Test
    public void givenAReminderIsTriggeredAndNotificationIsNotAReminderType_thenActionExecutorShouldProcessTheJobButNotWriteBackToCcd() {
        wrapper = NotificationSscsCaseDataWrapper.builder().state(State.APPEAL_CREATED).newSscsCaseData(newSscsCaseData).notificationEventType(SYA_APPEAL_CREATED).build();
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);

        ccdActionExecutor.execute(JOB_ID, JOB_GROUP, SYA_APPEAL_CREATED.getId(), "123456");

        verify(notificationService, times(1)).manageNotificationAndSubscription(any(), eq(true));
        verify(ccdService, times(0)).updateCase(any(), any(), any(), any(), any(), any());
    }

    @Test
    @Parameters({"123, 0", "124,1", "333, 3"})
    public void shouldReturnTheCorrectCaseAndRetryValueFromAPayload(long caseId, int retry) {
        String payload = (retry == 0) ? String.format("%s", caseId) : String.format("%s,%s", caseId, retry);
        long actualCaseId = ccdActionExecutor.getCaseId(payload);
        long actualRetry = ccdActionExecutor.getRetry(payload);
        assertEquals(caseId, actualCaseId);
        assertEquals(retry, actualRetry);
    }

    @Test
    public void shouldHandlePayloadWhenAlreadyRetriedOnceToSendNotification() {
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);
        ccdActionExecutor.execute(JOB_ID, JOB_GROUP, SYA_APPEAL_CREATED.getId(), "123456,1");

        verify(notificationService, times(1)).manageNotificationAndSubscription(any(), eq(true));
        verify(ccdService, times(0)).updateCase(any(), eq(123456L), any(), any(), any(), any());
    }

    @Test
    @Parameters({"1", "2", "3"})
    public void shouldScheduleToRetryAgainWhenNotificationFails(int retry) {
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);
        doThrow(new NotificationServiceException(caseDetails.getId().toString(), new NotificationClientException(new NullPointerException("error")))).when(notificationService).manageNotificationAndSubscription(any(), eq(true));
        final String payload = (retry == 0) ? "123456" : "123456," + retry;
        ccdActionExecutor.execute(JOB_ID, JOB_GROUP, SYA_APPEAL_CREATED.getId(), payload);

        verify(retryNotificationService).rescheduleIfHandledGovNotifyErrorStatus(eq(retry + 1), any(), any(NotificationServiceException.class));
        verify(ccdService, times(0)).updateCase(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void shouldWorkWithLocalDateTime() {
        caseDetails.setCreatedDate(LocalDateTime.now());
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);

        ccdActionExecutor.execute(JOB_ID, JOB_GROUP, SYA_APPEAL_CREATED.getId(), "123456");

        verify(notificationService, times(1)).manageNotificationAndSubscription(any(), eq(true));
        verify(ccdService, times(0)).updateCase(any(), eq(123456L), any(), any(), any(), any());

    }
}
