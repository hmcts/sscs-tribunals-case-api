package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Representative extends Person {

    @Override
    public String toString() {
        return "Representative{"
                +  "name=" + name
                +  ", phone=" + phone
                +  ", address=" + address
                + '}';
    }
}
