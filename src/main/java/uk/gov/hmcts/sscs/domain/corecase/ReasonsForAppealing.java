package uk.gov.hmcts.sscs.domain.corecase;

import java.util.List;
import uk.gov.hmcts.sscs.domain.wrapper.Reason;

public class ReasonsForAppealing {

    private List<Reason> reasons;

    private String otherReasons;

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
        return "ReasonsForAppealing{"
                + "reasons='" + reasons + '\''
                + ", otherReasons='" + otherReasons + '\''
                + '}';
    }
}
