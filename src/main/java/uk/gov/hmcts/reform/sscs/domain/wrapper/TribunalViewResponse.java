package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TribunalViewResponse {

    @ApiModelProperty(example = "decision_accepted", required = true)
    @JsonProperty(value = "reply")
    private String reply;

    @ApiModelProperty(example = "This is a reason for the accept or reject", required = false)
    @JsonProperty(value = "reason")
    private String reason;

    // needed for Jackson
    private TribunalViewResponse() {
    }

    public TribunalViewResponse(String reply, String reason) {
        this.reply = reply;
        this.reason = reason;
    }

    public String getReply() {
        return reply;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
