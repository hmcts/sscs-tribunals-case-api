package uk.gov.hmcts.reform.sscs.hearings.model.single.hearing;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.sscs.hearings.model.hmc.reference.RequirementType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PanelPreference {

    private String memberID;

    private MemberType memberType;

    private RequirementType requirementType;
}
