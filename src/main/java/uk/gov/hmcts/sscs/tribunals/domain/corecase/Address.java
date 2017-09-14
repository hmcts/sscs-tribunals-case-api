package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Address {

    private String county;

    private String town;

    private String postcode;

    private String line1;

    private String line3;

    private String country;

    private String line2;

    public Address() {
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine3() {
        return line3;
    }

    public void setLine3(String line3) {
        this.line3 = line3;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }


    @Override
    public String toString() {
        return "Address{"
                +    "county='" + county + '\''
                +    ", town='" + town + '\''
                +    ", postcode='" + postcode + '\''
                +    ", line1='" + line1 + '\''
                +    ", line3='" + line3 + '\''
                +    ", country='" + country + '\''
                +   ", line2='" + line2 + '\''
                +   '}';
    }
}
