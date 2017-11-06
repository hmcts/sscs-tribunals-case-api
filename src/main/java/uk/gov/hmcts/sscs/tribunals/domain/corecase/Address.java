package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = {"line1", "line2", "town", "county", "postcode"})
public class Address {

    private String line1;

    private String line2;

    private String town;

    private String county;

    private String postcode;

    public Address() { }

    public Address(String line1, String line2, String town, String county, String postcode) {
        this.line1 = line1;
        this.line2 = line2;
        this.town = town;
        this.county = county;
        this.postcode = postcode;
    }

    @XmlElement
    public String getLine1() { return line1; }

    public void setLine1(String line1) { this.line1 = line1; }

    @XmlElement
    public String getLine2() { return line2; }

    public void setLine2(String line2) { this.line2 = line2; }

    @XmlElement
    public String getTown() { return town; }

    public void setTown(String town) { this.town = town; }

    @XmlElement
    public String getCounty() { return county; }

    public void setCounty(String county) { this.county = county; }

    @XmlElement
    public String getPostcode() { return postcode; }

    public void setPostcode(String postcode) { this.postcode = postcode; }

    @Override
    public String toString() {
        return "Address{"
                + " line1='" + line1 + '\''
                + ", line2='" + line2 + '\''
                + ", town='" + town + '\''
                + ", county='" + county + '\''
                + ", postcode='" + postcode + '\''
                + '}';
    }
}
