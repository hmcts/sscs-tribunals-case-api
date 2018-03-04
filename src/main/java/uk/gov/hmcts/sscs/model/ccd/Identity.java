package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Identity {
    private String dob;
    private String nino;

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
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
        return "Identity{" +
                "dob='" + dob + '\'' +
                ", nino='" + nino + '\'' +
                '}';
    }
}
