package uk.gov.hmcts.reform.sscs.model.single.hearing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PanelRequirements {

    @JsonProperty("roleType")
    private List<String> roleTypes;

    private List<String> authorisationTypes;

    @JsonProperty("authorisationSubType")
    private List<String> authorisationSubTypes;

    private List<PanelPreference> panelPreferences;

    private List<String> panelSpecialisms;
}
