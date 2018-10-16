package uk.gov.hmcts.reform.sscs.service;

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

    @Test
    public void lookupPostcodePostcodesThatWereMissing() {
        String adminGroup = airLookupService.lookupRegionalCentre("bl11");
        assertEquals("Liverpool", adminGroup);

        adminGroup = airLookupService.lookupRegionalCentre("bl78");
        assertEquals("Liverpool", adminGroup);

        adminGroup = airLookupService.lookupRegionalCentre("s31");
        assertEquals("Leeds", adminGroup);

        adminGroup = airLookupService.lookupRegionalCentre("s30");
        assertEquals("Leeds", adminGroup);
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
        //n1w1 is a sorting office
        assertEquals(DEFAULT_VENUE_NAME, airLookupService.lookupAirVenueNameByPostCode("n1w1"));
    }

    @Test
    public void checkVenueIdForPostCodeWithNoPip() {
        assertEquals(24, airLookupService.lookupVenueId("n1w1"));
    }

    @Test
    public void checkVenueIdForValidPostCode() {
        assertEquals(1223, airLookupService.lookupVenueId("NN85"));
    }

}
