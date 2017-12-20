package uk.gov.hmcts.sscs.domain.corecase;

import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = {"line1", "line2", "town", "county", "postcode"})
public class Address {

    private String line1;

    private String line2;

    private String town;

    private String county;

    private String postcode;

    private String googleMapUrl;

    public Address() { }

    public Address(String line1, String line2, String town, String county, String postcode) {
        this.line1 = line1;
        this.line2 = line2;
        this.town = town;
        this.county = county;
        this.postcode = postcode;
    }

    public Address(String line1, String line2, String town, String county, String postcode,
                   String googleMapUrl) {
        this.line1 = line1;
        this.line2 = line2;
        this.town = town;
        this.county = county;
        this.postcode = postcode;
        this.googleMapUrl = googleMapUrl;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    @XmlTransient
    public String getGoogleMapUrl() {
        return googleMapUrl;
    }

    public void setGoogleMapUrl(String googleMapUrl) {
        this.googleMapUrl = googleMapUrl;
    }

    @Override
    public String toString() {
        return "Address{"
                + " line1='" + line1 + '\''
                + ", line2='" + line2 + '\''
                + ", town='" + town + '\''
                + ", county='" + county + '\''
                + ", postcode='" + postcode + '\''
                + ", googleMapUrl='" + googleMapUrl + '\''
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Address)) {
            return false;
        }
        Address address = (Address) o;
        return Objects.equals(line1, address.line1)
                && Objects.equals(line2, address.line2)
                && Objects.equals(town, address.town)
                && Objects.equals(county, address.county)
                && Objects.equals(postcode, address.postcode)
                && Objects.equals(googleMapUrl, address.googleMapUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(line1, line2, town, county, postcode, googleMapUrl);
    }
}
