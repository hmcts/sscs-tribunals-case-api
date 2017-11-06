package uk.gov.hmcts.sscs.tribunals.domain.corecase;

import javax.xml.bind.annotation.XmlElement;
import java.time.ZonedDateTime;

public class Mrn {

    private String lateReason;

    private String missingReason;

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

    @Override
    public String toString() {
        return "Mrn{"
                + " lateReason='" + lateReason + '\''
                + ", missingReason='" + missingReason + '\''
                + '}';
    }
}
