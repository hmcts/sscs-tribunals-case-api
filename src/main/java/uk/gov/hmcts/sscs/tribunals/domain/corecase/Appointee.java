package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Appointee extends Person {

    public Appointee(Name name, Address address, String phone, String email) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
    }

    @Override
    public String toString() {
        return "Appointee{"
                +  "name=" + name
                +  ", phone=" + phone
                +  ", address=" + address
                + '}';
    }

}
