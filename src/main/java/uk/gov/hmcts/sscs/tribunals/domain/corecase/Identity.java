package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.time.ZonedDateTime;

public class Identity {

    private ZonedDateTime dob;

    private String nino;

    public Identity() {
    }

    public ZonedDateTime getDob() {
        return dob;
    }

    public void setDob(ZonedDateTime dob) {
        this.dob = dob;
    }

    public String getNino() {
        return nino;
    }

    public void setNino(String nino) {
        this.nino = nino;
    }

    @Override
    public String toString() {
        return "Identity{"
                +      "dob=" + dob
                +      ", nino='" + nino + '\''
                +      '}';
    }
}
