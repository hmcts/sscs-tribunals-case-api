package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SscsDocument {

    private SscsDocumentDetails value;

    @JsonCreator
    public SscsDocument(@JsonProperty("value") SscsDocumentDetails value) {
        this.value = value;
    }
}
