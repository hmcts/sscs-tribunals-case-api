package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.SYA_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationProcessingServiceTest.verifyExpectedLogMessage;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.TyanRetryConfig;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.NotificationExecutionManager;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.OutOfHoursCalculator;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.reminder.JobGroupGenerator;
import uk.gov.service.notify.NotificationClientException;

@ExtendWith(MockitoExtension.class)
public class NotificationExecutionManagerTest {

    private static final int MAX_RETRY = 3;
    private static final NotificationEventType A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS = NotificationEventType.HEARING_REMINDER;

    @Mock
    private OutOfHoursCalculator outOfHoursCalculator;
    @Mock
    private JobScheduler jobScheduler;
    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private NotificationWrapper notificationWrapper;
    @Mock
    private NotificationExecutionManager.SendAction sendAction;
    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<ILoggingEvent> captorLoggingEvent;

    private final Map<Integer, Integer> delayInSecondsMap = new HashMap<>() {{
            put(1, 100);
            put(2, 200);
            put(3, 300);
        }};
    private final TyanRetryConfig retryConfig = new TyanRetryConfig();
    private final SscsCaseData newSscsCaseData = SscsCaseData.builder().ccdCaseId("456").build();
    private final NotificationSscsCaseDataWrapper wrapper = NotificationSscsCaseDataWrapper.builder()
            .state(State.APPEAL_CREATED).newSscsCaseData(newSscsCaseData)
            .notificationEventType(SYA_APPEAL_CREATED).build();
    private final NotificationWrapper ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);

    private NotificationExecutionManager underTest;

    @BeforeEach
    public void setUp() {
        retryConfig.setMax(MAX_RETRY);
        retryConfig.setDelayInSeconds(delayInSecondsMap);
        underTest =
                new NotificationExecutionManager(outOfHoursCalculator, jobScheduler, jobGroupGenerator, retryConfig);
        Logger logger = (Logger) LoggerFactory.getLogger(NotificationExecutionManager.class.getName());
        logger.addAppender(mockAppender);
    }

    @Test
    public void shouldExecuteNotificationIfNotificationTypeCanBeSentOutOfHouseAndItIsInHours() throws NotificationClientException {
        canExecuteNotification();
    }

    @Test
    public void shouldSendValidAppealCreatedNotificationIfNotificationTypeCanBeSentOutOfHouseAndItIsInHours() throws NotificationClientException {
        canExecuteNotification();
    }

    @Test
    public void shouldExecuteNotificationIfNotificationTypeCanBeSentOutOfHouseAndItIsOutOfHours() throws NotificationClientException {
        canExecuteNotification();
    }

    @Test
    public void shouldExecuteNotificationIfNotificationTypeCannotBeSentOutOfHouseAndItIsInHours() throws NotificationClientException {
        canExecuteNotification();
    }

    @Test
    public void canScheduleNotifications() {
        when(notificationWrapper.getNotificationType()).thenReturn(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS);
        String payload = "payload";
        when(notificationWrapper.getSchedulerPayload()).thenReturn(payload);
        String caseId = "caseId";
        when(notificationWrapper.getCaseId()).thenReturn(caseId);

        ZonedDateTime whenToScheduleJob = ZonedDateTime.now();
        when(outOfHoursCalculator.getStartOfNextInHoursPeriod()).thenReturn(whenToScheduleJob);
        String group = "group";
        when(jobGroupGenerator.generate(caseId, A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS.getId())).thenReturn(group);

        underTest.scheduleNotification(notificationWrapper);
        @SuppressWarnings({"rawtypes","unchecked"})
        ArgumentCaptor<Job<?>> argument = (ArgumentCaptor) ArgumentCaptor.forClass(Job.class);
        verify(jobScheduler).schedule(argument.capture());

        Job<?> value = argument.getValue();
        assertThat(value.triggerAt, is(whenToScheduleJob));
        assertThat(value.group, is(group));
        assertThat(value.name, is(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS.getId()));
        assertThat(value.payload, is(payload));
    }

    @Test
    public void shouldScheduleNotificationsAtASpecifiedTime() {
        when(notificationWrapper.getNotificationType()).thenReturn(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS);
        String payload = "payload";
        when(notificationWrapper.getSchedulerPayload()).thenReturn(payload);
        String caseId = "caseId";
        when(notificationWrapper.getCaseId()).thenReturn(caseId);

        ZonedDateTime whenToScheduleJob = ZonedDateTime.now();
        String group = "group";
        when(jobGroupGenerator.generate(caseId, A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS.getId())).thenReturn(group);

        underTest.scheduleNotification(notificationWrapper, whenToScheduleJob);
        @SuppressWarnings({"rawtypes","unchecked"})
        ArgumentCaptor<Job<?>> argument = (ArgumentCaptor) ArgumentCaptor.forClass(Job.class);
        verify(jobScheduler).schedule(argument.capture());

        Job<?> value = argument.getValue();
        assertThat(value.triggerAt, is(whenToScheduleJob));
        assertThat(value.group, is(group));
        assertThat(value.name, is(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS.getId()));
        assertThat(value.payload, is(payload));
    }

    private void canExecuteNotification() throws NotificationClientException {
        underTest.executeNotification(notificationWrapper, "someTemplate", "Email", sendAction);

        verify(sendAction).send();
    }

    @Test
    public void shouldScheduleNotificationsAtASpecifiedTimeWithRetry() {
        final int retry = 1;
        when(notificationWrapper.getNotificationType()).thenReturn(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS);
        final String payload = "payload";
        final String expectedPayload = payload + "," + retry;
        when(notificationWrapper.getSchedulerPayload()).thenReturn(payload);
        final String caseId = "caseId";
        when(notificationWrapper.getCaseId()).thenReturn(caseId);

        final ZonedDateTime whenToScheduleJob = ZonedDateTime.now();
        final String group = "group";
        when(jobGroupGenerator.generate(caseId, A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS.getId())).thenReturn(group);

        underTest.scheduleNotification(notificationWrapper, retry, whenToScheduleJob);
        @SuppressWarnings({"rawtypes","unchecked"})
        final ArgumentCaptor<Job<?>> argument = (ArgumentCaptor) ArgumentCaptor.forClass(Job.class);
        verify(jobScheduler).schedule(argument.capture());

        final Job<?> value = argument.getValue();
        assertThat(value.triggerAt, is(whenToScheduleJob));
        assertThat(value.group, is(group));
        assertThat(value.name, is(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS.getId()));
        assertThat(value.payload, is(expectedPayload));
    }

    @Test
    public void shouldThrowNotificationClientRuntimeExceptionForAnyNotificationException() throws Exception {
        doThrow(new NotificationClientException(new UnknownHostException()))
            .when(sendAction)
            .send();
        stubData();

        assertThrows(NotificationClientRuntimeException.class, () ->
            underTest.executeNotification(notificationWrapper, "someTemplate", "Email", sendAction)
        );

        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, notificationWrapper.getNewSscsCaseData().getCcdCaseId(), "Could not send Email notification for case id: 123", Level.ERROR);
    }

    @Test
    public void shouldLogGovNotifyErrorCodeWhenNotificationClientExceptionIsThrown() throws Exception {
        doThrow(new NotificationClientException("Should return a 400 error code"))
            .when(sendAction)
            .send();
        stubData();

        assertThrows(NotificationServiceException.class, () ->
            underTest.executeNotification(notificationWrapper, "someTemplate", "Email", sendAction)
        );

        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, notificationWrapper.getNewSscsCaseData().getCcdCaseId(), "Error code 400 on GovUKNotify for case id: 123, template: someTemplate", Level.ERROR);
    }

    @Test
    public void shouldNotContinueWithAGovNotifyException() throws Exception {
        stubData();
        doThrow(new NotificationClientException(new RuntimeException()))
            .when(sendAction)
            .send();

        assertThrows(Throwable.class, () ->
            underTest.executeNotification(notificationWrapper, "someTemplate", "Email", sendAction)
        );

        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, notificationWrapper.getNewSscsCaseData().getCcdCaseId(), "Could not send Email notification for case id:", Level.ERROR);
    }

    @Test
    public void shouldContinueAndHandleAnyOtherException() throws Exception {
        stubData();
        doThrow(new RuntimeException()).when(sendAction).send();

        underTest.executeNotification(notificationWrapper, "someTemplate", "Email", sendAction);

        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, notificationWrapper.getNewSscsCaseData().getCcdCaseId(), "Could not send Email notification for case id:", Level.ERROR);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403})
    public void shouldNotRescheduleErrorWhenGovNotifyHttpStatus(int govNotifyHttpStatus) {
        NotificationClientException exception = mock(NotificationClientException.class);
        when(exception.getHttpResult()).thenReturn(govNotifyHttpStatus);

        underTest.rescheduleIfHandledGovNotifyErrorStatus(1, ccdNotificationWrapper, new NotificationServiceException("123", exception));

        verifyNoInteractions(jobGroupGenerator);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    public void shouldNotRescheduleNotificationWhenRetryAboveMaxRetry(int retry) {
        NotificationClientException exception = mock(NotificationClientException.class);
        when(exception.getHttpResult()).thenReturn(500);

        underTest.rescheduleIfHandledGovNotifyErrorStatus(retry, ccdNotificationWrapper, new NotificationServiceException("123", exception));

        verifyNoInteractions(jobGroupGenerator);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, -2})
    public void shouldNotRescheduleNotificationWhenRetryIsBelowOne(int retry) {
        NotificationClientException exception = mock(NotificationClientException.class);
        when(exception.getHttpResult()).thenReturn(500);

        underTest.rescheduleIfHandledGovNotifyErrorStatus(retry, ccdNotificationWrapper, new NotificationServiceException("123", exception));

        verifyNoInteractions(jobGroupGenerator);
    }

    @Test
    public void shouldNotRescheduleErrorWhenCaughtExceptionIsNotAGovNotifyException() {
        NotificationServiceException error = new NotificationServiceException("123", new RuntimeException("error"));

        underTest.rescheduleIfHandledGovNotifyErrorStatus(1, ccdNotificationWrapper, error);

        verifyNoInteractions(jobGroupGenerator);
    }

    @ParameterizedTest
    @CsvSource({"500,1", "492,2", "0,3"})
    public void shouldRescheduleErrorWhenGovNotifyHttpStatusAndRetry(int govNotifyHttpStatus, int retry) {
        NotificationClientException exception = mock(NotificationClientException.class);
        when(exception.getHttpResult()).thenReturn(govNotifyHttpStatus);

        underTest.rescheduleIfHandledGovNotifyErrorStatus(retry, ccdNotificationWrapper, new NotificationServiceException("123", exception));

        ZonedDateTime expectedRescheduledDateTime = ZonedDateTime.now().plusSeconds(delayInSecondsMap.get(retry));
        @SuppressWarnings({"rawtypes","unchecked"})
        ArgumentCaptor<Job<?>> argument = (ArgumentCaptor) ArgumentCaptor.forClass(Job.class);
        verify(jobScheduler).schedule(argument.capture());
        assertTrue(argument.getValue().triggerAt.isBefore(expectedRescheduledDateTime)
                || argument.getValue().triggerAt.isEqual(expectedRescheduledDateTime));
    }

    private void stubData() {
        String caseId = "123";
        when(notificationWrapper.getCaseId()).thenReturn(caseId);
        SscsCaseData stubbedCaseData = SscsCaseData.builder().ccdCaseId(caseId).build();
        when(notificationWrapper.getNewSscsCaseData()).thenReturn(stubbedCaseData);
    }
}
