package uk.gov.hmcts.reform.sscs.model.ccd;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

public class EventTypeTest {

    @Test
    public void generateContentKeyForSingleEnumName() {
        assertEquals("status.adjourned", EventType.ADJOURNED.getContentKey());
    }

    @Test
    public void generateContentKeyForDoubleEnumName() {
        assertEquals("status.appealReceived", EventType.APPEAL_RECEIVED.getContentKey());
    }

    @Test
    public void generateContentKeyForMultipleEnumName() {
        assertEquals("status.newHearingBooked", EventType.NEW_HEARING_BOOKED.getContentKey());
    }
}