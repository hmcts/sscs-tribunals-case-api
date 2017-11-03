package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.time.ZonedDateTime;

public class Mrn {

    private String lateReason;

    private String missingReason;

    private ZonedDateTime dateOfDecision;

    private String mrnLocation;

    public Mrn() {
    }

    public String getLateReason() {
        return lateReason;
    }

    public void setLateReason(String lateReason) {
        this.lateReason = lateReason;
    }

    public String getMissingReason() {
        return missingReason;
    }

    public void setMissingReason(String missingReason) {
        this.missingReason = missingReason;
    }

    public ZonedDateTime getDateOfDecision() {
        return dateOfDecision;
    }

    public void setDateOfDecision(ZonedDateTime date) {
        this.dateOfDecision = dateOfDecision;
    }

    public String getMrnLocation() { return mrnLocation; }

    public void setMrnLocation(String mrnLocation) { this.mrnLocation = mrnLocation; }

    @Override
    public String toString() {
        return "Mrn{"
                +       "lateReason='" + lateReason + '\''
                +       ", missingReason='" + missingReason + '\''
                +       ", dateOfDecision=" + dateOfDecision + '\''
                +       ", mrnLocation='" + mrnLocation
                +       '}';
    }
}
