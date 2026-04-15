package uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationEventTypeTest {
    @Test
    void checkEventsWeDontHandle() {
        String event = "answers_submitted";
        assertThat(NotificationEventType.checkEvent(event)).isFalse();
    }

    @Test
    void checkEventWeDoHandle() {
        final List<String> events = List.of("confirmLapsed", "subscriptionCreated", "hearingReminder", "validAppealCreated",
            "actionHearingRecordingRequest");
        events.forEach(event -> assertThat(NotificationEventType.checkEvent(event)).isTrue());
    }
}