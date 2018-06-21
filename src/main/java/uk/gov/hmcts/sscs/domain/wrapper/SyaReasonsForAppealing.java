package uk.gov.hmcts.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SyaReasonsForAppealing {

    private List<Reason> reasons;

    private String otherReasons;

    @JsonProperty("evidences")
    private List<SyaEvidence> evidences;

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

    public List<SyaEvidence> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<SyaEvidence> evidences) {
        this.evidences = evidences;
    }

    @Override
    public String   toString() {
        return "SyaReasonsForAppealing{"
                + "reasons=" + reasons
                + ", otherReasons='" + otherReasons + '\''
                + ", evidences=" + evidences
                + '}';
    }
}
