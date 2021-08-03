package uk.gov.hmcts.reform.sscs.service.venue;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;

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
        return (prefixWithRpc ? (getRpcInCaseDataFormat() + " - ") : "") + venueDetails.getVenName()
            + getAddressComponent(venueDetails.getVenAddressLine1())
            + getAddressComponent(venueDetails.getVenAddressLine2())
            + getAddressComponent(venueDetails.getVenAddressTown())
            + getAddressComponent(venueDetails.getVenAddressCounty())
            + getAddressComponent(venueDetails.getVenAddressPostcode());
    }

    private String getAddressComponent(String component) {
        return StringUtils.isBlank(component) ? "" : (", " + component);
    }
}
