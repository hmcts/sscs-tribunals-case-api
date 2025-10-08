package uk.gov.hmcts.reform.sscs.bulkscan.domain.transformation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class SuccessfulTransformationResponse {

    @JsonProperty("case_creation_details")
    private final CaseCreationDetails caseCreationDetails;

    @JsonProperty("warnings")
    private final List<String> warnings;

    @JsonProperty("supplementary_data")
    private final Map<String, Map<String, Object>> supplementaryData;

    // region constructor
    public SuccessfulTransformationResponse(
        CaseCreationDetails caseCreationDetails,
        List<String> warnings,
        Map<String, Map<String, Object>> supplementaryData
    ) {
        this.caseCreationDetails = caseCreationDetails;
        this.warnings = warnings;
        this.supplementaryData = supplementaryData;
    }
    // endregion
}
