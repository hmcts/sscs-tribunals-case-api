package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@Value
public class SessionHaveContactedDwp {
    @JsonProperty("haveContactedDWP")
    private YesNo haveContactedDwp;
}
