package uk.gov.hmcts.reform.sscs.helper.service;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;

@Slf4j
public final class CaseHearingLocationHelper {

    private CaseHearingLocationHelper() {

    }

    public static Venue mapVenueDetailsToVenue(VenueDetails venueDetails) {
        return Venue.builder()
                .address(Address.builder()
                        .line1(venueDetails.getVenAddressLine1())
                        .line2(venueDetails.getVenAddressLine2())
                        .town(venueDetails.getVenAddressTown())
                        .county(venueDetails.getVenAddressCounty())
                        .postcode(venueDetails.getVenAddressPostcode())
                        .postcodeLookup(venueDetails.getVenAddressPostcode())
                        .postcodeAddress(venueDetails.getVenAddressPostcode())
                        .build())
                .googleMapLink(venueDetails.getUrl())
                .name(venueDetails.getVenName())
                .build();
    }
}
