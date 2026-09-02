package uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.HEARING_BOOKED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.HEARING_REMINDER;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingType;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.tyanotifications.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;

@RunWith(MockitoJUnitRunner.class)
public class HearingReminderTest {

    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobScheduler jobScheduler;

    private HearingReminder hearingReminder;

    private static final int BEFORE_FIRST_HEARING_REMINDER = 172800 * 2;
    private static final int BEFORE_SECOND_HEARING_REMINDER = 172800;

    @Before
    public void setup() {
        hearingReminder = new HearingReminder(
            jobGroupGenerator,
            jobScheduler,
            BEFORE_FIRST_HEARING_REMINDER,
            BEFORE_SECOND_HEARING_REMINDER
        );
    }

    @Test
    public void canHandleEventWhenOralHearingType() {

        for (NotificationEventType eventType : NotificationEventType.values()) {

            CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(eventType,
                AppealHearingType.ORAL.name());

            if (eventType == HEARING_BOOKED) {
                assertTrue(hearingReminder.canHandle(wrapper));
            } else {

                assertFalse(hearingReminder.canHandle(wrapper));
                assertThatThrownBy(() -> hearingReminder.handle(wrapper))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void canHandleEventWhenPaperHearingType() {

        for (NotificationEventType eventType : NotificationEventType.values()) {

            CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(eventType,
                AppealHearingType.PAPER.name());

            assertFalse(hearingReminder.canHandle(wrapper));
            assertThatThrownBy(() -> hearingReminder.handle(wrapper))
                .hasMessage("cannot handle ccdResponse")
                .isExactlyInstanceOf(IllegalArgumentException.class);

        }
    }

    @Test
    public void schedulesReminder() {

        final String expectedJobGroup = "ID_EVENT";

        LocalDate today = LocalDate.now(ZoneId.of(AppConstants.ZONE_ID));
        LocalDate hearingDate = today.plusDays(10);
        String hearingTime = "14:01:18";

        final ZonedDateTime expectedFirstTriggerAt = ZonedDateTime.ofLocal(
            hearingDate.atTime(14, 1, 18).minusSeconds(BEFORE_FIRST_HEARING_REMINDER),
            ZoneId.of(AppConstants.ZONE_ID),
            null
        );
        final ZonedDateTime expectedSecondTriggerAt = ZonedDateTime.ofLocal(
            hearingDate.atTime(14, 1, 18).minusSeconds(BEFORE_SECOND_HEARING_REMINDER),
            ZoneId.of(AppConstants.ZONE_ID),
            null
        );

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithHearingAndHearingType(
            HEARING_BOOKED,
            HearingType.ORAL,
            hearingDate.toString(),
            hearingTime
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), HEARING_REMINDER.getId())).thenReturn(expectedJobGroup);

        hearingReminder.handle(wrapper);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(2)).schedule(
            jobCaptor.capture()
        );

        Job<String> firstJob = jobCaptor.getAllValues().getFirst();
        assertEquals(expectedJobGroup, firstJob.group);
        assertEquals(HEARING_REMINDER.getId(), firstJob.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, firstJob.payload);
        assertEquals(expectedFirstTriggerAt, firstJob.triggerAt);

        Job<String> secondJob = jobCaptor.getAllValues().getLast();
        assertEquals(expectedJobGroup, secondJob.group);
        assertEquals(HEARING_REMINDER.getId(), secondJob.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, secondJob.payload);
        assertEquals(expectedSecondTriggerAt, secondJob.triggerAt);
    }

