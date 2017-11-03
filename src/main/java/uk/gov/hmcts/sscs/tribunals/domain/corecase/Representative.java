package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Representative extends Person {

    private String organisation;

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
