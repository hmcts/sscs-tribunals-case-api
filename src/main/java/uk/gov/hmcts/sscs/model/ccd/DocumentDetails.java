package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class DocumentDetails {
    private String dateReceived;
    private String evidenceType;
    private String evidenceProvidedBy;

    @JsonCreator
    public DocumentDetails(@JsonProperty("dateReceived") String dateReceived,
                           @JsonProperty("evidenceType") String evidenceType,
                           @JsonProperty("evidenceProvidedBy") String evidenceProvidedBy) {
        this.dateReceived = dateReceived;
        this.evidenceType = evidenceType;
        this.evidenceProvidedBy = evidenceProvidedBy;
    }
}
