package uk.gov.hmcts.reform.sscs.bulkscan.domain.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.Data;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.OcrDataField;

@Data
public class OcrDataValidationRequest {

    @Schema(description = "List of ocr data fields to be validated.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private final List<OcrDataField> ocrDataFields;

    public OcrDataValidationRequest(
        @JsonProperty("ocr_data_fields") List<OcrDataField> ocrDataFields
    ) {
        this.ocrDataFields = ocrDataFields;
    }
}
