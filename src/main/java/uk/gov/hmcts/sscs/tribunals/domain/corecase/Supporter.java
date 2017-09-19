package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Supporter {

    private Name name;

    private Contact contact;

    public Supporter() {
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    @Override
    public String toString() {
        return "Supporter{"
                +       "name=" + name
                +       ", contact=" + contact
                +       '}';
    }
}
