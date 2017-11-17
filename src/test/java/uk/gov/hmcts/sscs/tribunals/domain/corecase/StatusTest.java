package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import static org.junit.Assert.*;

import org.junit.Test;

public class StatusTest {

    @Test
    public void generateContentKeyForSingleEnumName() {
        assertEquals("status.adjourned", Status.ADJOURNED.getContentKey());
    }

    @Test
    public void generateContentKeyForDoubleEnumName() {
        assertEquals("status.appealReceived", Status.APPEAL_RECEIVED.getContentKey());
    }

    @Test
    public void generateContentKeyForMultipleEnumName() {
        assertEquals("status.newHearingBooked", Status.NEW_HEARING_BOOKED.getContentKey());
    }
}