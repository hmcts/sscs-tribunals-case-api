package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationServiceTest.verifyExpectedLogMessage;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder.JobGroupGenerator;
import uk.gov.service.notify.NotificationClientException;

@RunWith(MockitoJUnitRunner.class)
public class NotificationHandlerTest {

    private static final NotificationEventType A_NOTIFICATION_THAT_CAN_TRIGGER_OUT_OF_HOURS = NotificationEventType.SYA_APPEAL_CREATED;
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
    private NotificationHandler.SendNotification sendNotification;
    private NotificationHandler underTest;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<ILoggingEvent> captorLoggingEvent;

    @Before
    public void setUp() {
        underTest = new NotificationHandler(outOfHoursCalculator, jobScheduler, jobGroupGenerator);

        Logger logger = (Logger) LoggerFactory.getLogger(NotificationHandler.class.getName());
        logger.addAppender(mockAppender);
    }

    @Test
    public void shouldSendNotificationIfNotificationTypeCanBeSentOutOfHouseAndItIsInHours() throws NotificationClientException {
        canSendNotification(A_NOTIFICATION_THAT_CAN_TRIGGER_OUT_OF_HOURS, false);
    }

    @Test
    public void shouldSendValidAppealCreatedNotificationIfNotificationTypeCanBeSentOutOfHouseAndItIsInHours() throws NotificationClientException {
        canSendNotification(NotificationEventType.VALID_APPEAL_CREATED, false);
    }

    @Test
    public void shouldSendNotificationIfNotificationTypeCanBeSentOutOfHouseAndItIsOutOfHours() throws NotificationClientException {
        canSendNotification(A_NOTIFICATION_THAT_CAN_TRIGGER_OUT_OF_HOURS, true);
    }

    @Test
    public void shouldSendNotificationIfNotificationTypeCannotBeSentOutOfHouseAndItIsInHours() throws NotificationClientException {
        canSendNotification(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS, false);
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
        ArgumentCaptor<Job> argument = ArgumentCaptor.forClass(Job.class);
        verify(jobScheduler).schedule(argument.capture());

        Job value = argument.getValue();
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
        ArgumentCaptor<Job> argument = ArgumentCaptor.forClass(Job.class);
        verify(jobScheduler).schedule(argument.capture());

        Job value = argument.getValue();
        assertThat(value.triggerAt, is(whenToScheduleJob));
        assertThat(value.group, is(group));
        assertThat(value.name, is(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS.getId()));
        assertThat(value.payload, is(payload));
    }

    private void canSendNotification(NotificationEventType notificationEventType, boolean isOutOfHours) throws NotificationClientException {
        underTest.sendNotification(notificationWrapper, "someTemplate", "Email", sendNotification);

        verify(sendNotification).send();
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
        final ArgumentCaptor<Job> argument = ArgumentCaptor.forClass(Job.class);
        verify(jobScheduler).schedule(argument.capture());

        final Job value = argument.getValue();
        assertThat(value.triggerAt, is(whenToScheduleJob));
        assertThat(value.group, is(group));
        assertThat(value.name, is(A_NOTIFICATION_THAT_CANNOT_TRIGGER_OUT_OF_HOURS.getId()));
        assertThat(value.payload, is(expectedPayload));
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldThrowNotificationClientRuntimeExceptionForAnyNotificationException() throws Exception {
        doThrow(new NotificationClientException(new UnknownHostException()))
            .when(sendNotification)
            .send();
        stubData();
        try {
            underTest.sendNotification(notificationWrapper, "someTemplate", "Email", sendNotification);
        } catch (NotificationClientRuntimeException e) {
            verifyExpectedLogMessage(mockAppender, captorLoggingEvent, notificationWrapper.getNewSscsCaseData().getCcdCaseId(), "Could not send Email notification for case id: 123", Level.ERROR);
            throw e;
        }
    }

    @Test(expected = NotificationServiceException.class)
    public void shouldLogGovNotifyErrorCodeWhenNotificationClientExceptionIsThrown() throws Exception {
        doThrow(new NotificationClientException("Should return a 400 error code"))
            .when(sendNotification)
            .send();
        stubData();
        try {
            underTest.sendNotification(notificationWrapper, "someTemplate", "Email", sendNotification);
        } catch (NotificationServiceException ex) {
            verifyExpectedLogMessage(mockAppender, captorLoggingEvent, notificationWrapper.getNewSscsCaseData().getCcdCaseId(), "Error code 400 on GovUKNotify for case id: 123, template: someTemplate", Level.ERROR);
            throw ex;
        }
    }

    @Test(expected = NotificationServiceException.class)
    public void shouldNotContinueWithAGovNotifyException() throws Exception {
        stubData();
        doThrow(new NotificationClientException(new RuntimeException()))
            .when(sendNotification)
            .send();

        try {
            underTest.sendNotification(notificationWrapper, "someTemplate", "Email", sendNotification);
        } catch (Throwable throwable) {
            verifyExpectedLogMessage(mockAppender, captorLoggingEvent, notificationWrapper.getNewSscsCaseData().getCcdCaseId(), "Could not send Email notification for case id:", Level.ERROR);
            throw throwable;
        }
    }

    @Test
    public void shouldContinueAndHandleAnyOtherException() throws Exception {
        stubData();
        doThrow(new RuntimeException())
            .when(sendNotification)
            .send();

        underTest.sendNotification(notificationWrapper, "someTemplate", "Email", sendNotification);
        verifyExpectedLogMessage(mockAppender, captorLoggingEvent, notificationWrapper.getNewSscsCaseData().getCcdCaseId(), "Could not send Email notification for case id:", Level.ERROR);
    }

    private void stubData() {
        String caseId = "123";
        when(notificationWrapper.getCaseId()).thenReturn(caseId);
        SscsCaseData stubbedCaseData = SscsCaseData.builder().ccdCaseId(caseId).build();
        when(notificationWrapper.getNewSscsCaseData()).thenReturn(stubbedCaseData);
    }
}
