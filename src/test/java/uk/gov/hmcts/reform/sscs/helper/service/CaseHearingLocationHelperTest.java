package uk.gov.hmcts.reform.sscs.helper.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;

class CaseHearingLocationHelperTest {

    @DisplayName("When an valid venue is given to findVenue, mapVenueDetailsToVenue correctly maps those venue details to venue")
    @Test
    void testMapVenueDetailsToVenue() {

        final String epimsId = "123";
        VenueDetails venueDetails = VenueDetails.builder()
                .venueId(epimsId)
                .venName("venueName")
                .url("http://test.com")
                .venAddressLine1("adrLine1")
                .venAddressLine2("adrLine2")
                .venAddressTown("adrTown")
                .venAddressCounty("adrCounty")
                .venAddressPostcode("adrPostcode")
                .regionalProcessingCentre("regionalProcessingCentre")
                .build();


        Venue venue = CaseHearingLocationHelper.mapVenueDetailsToVenue(venueDetails);

        // then
        assertThat(venue).isNotNull();
        assertThat(venue.getName()).isEqualTo(venueDetails.getVenName());
        assertThat(venue.getGoogleMapLink()).isEqualTo(venueDetails.getUrl());

        Address expectedAddress = Address.builder()
                .line1(venueDetails.getVenAddressLine1())
                .line2(venueDetails.getVenAddressLine2())
                .town(venueDetails.getVenAddressTown())
                .county(venueDetails.getVenAddressCounty())
                .postcode(venueDetails.getVenAddressPostcode())
                .postcodeLookup(venueDetails.getVenAddressPostcode())
                .postcodeAddress(venueDetails.getVenAddressPostcode())
                .build();

        assertThat(venue.getAddress()).isEqualTo(expectedAddress);
    }
}
