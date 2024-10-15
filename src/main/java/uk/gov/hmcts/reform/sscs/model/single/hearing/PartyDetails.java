package uk.gov.hmcts.reform.sscs.model.single.hearing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.PartyType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartyDetails {

    private String partyID;

    private PartyType partyType;

    private String partyRole;

    private IndividualDetails individualDetails;

    private String partyChannelSubType;

    private OrganisationDetails organisationDetails;

    @JsonProperty("unavailabilityDOW")
    private List<UnavailabilityDayOfWeek> unavailabilityDayOfWeek;

    private List<UnavailabilityRange> unavailabilityRanges;
}
