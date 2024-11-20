package uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import org.junit.jupiter.api.Test;

public class JobGroupGeneratorTest {

    private final JobGroupGenerator jobGroupGenerator = new JobGroupGenerator();

    @Test
    public void generatesJobGroup() {
        assertEquals("123_responseReceived", jobGroupGenerator.generate("123", DWP_RESPONSE_RECEIVED.getId()));
        assertEquals("123_dwpUploadResponse", jobGroupGenerator.generate("123", DWP_UPLOAD_RESPONSE.getId()));
        assertEquals("123_hearingBooked", jobGroupGenerator.generate("123", HEARING_BOOKED.getId()));
    }

}
