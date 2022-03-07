package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@Value
public class SessionCheckMrn {
    @JsonProperty("checkedMRN")
    private YesNo checkedMrn;
}
