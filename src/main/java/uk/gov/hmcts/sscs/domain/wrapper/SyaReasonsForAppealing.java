package uk.gov.hmcts.sscs.domain.wrapper;

public class SyaReasonsForAppealing {

    private String reasons;

    private String otherReasons;

    public SyaReasonsForAppealing() {
        // For Json
    }

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
        return "SyaReasonsForAppealing{"
                + " reasons='" + reasons + '\''
                + ", otherReasons='" + otherReasons + '\''
                + '}';
    }
}
