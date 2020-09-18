package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.service.venue.VenueRpcDetailsService;

@RunWith(JUnitParamsRunner.class)
public class AdjournCaseCcdServiceTest {

    private AdjournCaseCcdService service;

    @Mock
    private VenueDataLoader venueDataLoader;

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        service = new AdjournCaseCcdService(new VenueRpcDetailsService(venueDataLoader));
        Map<String, VenueDetails> venueDetailsMap = new HashMap<>();
        venueDetailsMap.put("venue1Rpc1", VenueDetails.builder().venueId("venue1Rpc1").active("Yes").venName("Zebedee Venue 1")
                .regionalProcessingCentre("SSCS Rpc 1").build());
        venueDetailsMap.put("venue2Rpc1", VenueDetails.builder().venueId("venue2Rpc1").active("Yes").venName("Apple Venue 2")
                .regionalProcessingCentre("SSCS Rpc 1").build());
        venueDetailsMap.put("venue3Rpc2", VenueDetails.builder().venueId("venue3Rpc2").active("Yes").venName("Zebedee Venue 3")
                .regionalProcessingCentre("SSCS Rpc 2").build());
        venueDetailsMap.put("venue4Rpc2", VenueDetails.builder().venueId("venue4Rpc2").active("Yes").venName("Apple Venue 4")
                .regionalProcessingCentre("SSCS Rpc 2").build());
        venueDetailsMap.put("venue5Rpc3", VenueDetails.builder().venueId("venue5Rpc3").active("Yes").venName("Zebedee Venue 5")
                .regionalProcessingCentre("SSCS Rpc 3").build());
        venueDetailsMap.put("venue6Rpc3", VenueDetails.builder().venueId("venue6Rpc3").active("Yes").venName("Apple Venue 6")
                .regionalProcessingCentre("SSCS Rpc 3").build());
        venueDetailsMap.put("venue7Rpc3", VenueDetails.builder().venueId("venue7Rpc3").active("No").venName("Apple Venue 7")
            .regionalProcessingCentre("SSCS Rpc 3").build());
        Mockito.when(venueDataLoader.getVenueDetailsMap()).thenReturn(venueDetailsMap);
    }

    @Test
    public void testValidRpc1() {
        DynamicList dynamicList = service.getVenueDynamicListForRpcName("Rpc 1");
        assertNotNull(dynamicList);
        assertNotNull(dynamicList.getListItems());
        assertFalse(dynamicList.getListItems().isEmpty());
        assertEquals(6,  dynamicList.getListItems().size());
        final DynamicListItem first = dynamicList.getListItems().get(0);
        final DynamicListItem second = dynamicList.getListItems().get(1);
        final DynamicListItem third = dynamicList.getListItems().get(2);
        final DynamicListItem fourth = dynamicList.getListItems().get(3);
        final DynamicListItem fifth = dynamicList.getListItems().get(4);
        final DynamicListItem sixth = dynamicList.getListItems().get(5);

        // Assert the matching venues are in the correct order, at the
        // top - in alphabetical order of venue name
        assertEquals("venue2Rpc1", first.getCode());
        assertEquals("venue1Rpc1", second.getCode());

        // Assert the remaining venues are in the correct order, with the non-matching RPC venues
        // below, in alphetical order of RPC name, venue name.
        assertEquals("venue4Rpc2", third.getCode());
        assertEquals("venue3Rpc2", fourth.getCode());
        assertEquals("venue6Rpc3", fifth.getCode());
        assertEquals("venue5Rpc3", sixth.getCode());

        // Assert that the matching venue names do not have the RPC prefix
        assertEquals("Apple Venue 2", first.getLabel());
        assertEquals("Zebedee Venue 1", second.getLabel());

        // Assert the remaining venues do have the RPC prefix
        assertEquals("Rpc 2 - Apple Venue 4", third.getLabel());
        assertEquals("Rpc 2 - Zebedee Venue 3", fourth.getLabel());
        assertEquals("Rpc 3 - Apple Venue 6", fifth.getLabel());
        assertEquals("Rpc 3 - Zebedee Venue 5", sixth.getLabel());
    }

    @Test
    public void testValidRpc2() {
        DynamicList dynamicList = service.getVenueDynamicListForRpcName("Rpc 2");
        assertNotNull(dynamicList);
        assertNotNull(dynamicList.getListItems());
        assertFalse(dynamicList.getListItems().isEmpty());
        assertEquals(6,  dynamicList.getListItems().size());
        final DynamicListItem first = dynamicList.getListItems().get(0);
        final DynamicListItem second = dynamicList.getListItems().get(1);
        final DynamicListItem third = dynamicList.getListItems().get(2);
        final DynamicListItem fourth = dynamicList.getListItems().get(3);
        final DynamicListItem fifth = dynamicList.getListItems().get(4);
        final DynamicListItem sixth = dynamicList.getListItems().get(5);

        // Assert the matching venues are in the correct order, at the
        // top - in alphabetical order of venue name
        assertEquals("venue4Rpc2", first.getCode());
        assertEquals("venue3Rpc2", second.getCode());

        // Assert the remaining venues are in the correct order, with the non-matching RPC venues
        // below, in alphetical order of RPC name, venue name.
        assertEquals("venue2Rpc1", third.getCode());
        assertEquals("venue1Rpc1", fourth.getCode());
        assertEquals("venue6Rpc3", fifth.getCode());
        assertEquals("venue5Rpc3", sixth.getCode());

        // Assert that the matching venue names do not have the RPC prefix
        assertEquals("Apple Venue 4", first.getLabel());
        assertEquals("Zebedee Venue 3", second.getLabel());

        // Assert the remaining venues do have the RPC prefix
        assertEquals("Rpc 1 - Apple Venue 2", third.getLabel());
        assertEquals("Rpc 1 - Zebedee Venue 1", fourth.getLabel());
        assertEquals("Rpc 3 - Apple Venue 6", fifth.getLabel());
        assertEquals("Rpc 3 - Zebedee Venue 5", sixth.getLabel());
    }

    /**
     * Should never happen that an invalid RPC is passed in, as the parameter to be passed
     * in is obtained from the list of available venues.
     * But just in case....
     */
    @Test
    public void testInValidRpc4() {

        // If an invalid rpc is passed in, we just expect the entire list to be returned
        // with all venues prefixed by rpc

        DynamicList dynamicList = service.getVenueDynamicListForRpcName("Rpc 4");
        assertNotNull(dynamicList);
        assertNotNull(dynamicList.getListItems());
        assertFalse(dynamicList.getListItems().isEmpty());
        assertEquals(6,  dynamicList.getListItems().size());
        final DynamicListItem first = dynamicList.getListItems().get(0);
        final DynamicListItem second = dynamicList.getListItems().get(1);
        final DynamicListItem third = dynamicList.getListItems().get(2);
        final DynamicListItem fourth = dynamicList.getListItems().get(3);
        final DynamicListItem fifth = dynamicList.getListItems().get(4);
        final DynamicListItem sixth = dynamicList.getListItems().get(5);

        // Assert the venuess are in the correct order, with the non-matching RPC venues
        // below, in alphetical order of RPC name, venue name.
        assertEquals("venue2Rpc1", first.getCode());
        assertEquals("venue1Rpc1", second.getCode());
        assertEquals("venue4Rpc2", third.getCode());
        assertEquals("venue3Rpc2", fourth.getCode());
        assertEquals("venue6Rpc3", fifth.getCode());
        assertEquals("venue5Rpc3", sixth.getCode());

        // Assert the all venues do have the RPC prefix
        assertEquals("Rpc 1 - Apple Venue 2", first.getLabel());
        assertEquals("Rpc 1 - Zebedee Venue 1", second.getLabel());
        assertEquals("Rpc 2 - Apple Venue 4", third.getLabel());
        assertEquals("Rpc 2 - Zebedee Venue 3", fourth.getLabel());
        assertEquals("Rpc 3 - Apple Venue 6", fifth.getLabel());
        assertEquals("Rpc 3 - Zebedee Venue 5", sixth.getLabel());
    }
}
