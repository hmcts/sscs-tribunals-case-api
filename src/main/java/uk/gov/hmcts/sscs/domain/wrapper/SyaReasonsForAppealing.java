package uk.gov.hmcts.sscs.domain.wrapper;

import java.util.List;

public class SyaReasonsForAppealing {

    private List<Reason> reasons;

    private String otherReasons;

    public SyaReasonsForAppealing() {
        // For Json
    }

    public List<Reason> getReasons() {
        return reasons;
    }

    public void setReasons(List<Reason> reasons) {
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
