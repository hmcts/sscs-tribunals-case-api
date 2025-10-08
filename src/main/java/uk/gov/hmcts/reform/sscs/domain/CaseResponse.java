package uk.gov.hmcts.reform.sscs.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.sscs.bulkscan.domain.validation.ValidationStatus;

@Data
@Builder
public class CaseResponse {
    @Schema(description = "Warning messages")
    private List<String> warnings;
    @Schema(description = "Transformed case")
    private Map<String, Object> transformedCase;
    @Schema(description = "Error messages")
    private List<String> errors;
    @Schema(description = "Validation status")
    private ValidationStatus status;

}
