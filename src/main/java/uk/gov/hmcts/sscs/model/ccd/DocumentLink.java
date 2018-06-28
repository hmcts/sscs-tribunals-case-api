package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentLink {

    @JsonProperty("document_url")
    private String documentUrl;

    @JsonCreator
    public DocumentLink(@JsonProperty("document_url") String documentUrl) {
        this.documentUrl = documentUrl;
    }
}
