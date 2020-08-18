package uk.gov.hmcts.reform.sscs.service.venue;

import uk.gov.hmcts.reform.sscs.model.VenueDetails;

/**
 * Wrapper around VenueDetails
 */
public class VenueRpcDetails {

    private VenueDetails venueDetails;
    private String rpc;

    public VenueRpcDetails(VenueDetails venueDetails) {
        this.venueDetails = venueDetails;
        this.rpc = venueDetails.getRegionalProcessingCentre().substring(5);
    }

    public String getRpcInCaseDataFormat() {
        return rpc;
    }

    public String getVenueId() {
        return venueDetails.getVenueId();
    }

    public String getVenueDisplayString(boolean prefixWithRpc) {
        return (prefixWithRpc ? (getRpcInCaseDataFormat() + " - " ) : "") + venueDetails.getVenName() + ", "
            + venueDetails.getVenAddressLine1() + ", "
            + venueDetails.getVenAddressLine2() + ", "
            + venueDetails.getVenAddressTown() + ", "
            + venueDetails.getVenAddressCounty() + ", "
            + venueDetails.getVenAddressPostcode();
    }
}
