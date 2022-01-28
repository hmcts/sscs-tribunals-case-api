package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtherPartyAttendedDescriptor {
    @JsonProperty("other_party_name")
    private String otherPartyName;
    @JsonProperty("other_party_attended_hearing")
    private String otherPartyAttendedHearing;
}
