package uk.gov.hmcts.reform.sscs.domain.wrapper;

import lombok.Data;

@Data
public class SyaContactDetails {
    private String addressLine1;
    private String addressLine2;
    private String townCity;
    private String county;
    private String postCode;
    private String phoneNumber;
    private String emailAddress;
    private String postcodeLookup;
    private String postcodeAddress;
}