    @Test
    public void schedulesReminder_usingStartDateOverHearingDateTime() {

        final String expectedJobGroup = "ID_EVENT";
        LocalDateTime start = LocalDateTime.now(ZoneId.of(AppConstants.ZONE_ID)).plusDays(10);
        LocalDateTime hearingDateTime = start.minusHours(1);

        final ZonedDateTime expectedFirstTriggerBasedOnStart = ZonedDateTime.ofLocal(
                start.minusSeconds(BEFORE_FIRST_HEARING_REMINDER),
            ZoneId.of(AppConstants.ZONE_ID),
            null
        );
        final ZonedDateTime expectedSecondTriggerBasedOnStart  = ZonedDateTime.ofLocal(
                start.minusSeconds(BEFORE_SECOND_HEARING_REMINDER),
            ZoneId.of(AppConstants.ZONE_ID),
            null
        );

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(HEARING_BOOKED);
        List<Hearing> hearingList = List.of(Hearing.builder().value(HearingDetails.builder()
                        .start(start)
                        .hearingDate(hearingDateTime.toLocalDate().toString())
                        .time(hearingDateTime.toLocalTime().toString())
                .build()).build());
        wrapper.getNewSscsCaseData().setHearings(hearingList);

        when(jobGroupGenerator.generate(wrapper.getCaseId(), HEARING_REMINDER.getId())).thenReturn(expectedJobGroup);

        hearingReminder.handle(wrapper);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(2)).schedule(
            jobCaptor.capture()
        );

        Job<String> firstJob = jobCaptor.getAllValues().getFirst();
        assertEquals(expectedFirstTriggerBasedOnStart, firstJob.triggerAt);

        Job<String> secondJob = jobCaptor.getAllValues().getLast();
        assertEquals(expectedSecondTriggerBasedOnStart, secondJob.triggerAt);
    }

    @Test
    public void schedulesOnlyFutureReminderWhenOneReminderDateIsInThePast() {

        final String expectedJobGroup = "ID_EVENT";
        LocalDate today = LocalDate.now(ZoneId.of(AppConstants.ZONE_ID));
        LocalDate hearingDate = today.plusDays(3);
        String hearingTime = "14:01:18";

        final ZonedDateTime expectedTriggerAt = ZonedDateTime.ofLocal(
            hearingDate.atTime(14, 1, 18).minusSeconds(BEFORE_SECOND_HEARING_REMINDER),
            ZoneId.of(AppConstants.ZONE_ID),
            null
        );

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithHearingAndHearingType(
            HEARING_BOOKED,
            HearingType.ORAL,
            hearingDate.toString(),
            hearingTime
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), HEARING_REMINDER.getId())).thenReturn(expectedJobGroup);

        hearingReminder.handle(wrapper);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(1)).schedule(
            jobCaptor.capture()
        );

        Job<String> job = jobCaptor.getValue();
        assertEquals(expectedJobGroup, job.group);
        assertEquals(HEARING_REMINDER.getId(), job.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, job.payload);
        assertEquals(expectedTriggerAt, job.triggerAt);
    }

    @Test
    public void canNotScheduleReminderWhenReminderDateIsInThePast() {

        final String expectedJobGroup = "ID_EVENT";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithHearingAndHearingType(
            HEARING_BOOKED,
            HearingType.ORAL,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), HEARING_REMINDER.getId())).thenReturn(expectedJobGroup);

        hearingReminder.handle(wrapper);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(0)).schedule(
            jobCaptor.capture()
        );

        assertTrue(jobCaptor.getAllValues().isEmpty());
    }

    @Test
    public void canNotSchedulesReminderWhenReminderDateIsNull() {

        final String expectedJobGroup = "ID_EVENT";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithHearingAndHearingType(
            HEARING_BOOKED,
            HearingType.ORAL,
            hearingDate,
            hearingTime
        );

        wrapper.getNewSscsCaseData().setHearings(Lists.newArrayList());

        when(jobGroupGenerator.generate(wrapper.getCaseId(), HEARING_REMINDER.getId())).thenReturn(expectedJobGroup);

        hearingReminder.handle(wrapper);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(0)).schedule(
            jobCaptor.capture()
        );

        assertTrue(jobCaptor.getAllValues().isEmpty());
    }

    @Test(expected = Exception.class)
    public void canScheduleReturnFalseWhenFindHearingDateThrowError() {

        CcdNotificationWrapper ccdResponse = null;

        assertFalse(hearingReminder.canSchedule(ccdResponse));
    }

    @Test
    public void canScheduleReturnFalseWhenCannotFindHearingDate() {

        CcdNotificationWrapper ccdResponse = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(HEARING_BOOKED);

        assertFalse(hearingReminder.canSchedule(ccdResponse));
    }

}
