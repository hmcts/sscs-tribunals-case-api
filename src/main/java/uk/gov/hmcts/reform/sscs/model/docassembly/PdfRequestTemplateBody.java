package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

@Builder(toBuilder = true)
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PdfRequestTemplateBody implements FormPayload {
    @JsonProperty("title")
    String title;

    @JsonProperty("text")
    String text;
}
