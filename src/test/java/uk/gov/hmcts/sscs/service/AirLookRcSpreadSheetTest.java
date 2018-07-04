package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertTrue;

import java.util.*;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests to look at the integrity of the data in a business spreadsheet
 * and our list of venues.
 */
public class AirLookRcSpreadSheetTest {
    AirLookupService airLookupService;
    Map<String, String> lookupData;
    Map<String, String> venueData;
    Map<String, Integer> lookupVenueIdByAirLookupName;

    @Before
    public void setUp() {
        airLookupService = new AirLookupService();
        airLookupService.init();

        lookupData = airLookupService.getLookupRegionalCentreByPostCode();
        venueData = airLookupService.getLookupAirVenueNameByPostCode();
        lookupVenueIdByAirLookupName = airLookupService.getLookupVenueIdByAirVenueName();
    }


    @Test
    public void testAllPostcodesHavePip() {
        List<String> missingPipVenuePostcodes = new ArrayList<>();

        Set<String> rcKeys = lookupData.keySet();
        Iterator<String> iterator = rcKeys.iterator();
        while (iterator.hasNext()) {
            String postcode = iterator.next();
            if (!venueData.keySet().contains(postcode) && !lookupData.get(postcode).equals("Glasgow")) {
                missingPipVenuePostcodes.add(postcode);
            }
        }
        assertTrue(missingPipVenuePostcodes + " of " + lookupData.size()
                        + " post codes do not have a PIP entry: "
                        + Arrays.toString(missingPipVenuePostcodes.toArray()),
                missingPipVenuePostcodes.size() == 0);
    }

    @Test
    public void testAllPipsMapToVenueId() {
        Set<String> postCodesForVenueNames = venueData.keySet();

        Set<String> missingAirLookupNames = new HashSet<>();
        Set<String> workingAirLookupNames = new HashSet<>();

        Iterator postCodeIterator = postCodesForVenueNames.iterator();
        while (postCodeIterator.hasNext()) {
            Object postCode = postCodeIterator.next();
            String airLookupName = venueData.get(postCode);
            Integer venueId = lookupVenueIdByAirLookupName.get(airLookupName);

            if (venueId == null || venueId.intValue() == 0) {
                missingAirLookupNames.add(airLookupName);
            } else {
                workingAirLookupNames.add(airLookupName);
            }
        }

        assertTrue(missingAirLookupNames.size() + " airLookupNames don't map to a venueId"
                + "\nMissing: " + Arrays.toString(missingAirLookupNames.toArray())
                + "\nWorking: " + Arrays.toString(workingAirLookupNames.toArray()),
                missingAirLookupNames.size() == 0);
    }
}
