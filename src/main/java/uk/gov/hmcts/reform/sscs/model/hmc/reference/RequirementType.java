package uk.gov.hmcts.reform.sscs.model.hmc.reference;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RequirementType {

    MUST_INCLUDE("MUSTINC"),
    OPTIONAL_INCLUDE("OPTINC"),
    EXCLUDE("EXCLUDE");

    @JsonValue
    private final String requirementLabel;
}
