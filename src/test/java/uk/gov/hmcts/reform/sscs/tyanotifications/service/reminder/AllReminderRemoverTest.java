package uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobNotFoundException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobRemover;
import uk.gov.hmcts.reform.sscs.tyanotifications.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;

public class AllReminderRemoverTest {

    private JobGroupGenerator jobGroupGenerator = mock(JobGroupGenerator.class);
    private JobRemover jobRemover = mock(JobRemover.class);

    private AllReminderRemover allReminderRemover;
    private static final List<NotificationEventType> ALL_UNHANDLED_EVENTS =
        Arrays.stream(NotificationEventType.values())
            .filter(f -> (!f.equals(APPEAL_LAPSED) && !f.equals(DWP_APPEAL_LAPSED)
                && !f.equals(HMCTS_APPEAL_LAPSED) && !f.equals(APPEAL_WITHDRAWN)
                && !f.equals(ADMIN_APPEAL_WITHDRAWN) && !f.equals(APPEAL_DORMANT)
                && !f.equals(DECISION_ISSUED)
                && !f.equals(ISSUE_FINAL_DECISION)))
            .collect(Collectors.toCollection(ArrayList::new));

    @BeforeEach
    public void setup() {
        allReminderRemover = new AllReminderRemover(
            jobGroupGenerator,
            jobRemover
        );
    }

    @ParameterizedTest
    public void canHandleEvent() {

        for (NotificationEventType eventType : NotificationEventType.values()) {

            CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(eventType);

            if (!ALL_UNHANDLED_EVENTS.contains(eventType)) {
                assertTrue(allReminderRemover.canHandle(wrapper));
            } else {
                assertFalse(allReminderRemover.canHandle(wrapper));
                assertThatThrownBy(() -> allReminderRemover.handle(wrapper))
                    .hasMessage("cannot handle ccdResponse")
                    .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @ParameterizedTest
    public void removedHearingReminder() {

        final String expectedHearingJobGroup = "ID_EVENT_HEARING";
        final String expectedEvidenceJobGroup = "ID_EVENT_EVIDENCE";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithHearing(
            APPEAL_LAPSED,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), HEARING_REMINDER.getId())).thenReturn(expectedHearingJobGroup);

        when(jobGroupGenerator.generate(wrapper.getCaseId(), EVIDENCE_RECEIVED.getId())).thenReturn(expectedEvidenceJobGroup);

        allReminderRemover.handle(wrapper);

        verify(jobRemover, times(1)).removeGroup(expectedHearingJobGroup);
        verify(jobRemover, times(1)).removeGroup(expectedEvidenceJobGroup);
    }

    @ParameterizedTest
    public void removedHearingReminderForDwpLapsed() {

        final String expectedHearingJobGroup = "ID_EVENT_HEARING";
        final String expectedEvidenceJobGroup = "ID_EVENT_EVIDENCE";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithHearing(
            DWP_APPEAL_LAPSED,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), HEARING_REMINDER.getId())).thenReturn(expectedHearingJobGroup);

        when(jobGroupGenerator.generate(wrapper.getCaseId(), EVIDENCE_RECEIVED.getId())).thenReturn(expectedEvidenceJobGroup);

        allReminderRemover.handle(wrapper);

        verify(jobRemover, times(1)).removeGroup(expectedHearingJobGroup);
        verify(jobRemover, times(1)).removeGroup(expectedEvidenceJobGroup);
    }

    @ParameterizedTest
    public void doesNotThrowExceptionWhenCannotFindReminder() {

        final String expectedJobGroup = "NOT_EXISTANT";

        String hearingDate = "2018-01-01";
        String hearingTime = "14:01:18";

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapperWithHearing(
            APPEAL_LAPSED,
            hearingDate,
            hearingTime
        );

        when(jobGroupGenerator.generate(wrapper.getCaseId(), HEARING_REMINDER.getId())).thenReturn(expectedJobGroup);
        when(jobGroupGenerator.generate(wrapper.getCaseId(), EVIDENCE_RECEIVED.getId())).thenReturn(expectedJobGroup);

        doThrow(JobNotFoundException.class)
            .when(jobRemover)
            .removeGroup(expectedJobGroup);

        allReminderRemover.handle(wrapper);

        verify(jobRemover, times(2)).removeGroup(
            expectedJobGroup
        );
    }

    @ParameterizedTest
    public void canScheduleReturnAlwaysTrue() {

        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(HEARING_REMINDER);

        assertTrue(allReminderRemover.canSchedule(wrapper));
    }

    @ParameterizedTest
    @MethodSource("unhandledNotificationEventTypes")
    public void willNotHandleNotifications(NotificationEventType notificationEventType) {
        CcdNotificationWrapper wrapper = SscsCaseDataUtils.buildBasicCcdNotificationWrapper(notificationEventType);

        assertFalse(allReminderRemover.canHandle(wrapper));
    }

    @SuppressWarnings({"Indentation", "unused"})
    private static Object[] unhandledNotificationEventTypes() {
        Object[] result = new Object[ALL_UNHANDLED_EVENTS.size()];
        int i = 0;
        for (NotificationEventType event : ALL_UNHANDLED_EVENTS) {
            result[i++] = new Object[]{event.name()};
        }
        return result;
    }

}
