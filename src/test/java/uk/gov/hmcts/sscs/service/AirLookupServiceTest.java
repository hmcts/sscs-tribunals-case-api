package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class AirLookupServiceTest {
    AirLookupService airLookupService;

    private static String DEFAULT_VENUE_NAME = "Birmingham";

    @Before
    public void setUp() {
        airLookupService = new AirLookupService();
        airLookupService.init();
    }

    @Test
    public void lookupPostcode() {
        String adminGroup = airLookupService.lookupRegionalCentre("BR3 8JK");
        assertEquals("Sutton", adminGroup);
    }

    @Test
    public void lookupPostcodeLowerCase() {
        String adminGroup = airLookupService.lookupRegionalCentre("br3 8JK");
        assertEquals("Sutton", adminGroup);
    }

    @Test
    public void lookupPostcodeNotThere() {
        String adminGroup = airLookupService.lookupRegionalCentre("aa1 1aa");
        assertEquals(null, adminGroup);
    }

    @Test
    public void lookupLastValue() {
        String adminGroup = airLookupService.lookupRegionalCentre("ze3 4gh");
        assertEquals("Glasgow", adminGroup);
    }

    @Test
    public void lookupFirstValue() {
        String adminGroup = airLookupService.lookupRegionalCentre("ab1 2gh");
        assertEquals("Glasgow", adminGroup);
    }

    //Tests for parsing the venue
    @Test
    public void checkForPip() {
        String cellWithPip = "Bristol Magistrates- 03 - PIP/DLA";
        assertTrue(airLookupService.hasPip(cellWithPip));
    }

    //Tests for the venue ID lookup
    @Test
    public void checkAirPostcodeWithNoPipReturnsBirmingham() {
        assertEquals(DEFAULT_VENUE_NAME, airLookupService.lookupAirVenueNameByPostCode("ec1m"));
    }

    @Test
    public void checkVenueIdForPostCodeWithNoPip() {
        assertEquals(24, airLookupService.lookupVenueId("ec1m"));
    }

    @Test
    public void checkVenueIdForValidPostCode() {
        assertEquals(1223, airLookupService.lookupVenueId("NN85"));
    }

    @Test
    public void lookupShortPostcode() {
        String adminGroup = airLookupService.lookupRegionalCentre("l2 1RT");
        assertEquals("Liverpool", adminGroup);
    }

    @Test
    public void lookupLongPostcode() {
        String adminGroup = airLookupService.lookupRegionalCentre("HP27 1RT");
        assertEquals("Birmingham", adminGroup);
    }

    @Test
    public void lookupShortPostcodeNoSpace() {
        String adminGroup = airLookupService.lookupRegionalCentre("l21RT");
        assertEquals("Liverpool", adminGroup);
    }


    @Test
    public void lookupLongPostcodeNoSpace() {
        String adminGroup = airLookupService.lookupRegionalCentre("HP271RT");
        assertEquals("Birmingham", adminGroup);
    }

    @Test
    public void lookupLongPostcodeOutcode() {
        String adminGroup = airLookupService.lookupRegionalCentre("HP27");
        assertEquals("Birmingham", adminGroup);
    }

    //Tests for parsing the venue
    @Test
    public void checkForPip() {
        String cellWithPip = "Bristol Magistrates- 03 - PIP/DLA";
        assertTrue(airLookupService.hasPip(cellWithPip));
    }

    //Tests for the venue ID lookup
    @Test
    public void checkAirPostcodeWithNoPipReturnsBirmingham() {
        assertEquals(DEFAULT_VENUE_NAME, airLookupService.lookupAirVenueNameByPostCode("ec1m"));
    }

    @Test
    public void checkVenueIdForPostCodeWithNoPip() {
        assertEquals(24, airLookupService.lookupVenueId("ec1m"));
    }

    @Test
    public void checkVenueIdForValidPostCode() {
        assertEquals(1223, airLookupService.lookupVenueId("NN85"));
    }
}
