package uk.gov.hmcts.reform.sscs.service.venue;

import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;

@RunWith(JUnitParamsRunner.class)
public class VenueRpcDetailsTest {

    @Test
    public void testGetVenueId() {

        VenueDetails venueDetails = VenueDetails.builder().venueId("someId").venName("Some venue Name").regionalProcessingCentre("SSCS Leeds").build();
        VenueRpcDetails venueRpcDetails = new VenueRpcDetails(venueDetails);
        Assert.assertEquals("someId", venueRpcDetails.getVenueId());
    }

    @Test
    public void testGetRpcInCaseDataFormat() {
        VenueDetails venueDetails = VenueDetails.builder().venueId("someId").venName("Some venue Name").regionalProcessingCentre("SSCS Leeds").build();
        VenueRpcDetails venueRpcDetails = new VenueRpcDetails(venueDetails);
        Assert.assertEquals("Leeds", venueRpcDetails.getRpcInCaseDataFormat());
    }


    @NamedParameters("addressFieldCombinations")
    @SuppressWarnings("unused")
    private Object[] allNextHearingTypeParameters() {
        return new Object[] {
            new String[] {"Some Venue Name", "Line 1", "Line 2", "Some town", "Some county", "Some postcode", "Some Venue Name, Line 1, Line 2, Some town, Some county, Some postcode"},
            new String[] {"Some Venue Name", "Line 1", "Line 2", "Some town", "Some county", null, "Some Venue Name, Line 1, Line 2, Some town, Some county"},
            new String[] {"Some Venue Name", "Line 1", "Line 2", "Some town", null, "Some postcode", "Some Venue Name, Line 1, Line 2, Some town, Some postcode"},
            new String[] {"Some Venue Name", "Line 1", "Line 2", "Some town", null, null, "Some Venue Name, Line 1, Line 2, Some town"},
            new String[] {"Some Venue Name", "Line 1", "Line 2", null, "Some county", "Some postcode", "Some Venue Name, Line 1, Line 2, Some county, Some postcode"},
            new String[] {"Some Venue Name", "Line 1", "Line 2", null, "Some county", null, "Some Venue Name, Line 1, Line 2, Some county"},
            new String[] {"Some Venue Name", "Line 1", "Line 2", null, null, "Some postcode", "Some Venue Name, Line 1, Line 2, Some postcode"},
            new String[] {"Some Venue Name", "Line 1", "Line 2", null, null, null, "Some Venue Name, Line 1, Line 2"},
            new String[] {"Some Venue Name", "Line 1", null, "Some town", "Some county", "Some postcode", "Some Venue Name, Line 1, Some town, Some county, Some postcode"},
            new String[] {"Some Venue Name", "Line 1", null, "Some town", "Some county", null, "Some Venue Name, Line 1, Some town, Some county"},
            new String[] {"Some Venue Name", "Line 1", null, "Some town", null, "Some postcode", "Some Venue Name, Line 1, Some town, Some postcode"},
            new String[] {"Some Venue Name", "Line 1", null, "Some town", null, null, "Some Venue Name, Line 1, Some town"},
            new String[] {"Some Venue Name", "Line 1", null, null, "Some county", "Some postcode", "Some Venue Name, Line 1, Some county, Some postcode"},
            new String[] {"Some Venue Name", "Line 1", null, null, "Some county", null, "Some Venue Name, Line 1, Some county"},
            new String[] {"Some Venue Name", "Line 1", null, null, null, "Some postcode", "Some Venue Name, Line 1, Some postcode"},
            new String[] {"Some Venue Name", "Line 1", null, null, null, null, "Some Venue Name, Line 1"},

            new String[] {"Some Venue Name", "", "Line 2", "Some town", "Some county", "Some postcode", "Some Venue Name, Line 2, Some town, Some county, Some postcode"},
            new String[] {"Some Venue Name", "", "Line 2", "Some town", "Some county", "", "Some Venue Name, Line 2, Some town, Some county"},
            new String[] {"Some Venue Name", "", "Line 2", "Some town", "", "Some postcode", "Some Venue Name, Line 2, Some town, Some postcode"},
            new String[] {"Some Venue Name", "", "Line 2", "Some town", "", "", "Some Venue Name, Line 2, Some town"},
            new String[] {"Some Venue Name", "", "Line 2", "", "Some county", "Some postcode", "Some Venue Name, Line 2, Some county, Some postcode"},
            new String[] {"Some Venue Name", "", "Line 2", "", "Some county", "", "Some Venue Name, Line 2, Some county"},
            new String[] {"Some Venue Name", "", "Line 2", "", "", "Some postcode", "Some Venue Name, Line 2, Some postcode"},
            new String[] {"Some Venue Name", "", "Line 2", "", "", "", "Some Venue Name, Line 2"},
            new String[] {"Some Venue Name", "", "", "Some town", "Some county", "Some postcode", "Some Venue Name, Some town, Some county, Some postcode"},
            new String[] {"Some Venue Name", "", "", "Some town", "Some county", "", "Some Venue Name, Some town, Some county"},
            new String[] {"Some Venue Name", "", "", "Some town", "", "Some postcode", "Some Venue Name, Some town, Some postcode"},
            new String[] {"Some Venue Name", "", "", "Some town", "", "", "Some Venue Name, Some town"},
            new String[] {"Some Venue Name", "", "", "", "Some county", "Some postcode", "Some Venue Name, Some county, Some postcode"},
            new String[] {"Some Venue Name", "", "", "", "Some county", "", "Some Venue Name, Some county"},
            new String[] {"Some Venue Name", "", "", "", "", "Some postcode", "Some Venue Name, Some postcode"},
            new String[] {"Some Venue Name", "", "", "", "", "", "Some Venue Name"},
        };
    }

