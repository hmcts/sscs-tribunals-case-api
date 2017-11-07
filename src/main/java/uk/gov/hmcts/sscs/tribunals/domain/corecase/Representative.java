package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Representative extends Person {

    private String organisation;

    public Representative(Name name, Address address, String phone, String email, String organisation) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.organisation = organisation;
    }

    public String getOrganisation() { return organisation; }

    public void setOrganisation(String organisation) { this.organisation = organisation; }

    @Override
    public String toString() {
        return "Representative{"
                + " organisation='" + organisation + '\''
                + ", name=" + name
                + ", address=" + address
                + ", phone='" + phone + '\''
                + '}';
    }
}
