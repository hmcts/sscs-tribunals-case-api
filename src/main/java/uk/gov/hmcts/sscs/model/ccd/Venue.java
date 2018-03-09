package uk.gov.hmcts.sscs.model.ccd;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Venue {
    private String name;
    private Address address;
    private String googleMapLink;
}
