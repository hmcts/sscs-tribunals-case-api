package uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.HEARING_BOOKED;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.reminder.JobGroupGenerator;

public class JobGroupGeneratorTest {

    private final JobGroupGenerator jobGroupGenerator = new JobGroupGenerator();

    @Test
    public void generatesJobGroup() {
        assertEquals("123_responseReceived", jobGroupGenerator.generate("123", DWP_RESPONSE_RECEIVED.getId()));
        assertEquals("123_dwpUploadResponse", jobGroupGenerator.generate("123", DWP_UPLOAD_RESPONSE.getId()));
        assertEquals("123_hearingBooked", jobGroupGenerator.generate("123", HEARING_BOOKED.getId()));
    }

}
