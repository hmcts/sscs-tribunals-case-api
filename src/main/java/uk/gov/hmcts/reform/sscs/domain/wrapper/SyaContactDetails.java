package uk.gov.hmcts.reform.sscs.domain.wrapper;

public class SyaContactDetails {

    private String addressLine1;

    private String addressLine2;

    private String townCity;

    private String county;

    private String postCode;

    private String phoneNumber;

    private String emailAddress;


    public SyaContactDetails() {
        // For JSON
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getTownCity() {
        return townCity;
    }

    public void setTownCity(String townCity) {
        this.townCity = townCity;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public String toString() {
        return "SyaContactDetails{"
                + " addressLine1='" + addressLine1 + '\''
                + ", addressLine2='" + addressLine2 + '\''
                + ", townCity='" + townCity + '\''
                + ", county='" + county + '\''
                + ", postCode='" + postCode + '\''
                + ", phoneNumber='" + phoneNumber + '\''
                + ", emailAddress='" + emailAddress + '\''
                + '}';
    }
}
