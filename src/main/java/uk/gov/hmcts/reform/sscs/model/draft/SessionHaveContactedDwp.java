package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class SessionHaveContactedDwp {
    @JsonProperty("haveContactedDWP")
    private String haveContactedDwp;
}