    @Test
    @Parameters(named = "addressFieldCombinations")
    public void testVenueDisplayStringWithoutRpcPrefixForAddressCombinations(String venueName, String line1, String line2, String town, String county, String postcode, String expectedAddressDisplayString) {

        VenueDetails venueDetails = VenueDetails.builder().venueId("someId").venAddressLine1(line1)
            .venAddressLine2(line2).venAddressTown(town).venAddressCounty(county).venAddressPostcode(postcode).venName(venueName).regionalProcessingCentre("SSCS Leeds").build();
        VenueRpcDetails venueRpcDetails = new VenueRpcDetails(venueDetails);
        Assert.assertEquals("Leeds", venueRpcDetails.getRpcInCaseDataFormat());
        Assert.assertEquals("someId", venueRpcDetails.getVenueId());
        Assert.assertNotNull(venueRpcDetails.getVenueDisplayString(true));
        Assert.assertTrue(venueRpcDetails.getVenueDisplayString(true).startsWith("Leeds - "));
        Assert.assertEquals("Leeds - " + expectedAddressDisplayString, venueRpcDetails.getVenueDisplayString(true));

        Assert.assertFalse(venueRpcDetails.getVenueDisplayString(false).startsWith("Leeds - "));
        Assert.assertEquals(expectedAddressDisplayString, venueRpcDetails.getVenueDisplayString(false));

    }

    @Test
    @Parameters(named = "addressFieldCombinations")
    public void testVenueDisplayStringWithRpcPrefixForAddressCombinations(String venueName, String line1, String line2, String town, String county, String postcode, String expectedAddressDisplayString) {

        VenueDetails venueDetails = VenueDetails.builder().venueId("someId").venAddressLine1(line1)
            .venAddressLine2(line2).venAddressTown(town).venAddressCounty(county).venAddressPostcode(postcode).venName(venueName).regionalProcessingCentre("SSCS Leeds").build();
        VenueRpcDetails venueRpcDetails = new VenueRpcDetails(venueDetails);
        Assert.assertEquals("Leeds", venueRpcDetails.getRpcInCaseDataFormat());
        Assert.assertEquals("someId", venueRpcDetails.getVenueId());
        Assert.assertNotNull(venueRpcDetails.getVenueDisplayString(true));
        Assert.assertTrue(venueRpcDetails.getVenueDisplayString(true).startsWith("Leeds - "));
        Assert.assertEquals("Leeds - " + expectedAddressDisplayString, venueRpcDetails.getVenueDisplayString(true));
    }
}
