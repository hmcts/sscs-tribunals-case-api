package uk.gov.hmcts.sscs.domain.wrapper;

public class SyaRepresentative {

    private String lastName;

    private String organisation;

    private SyaContactDetails contactDetails;

    private String firstName;


    public SyaRepresentative() {
        // For Json
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public SyaContactDetails getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(SyaContactDetails contactDetails) {
        this.contactDetails = contactDetails;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String toString() {
        return "SyaRepresentative{"
                + " lastName='" + lastName + '\''
                + ", organisation='" + organisation + '\''
                + ", contactDetails=" + contactDetails
                + ", firstName='" + firstName + '\''
                + '}';
    }
}
