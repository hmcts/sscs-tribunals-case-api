package uk.gov.hmcts.reform.sscs.model.draft;

import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;

@Value
public class SessionPostcodeChecker {
    private String postcode;

    public SessionPostcodeChecker(Address address) {
        postcode = address.getPostcode();
    }
}
