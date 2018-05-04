package uk.gov.hmcts.sscs.model.ccd;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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