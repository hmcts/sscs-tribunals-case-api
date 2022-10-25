package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressDetails {
    private final String line1;
    private final String line2;
    private final String town;
    private final String county;
    private final String postcode;

    public AddressDetails(String line1, String line2, String town, String county, String postcode) {
        this.line1 = line1;
        this.line2 = line2;
        this.town = town;
        this.county = county;
        this.postcode = postcode;
    }

    @Schema(example = "14 Oxford Road", required = true)
    @JsonProperty(value = "line1")
    public String getLine1() {
        return line1;
    }

    @Schema(example = "Hastings", required = true)
    @JsonProperty(value = "line2")
    public String getLine2() {
        return line2;
    }

    @Schema(example = "East Sussex", required = true)
    @JsonProperty(value = "town")
    public String getTown() {
        return town;
    }

    @Schema(example = "Sussex", required = true)
    @JsonProperty(value = "county")
    public String getCounty() {
        return county;
    }

    @Schema(example = "TN38 6EW", required = true)
    @JsonProperty(value = "postcode")
    public String getPostcode() {
        return postcode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AddressDetails that = (AddressDetails) o;
        return Objects.equals(line1, that.line1)
                && Objects.equals(line2, that.line2)
                && Objects.equals(town, that.town)
                && Objects.equals(county, that.county)
                && Objects.equals(postcode, that.postcode);
    }

    @Override
    public int hashCode() {

        return Objects.hash(line1, line2, town, county, postcode);
    }

    @Override
    public String toString() {
        return "AddressDetails{"
                + "line1='" + line1 + '\''
                + ", line2='" + line2 + '\''
                + ", town='" + town + '\''
                + ", county='" + county + '\''
                + ", postcode='" + postcode + '\''
                + '}';
    }
}
