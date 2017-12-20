package uk.gov.hmcts.sscs.domain.corecase;

public class ReasonsForAppealing {

    private String reasons;

    private String otherReasons;

    public String getReasons() {
        return reasons;
    }

    public void setReasons(String reasons) {
        this.reasons = reasons;
    }

    public String getOtherReasons() {
        return otherReasons;
    }

    public void setOtherReasons(String otherReasons) {
        this.otherReasons = otherReasons;
    }

    @Override
    public String toString() {
        return "ReasonsForAppealing{"
                + "reasons='" + reasons + '\''
                + ", otherReasons='" + otherReasons + '\''
                + '}';
    }
}
