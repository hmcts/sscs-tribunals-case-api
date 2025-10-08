package uk.gov.hmcts.reform.sscs.docmosis.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PdfDocumentRequest {
    @JsonProperty(value = "accessKey", required = true)
    @NotBlank
    private final String accessKey;
    @JsonProperty(value = "templateName", required = true)
    @NotBlank
    private final String templateName;
    @JsonProperty(value = "outputName", required = true)
    @NotBlank
    private final String outputName;
    @JsonProperty(value = "data", required = true)
    private final Map<String, Object> data;
    @JsonProperty(value = "pdfArchiveMode")
    private final boolean pdfArchiveMode;
}
