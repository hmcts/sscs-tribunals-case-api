package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Address {
    private String line1;
    private String line2;
    private String town;
    private String county;
    private String postcode;
}
