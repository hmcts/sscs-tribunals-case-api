package uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_RESPOND;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.tyanotifications.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.extractor.DwpResponseReceivedDateExtractor;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class EvidenceReminderTest {

    @Mock
    private DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;

    @Mock
    private JobGroupGenerator jobGroupGenerator;

    @Mock
    private JobScheduler jobScheduler;

    private EvidenceReminder evidenceReminder;

    @Before
    public void setup() {
        openMocks(this);
        evidenceReminder = new EvidenceReminder(
            dwpResponseReceivedDateExtractor,
            jobGroupGenerator,
            jobScheduler,
            172800
        );
    }

    @Test
    public void canHandleEvent() {
        final List<NotificationEventType> handledEventTypes = Arrays.asList(DWP_RESPONSE_RECEIVED, DWP_UPLOAD_RESPONSE);

        for (NotificationEventType eventType : NotificationEventType.values()) {

            CcdNotificationWrapper ccdResponse = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(eventType);

            if (handledEventTypes.contains(eventType)) {
                assertTrue(evidenceReminder.canHandle(ccdResponse));
            } else {

                assertFalse(evidenceReminder.canHandle(ccdResponse));
                assertThatThrownBy(() -> evidenceReminder.handle(ccdResponse))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    @Parameters({"DWP_RESPONSE_RECEIVED", "DWP_UPLOAD_RESPONSE"})
    public void schedulesReminder(NotificationEventType eventType) {

        final String expectedJobGroup = "ID_EVENT";
        final String expectedTriggerAt = "2018-01-03T14:01:18Z[Europe/London]";

        ZonedDateTime dwpResponseReceivedDate = ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithEvent(
            eventType,
            DWP_RESPOND,
            dwpResponseReceivedDate.toString()
        );

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.of(dwpResponseReceivedDate));
        when(jobGroupGenerator.generate(wrapper.getCaseId(), EVIDENCE_REMINDER.getId())).thenReturn(expectedJobGroup);

        evidenceReminder.handle(wrapper);

        ArgumentCaptor<Job<String>> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(1)).schedule(
            jobCaptor.capture()
        );

        Job<String> job = jobCaptor.getValue();
        assertEquals(expectedJobGroup, job.group);
        assertEquals(EVIDENCE_REMINDER.getId(), job.name);
        assertEquals(SscsCaseDataUtils.CASE_ID, job.payload);
        assertEquals(expectedTriggerAt, job.triggerAt.toString());
    }

    @Test
    public void canNotSchedulesReminderWhenReminderDateIsNull() {

        final String expectedJobGroup = "ID_EVENT";

        ZonedDateTime dwpResponseReceivedDate = ZonedDateTime.parse("2018-01-01T14:01:18Z[Europe/London]");

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithEvent(
            DWP_RESPONSE_RECEIVED,
            DWP_RESPOND,
            dwpResponseReceivedDate.toString()
        );

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.empty());
        when(jobGroupGenerator.generate(wrapper.getCaseId(), EVIDENCE_REMINDER.getId())).thenReturn(expectedJobGroup);

        evidenceReminder.handle(wrapper);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);

        verify(jobScheduler, times(0)).schedule(
            jobCaptor.capture()
        );

        assertTrue(jobCaptor.getAllValues().isEmpty());
    }

    @Test(expected = Exception.class)
    public void canScheduleReturnFalseWhenDwpResponseReceivedThrowError() {

        CcdNotificationWrapper wrapper = null;

        assertFalse(evidenceReminder.canSchedule(wrapper));
    }

    @Test
    public void canScheduleReturnFalseWhenDwpResponseReceivedDateNotPresent() {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(DWP_RESPONSE_RECEIVED);

        when(dwpResponseReceivedDateExtractor.extract(wrapper.getNewSscsCaseData())).thenReturn(Optional.empty());

        assertFalse(evidenceReminder.canSchedule(wrapper));
    }

}
