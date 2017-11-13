package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.util.Objects;

public class Representative extends Person {

    private String organisation;

    public Representative(Name name, Address address, String phone, String email,
                          String organisation) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.organisation = organisation;
    }

    public Representative() {

    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    @Override
    public String toString() {
        return "Representative{"
                + " name=" + name
                + ", address=" + address
                + ", phone='" + phone + '\''
                + ", organisation='" + organisation + '\''
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Representative)) {
            return false;
        }
        Representative representative = (Representative) o;
        return Objects.equals(name, representative.name)
                && Objects.equals(address, representative.address)
                && Objects.equals(phone, representative.phone)
                && Objects.equals(email, representative.email)
                && Objects.equals(organisation, representative.organisation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, address, phone, email, organisation);
    }
}
