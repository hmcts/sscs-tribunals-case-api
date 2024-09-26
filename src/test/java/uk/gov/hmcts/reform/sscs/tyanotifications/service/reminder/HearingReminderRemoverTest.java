package uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.HEARING_REMINDER;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.POSTPONEMENT;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobNotFoundException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobRemover;
import uk.gov.hmcts.reform.sscs.tyanotifications.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;

@RunWith(MockitoJUnitRunner.class)
public class HearingReminderRemoverTest {

    @Mock
    private JobGroupGenerator jobGroupGenerator;
    @Mock
    private JobRemover jobRemover;

    private HearingReminderRemover hearingReminderRemoverTest;

    @Before
    public void setup() {
        hearingReminderRemoverTest = new HearingReminderRemover(
            jobGroupGenerator,
            jobRemover
        );
    }

    @Test
    public void canHandleEvent() {

        for (NotificationEventType eventType : NotificationEventType.values()) {

            CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(eventType);

            if (eventType == POSTPONEMENT) {
                assertTrue(hearingReminderRemoverTest.canHandle(wrapper));
            } else {

                assertFalse(hearingReminderRemoverTest.canHandle(wrapper));
                assertThatThrownBy(() -> hearingReminderRemoverTest.handle(wrapper))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    public void removedHearingReminder() {

        final String expectedJobGroup = "ID_EVENT";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithHearing(
            POSTPONEMENT,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), HEARING_REMINDER.getId())).thenReturn(expectedJobGroup);

        hearingReminderRemoverTest.handle(wrapper);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void doesNotThrowExceptionWhenCannotFindReminder() {

        final String expectedJobGroup = "NOT_EXISTANT";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithHearing(
            POSTPONEMENT,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), HEARING_REMINDER.getId())).thenReturn(expectedJobGroup);

        doThrow(JobNotFoundException.class)
            .when(jobRemover)
            .removeGroup(expectedJobGroup);

        hearingReminderRemoverTest.handle(wrapper);

        verify(jobRemover, times(1)).removeGroup(
            expectedJobGroup
        );
    }

    @Test
    public void canScheduleReturnAlwaysTrue() {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(HEARING_REMINDER);

        assertTrue(hearingReminderRemoverTest.canSchedule(wrapper));
    }

}
