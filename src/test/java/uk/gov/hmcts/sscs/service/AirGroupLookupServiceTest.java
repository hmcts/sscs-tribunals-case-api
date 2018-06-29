package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class AirGroupLookupServiceTest {
    AirLookupService airLookupService;

    @Before
    public void setUp() {
        airLookupService = new AirLookupService();
        airLookupService.init();
    }

    @Test
    public void lookupPostcode() {
        String adminGroup = airLookupService.lookupRegionalCentre("BR3");
        assertEquals("Sutton", adminGroup);
    }

    @Test
    public void lookupPostcodeLowerCase() {
        String adminGroup = airLookupService.lookupRegionalCentre("br3");
        assertEquals("Sutton", adminGroup);
    }

    @Test
    public void lookupPostcodeNotThere() {
        String adminGroup = airLookupService.lookupRegionalCentre("aa1");
        assertEquals(null, adminGroup);
    }

    @Test
    public void lookupLastValue() {
        String adminGroup = airLookupService.lookupRegionalCentre("ze3");
        assertEquals("Glasgow", adminGroup);
    }

    @Test
    public void lookupFirstValue() {
        String adminGroup = airLookupService.lookupRegionalCentre("ab1");
        assertEquals("Glasgow", adminGroup);
    }
}
