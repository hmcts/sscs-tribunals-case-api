package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

public class FinalDecision {
    private final String reason;

    public FinalDecision(String reason) {
        this.reason = reason;
    }

    @Schema(example = "Some final reason for the decision", required = false)
    @JsonProperty(value = "reason")
    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FinalDecision that = (FinalDecision) o;
        return Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reason);
    }

    @Override
    public String toString() {
        return "FinalDecision{"
                + "reason='" + reason + '\''
                + '}';
    }
}
