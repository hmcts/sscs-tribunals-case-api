package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.util.Objects;

public class Appointee extends Person {

    public Appointee(Name name, Address address, String phone, String email) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
    }

    public Appointee() {
    }

    @Override
    public String toString() {
        return "Appointee{"
                +  "name=" + name
                +  ", phone=" + phone
                +  ", address=" + address
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Appointee)) {
            return false;
        }
        Appointee appointee = (Appointee) o;
        return Objects.equals(name, appointee.name)
                && Objects.equals(address, appointee.address)
                && Objects.equals(phone, appointee.phone)
                && Objects.equals(email, appointee.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, address, phone, email);
    }

}
