package uk.gov.hmcts.sscs.model.tya;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegionalProcessingCenter {

    @JsonCreator
    public RegionalProcessingCenter(@JsonProperty("faxNumber") String faxNumber,
                                    @JsonProperty("address4") String address4,
                                    @JsonProperty("phoneNumber") String phoneNumber,
                                    @JsonProperty("name") String name,
                                    @JsonProperty("address1") String address1,
                                    @JsonProperty("address2") String address2,
                                    @JsonProperty("address3") String address3,
                                    @JsonProperty("postcode") String postcode,
                                    @JsonProperty("city") String city) {
        this.faxNumber = faxNumber;
        this.address4 = address4;
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.postcode = postcode;
        this.city = city;
    }

    private String faxNumber;

    private String address4;

    private String phoneNumber;

    private String name;

    private String address1;

    private String address2;

    private String address3;

    private String postcode;

    private String city;

}
