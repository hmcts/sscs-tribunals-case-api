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
public class SscsDocumentDetails {

    private String documentType;
    private String documentFileName;
    private String documentEmailContent;
    private String documentDateAdded;
    private DocumentLink documentLink;
    private String documentComment;

    @JsonCreator
    public SscsDocumentDetails(@JsonProperty("documentType") String documentType,
                               @JsonProperty("documentFileName") String documentFileName,
                               @JsonProperty("documentEmailContent") String documentEmailContent,
                               @JsonProperty("documentDateAdded") String documentDateAdded,
                               @JsonProperty("documentLink") DocumentLink documentLink,
                               @JsonProperty("documentComment") String documentComment) {
        this.documentType = documentType;
        this.documentFileName = documentFileName;
        this.documentEmailContent = documentEmailContent;
        this.documentDateAdded = documentDateAdded;
        this.documentLink = documentLink;
        this.documentComment = documentComment;
    }
}
