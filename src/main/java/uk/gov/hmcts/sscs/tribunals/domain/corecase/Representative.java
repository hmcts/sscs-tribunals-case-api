package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Representative {

    private String organisation;

    private Contact contact;

    public Representative() {
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    @Override
    public String toString() {
        return "Representative{"
                +      "organisation='" + organisation + '\''
                +      ", contact=" + contact
                +      '}';
    }
}
