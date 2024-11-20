package uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class NotificationEventTypeTest {
    @Test
    public void checkEventsWeDontHandle() {
        String event = "answers_submitted";
        assertFalse(NotificationEventType.checkEvent(event));
    }

    @Test
    public void checkEventWeDoHandle() {
        String event = "confirmLapsed";
        assertTrue(NotificationEventType.checkEvent(event));

        event = "subscriptionCreated";
        assertTrue(NotificationEventType.checkEvent(event));

        event = "hearingReminder";
        assertTrue(NotificationEventType.checkEvent(event));

        event = "validAppealCreated";
        assertTrue(NotificationEventType.checkEvent(event));

        event = "actionHearingRecordingRequest";
        assertTrue(NotificationEventType.checkEvent(event));


    }
}
