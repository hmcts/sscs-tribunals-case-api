package uk.gov.hmcts.sscs.domain.tya;

public class RegionalProcessingCenter {

    private String faxNumber;

    private String address4;

    private String phoneNumber;

    private String name;

    private String address1;

    private String address2;

    private String address3;

    private String postcode;

    private String city;

    public RegionalProcessingCenter() {
        //
    }

    public String getFaxNumber() {
        return faxNumber;
    }

    public void setFaxNumber(String faxNumber) {
        this.faxNumber = faxNumber;
    }

    public String getAddress4() {
        return address4;
    }

    public void setAddress4(String address4) {
        this.address4 = address4;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return "RegionalProcessingCenter{"
                + "faxNumber='" + faxNumber + '\''
                + ", address4='" + address4 + '\''
                + ", phoneNumber='" + phoneNumber + '\''
                + ", name='" + name + '\''
                + ", address1='" + address1 + '\''
                + ", address2='" + address2 + '\''
                + ", address3='" + address3 + '\''
                + ", postcode='" + postcode + '\''
                + ", city='" + city + '\''
                + '}';
    }
}
