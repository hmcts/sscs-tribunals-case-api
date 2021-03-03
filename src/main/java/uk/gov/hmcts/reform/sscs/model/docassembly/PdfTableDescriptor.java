package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PdfTableDescriptor {

    @JsonProperty("document_type")
    private final String documentType;
    @JsonProperty("document_url")
    private final String documentUrl;
    @JsonProperty("date_added")
    private final String dateAdded;
    @JsonProperty("date_approved")
    private final String dateApproved;
    @JsonProperty("upload_party")
    private final String uploadParty;
}
