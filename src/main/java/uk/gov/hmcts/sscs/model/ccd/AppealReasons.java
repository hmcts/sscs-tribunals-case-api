package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class AppealReasons {
    private List<AppealReason> reasons;
    private String otherReasons;

    @JsonCreator
    public AppealReasons(@JsonProperty("reasons") List<AppealReason> reasons,
                         @JsonProperty("otherReasons") String otherReasons) {
        this.reasons = reasons;
        this.otherReasons = otherReasons;
    }
}
