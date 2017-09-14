package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import java.time.ZonedDateTime;

public class Mrn {

    private String lateReason;

    private String missingReason;

    private ZonedDateTime date;

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

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Mrn{"
                +       "lateReason='" + lateReason + '\''
                +       ", missingReason='" + missingReason + '\''
                +       ", date=" + date
                +       '}';
    }
}
