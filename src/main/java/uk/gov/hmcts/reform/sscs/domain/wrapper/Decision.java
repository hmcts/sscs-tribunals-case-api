package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Decision {
    private final String decisionState;
    private final String decisionStateDateTime;
    private final String appellantReply;
    private final String appellantReplyDateTime;
    private final String startDate;
    private final String endDate;
    private final DecisionRates decisionRates;
    private final String reason;
    private final Activities activities;

    public Decision(String decisionState, String decisionStateDateTime, String appellantReply, String appellantReplyDateTime, String startDate, String endDate, DecisionRates decisionRates, String reason, Activities activities) {
        this.decisionState = decisionState;
        this.decisionStateDateTime = decisionStateDateTime;
        this.appellantReply = appellantReply;
        this.appellantReplyDateTime = appellantReplyDateTime;
        this.startDate = startDate;
        this.endDate = endDate;
        this.decisionRates = decisionRates;
        this.reason = reason;
        this.activities = activities;
    }

    @Schema(example = "decision_issued", required = true)
    @JsonProperty(value = "decision_state")
    public String getDecisionState() {
        return decisionState;
    }

    @Schema(example = "2018-10-05T09:36:33Z", required = true)
    @JsonProperty(value = "decision_state_datetime")
    public String getDecisionStateDateTime() {
        return decisionStateDateTime;
    }

    @Schema(example = "2018-10-05", required = true)
    @JsonProperty(value = "start_date")
    public String getStartDate() {
        return startDate;
    }

    @Schema(example = "2018-10-05")
    @JsonProperty(value = "end_date")
    public String getEndDate() {
        return endDate;
    }

    @JsonProperty(value = "decision_rates")
    public DecisionRates getDecisionRates() {
        return decisionRates;
    }

    @Schema(example = "Some reason for the decision", required = true)
    @JsonProperty(value = "reason")
    public String getReason() {
        return reason;
    }

    @JsonProperty(value = "activities")
    public Activities getActivities() {
        return activities;
    }

    @Schema(example = "decision_accepted")
    @JsonProperty(value = "appellant_reply")
    public String getAppellantReply() {
        return appellantReply;
    }

    @Schema(example = "2018-10-06T10:30:24Z")
    @JsonProperty(value = "appellant_reply_datetime")
    public String getAppellantReplyDateTime() {
        return appellantReplyDateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Decision decision = (Decision) o;
        return Objects.equals(decisionState, decision.decisionState)
                && Objects.equals(decisionStateDateTime, decision.decisionStateDateTime)
                && Objects.equals(startDate, decision.startDate)
                && Objects.equals(endDate, decision.endDate)
                && Objects.equals(decisionRates, decision.decisionRates)
                && Objects.equals(reason, decision.reason)
                && Objects.equals(activities, decision.activities)
                && Objects.equals(appellantReply, decision.appellantReply)
                && Objects.equals(appellantReplyDateTime, decision.appellantReplyDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(decisionState, decisionStateDateTime, startDate, endDate, decisionRates, reason, activities, appellantReply, appellantReplyDateTime);
    }

    @Override
    public String toString() {
        return "Decision{"
                + "decisionState='" + decisionState + '\''
                + ", decisionStateDateTime='" + decisionStateDateTime + '\''
                + ", appellantReply='" + appellantReply + '\''
                + ", appellantReplyDateTime='" + appellantReplyDateTime + '\''
                + ", startDate='" + startDate + '\''
                + ", endDate='" + endDate + '\''
                + ", decisionRates=" + decisionRates
                + ", reason='" + reason + '\''
                + ", activities=" + activities
                + '}';
    }
}
