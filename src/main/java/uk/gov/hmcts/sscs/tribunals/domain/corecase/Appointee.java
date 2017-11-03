package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class Appointee extends Person {

    @Override
    public String toString() {
        return "Appointee{"
                +  "name=" + name
                +  ", phone=" + phone
                +  ", address=" + address
                + '}';
    }

}
