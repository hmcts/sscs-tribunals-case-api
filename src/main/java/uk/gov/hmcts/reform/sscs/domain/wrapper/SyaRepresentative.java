package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SyaRepresentative {

    private String title;

    private String lastName;

    private String organisation;

    @JsonProperty("contactDetails")
    private SyaContactDetails contactDetails;

    private String firstName;


    public SyaRepresentative() {
        // For Json
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getFullName() {
        return String.format("%s %s", firstName, lastName);
    }

    @Override
    public String toString() {
        return "SyaRepresentative{"
                + " title='" + title + '\''
                + ", firstName='" + firstName + '\''
                + ", lastName='" + lastName + '\''
                + ", organisation='" + organisation + '\''
                + ", contactDetails=" + contactDetails
                + '}';
    }
}
