package uk.gov.hmcts.reform.sscs.service.venue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;

public class VenueRpcDetailsTest {

    @ParameterizedTest
    public void testGetVenueId() {

        VenueDetails venueDetails = VenueDetails.builder().venueId("someId").venName("Some venue Name").regionalProcessingCentre("SSCS Leeds").build();
        VenueRpcDetails venueRpcDetails = new VenueRpcDetails(venueDetails);
        Assertions.assertEquals("someId", venueRpcDetails.getVenueId());
    }

    @ParameterizedTest
    public void testGetRpcInCaseDataFormat() {
        VenueDetails venueDetails = VenueDetails.builder().venueId("someId").venName("Some venue Name").regionalProcessingCentre("SSCS Leeds").build();
        VenueRpcDetails venueRpcDetails = new VenueRpcDetails(venueDetails);
        Assertions.assertEquals("Leeds", venueRpcDetails.getRpcInCaseDataFormat());
    }


    @SuppressWarnings("unused")
    private static Object[] allNextHearingTypeParameters() {
        return new Object[]{
            new String[]{"Some Venue Name", "Line 1", "Line 2", "Some town", "Some county", "Some postcode", "Some Venue Name, Line 1, Line 2, Some town, Some county, Some postcode"},
            new String[]{"Some Venue Name", "Line 1", "Line 2", "Some town", "Some county", null, "Some Venue Name, Line 1, Line 2, Some town, Some county"},
            new String[]{"Some Venue Name", "Line 1", "Line 2", "Some town", null, "Some postcode", "Some Venue Name, Line 1, Line 2, Some town, Some postcode"},
            new String[]{"Some Venue Name", "Line 1", "Line 2", "Some town", null, null, "Some Venue Name, Line 1, Line 2, Some town"},
            new String[]{"Some Venue Name", "Line 1", "Line 2", null, "Some county", "Some postcode", "Some Venue Name, Line 1, Line 2, Some county, Some postcode"},
            new String[]{"Some Venue Name", "Line 1", "Line 2", null, "Some county", null, "Some Venue Name, Line 1, Line 2, Some county"},
            new String[]{"Some Venue Name", "Line 1", "Line 2", null, null, "Some postcode", "Some Venue Name, Line 1, Line 2, Some postcode"},
            new String[]{"Some Venue Name", "Line 1", "Line 2", null, null, null, "Some Venue Name, Line 1, Line 2"},
            new String[]{"Some Venue Name", "Line 1", null, "Some town", "Some county", "Some postcode", "Some Venue Name, Line 1, Some town, Some county, Some postcode"},
            new String[]{"Some Venue Name", "Line 1", null, "Some town", "Some county", null, "Some Venue Name, Line 1, Some town, Some county"},
            new String[]{"Some Venue Name", "Line 1", null, "Some town", null, "Some postcode", "Some Venue Name, Line 1, Some town, Some postcode"},
            new String[]{"Some Venue Name", "Line 1", null, "Some town", null, null, "Some Venue Name, Line 1, Some town"},
            new String[]{"Some Venue Name", "Line 1", null, null, "Some county", "Some postcode", "Some Venue Name, Line 1, Some county, Some postcode"},
            new String[]{"Some Venue Name", "Line 1", null, null, "Some county", null, "Some Venue Name, Line 1, Some county"},
            new String[]{"Some Venue Name", "Line 1", null, null, null, "Some postcode", "Some Venue Name, Line 1, Some postcode"},
            new String[]{"Some Venue Name", "Line 1", null, null, null, null, "Some Venue Name, Line 1"},

            new String[]{"Some Venue Name", "", "Line 2", "Some town", "Some county", "Some postcode", "Some Venue Name, Line 2, Some town, Some county, Some postcode"},
            new String[]{"Some Venue Name", "", "Line 2", "Some town", "Some county", "", "Some Venue Name, Line 2, Some town, Some county"},
            new String[]{"Some Venue Name", "", "Line 2", "Some town", "", "Some postcode", "Some Venue Name, Line 2, Some town, Some postcode"},
            new String[]{"Some Venue Name", "", "Line 2", "Some town", "", "", "Some Venue Name, Line 2, Some town"},
            new String[]{"Some Venue Name", "", "Line 2", "", "Some county", "Some postcode", "Some Venue Name, Line 2, Some county, Some postcode"},
            new String[]{"Some Venue Name", "", "Line 2", "", "Some county", "", "Some Venue Name, Line 2, Some county"},
            new String[]{"Some Venue Name", "", "Line 2", "", "", "Some postcode", "Some Venue Name, Line 2, Some postcode"},
            new String[]{"Some Venue Name", "", "Line 2", "", "", "", "Some Venue Name, Line 2"},
            new String[]{"Some Venue Name", "", "", "Some town", "Some county", "Some postcode", "Some Venue Name, Some town, Some county, Some postcode"},
            new String[]{"Some Venue Name", "", "", "Some town", "Some county", "", "Some Venue Name, Some town, Some county"},
            new String[]{"Some Venue Name", "", "", "Some town", "", "Some postcode", "Some Venue Name, Some town, Some postcode"},
            new String[]{"Some Venue Name", "", "", "Some town", "", "", "Some Venue Name, Some town"},
            new String[]{"Some Venue Name", "", "", "", "Some county", "Some postcode", "Some Venue Name, Some county, Some postcode"},
            new String[]{"Some Venue Name", "", "", "", "Some county", "", "Some Venue Name, Some county"},
            new String[]{"Some Venue Name", "", "", "", "", "Some postcode", "Some Venue Name, Some postcode"},
            new String[]{"Some Venue Name", "", "", "", "", "", "Some Venue Name"},
        };
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    public void testVenueDisplayStringWithoutRpcPrefixForAddressCombinations(String venueName, String line1, String line2, String town, String county, String postcode, String expectedAddressDisplayString) {

        VenueDetails venueDetails = VenueDetails.builder().venueId("someId").venAddressLine1(line1)
            .venAddressLine2(line2).venAddressTown(town).venAddressCounty(county).venAddressPostcode(postcode).venName(venueName).regionalProcessingCentre("SSCS Leeds").build();
        VenueRpcDetails venueRpcDetails = new VenueRpcDetails(venueDetails);
        Assertions.assertEquals("Leeds", venueRpcDetails.getRpcInCaseDataFormat());
        Assertions.assertEquals("someId", venueRpcDetails.getVenueId());
        Assertions.assertNotNull(venueRpcDetails.getVenueDisplayString(true));
        Assertions.assertTrue(venueRpcDetails.getVenueDisplayString(true).startsWith("Leeds - "));
        Assertions.assertEquals("Leeds - " + expectedAddressDisplayString, venueRpcDetails.getVenueDisplayString(true));

        Assertions.assertFalse(venueRpcDetails.getVenueDisplayString(false).startsWith("Leeds - "));
        Assertions.assertEquals(expectedAddressDisplayString, venueRpcDetails.getVenueDisplayString(false));

    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    public void testVenueDisplayStringWithRpcPrefixForAddressCombinations(String venueName, String line1, String line2, String town, String county, String postcode, String expectedAddressDisplayString) {

        VenueDetails venueDetails = VenueDetails.builder().venueId("someId").venAddressLine1(line1)
            .venAddressLine2(line2).venAddressTown(town).venAddressCounty(county).venAddressPostcode(postcode).venName(venueName).regionalProcessingCentre("SSCS Leeds").build();
        VenueRpcDetails venueRpcDetails = new VenueRpcDetails(venueDetails);
        Assertions.assertEquals("Leeds", venueRpcDetails.getRpcInCaseDataFormat());
        Assertions.assertEquals("someId", venueRpcDetails.getVenueId());
        Assertions.assertNotNull(venueRpcDetails.getVenueDisplayString(true));
        Assertions.assertTrue(venueRpcDetails.getVenueDisplayString(true).startsWith("Leeds - "));
        Assertions.assertEquals("Leeds - " + expectedAddressDisplayString, venueRpcDetails.getVenueDisplayString(true));
    }
}
