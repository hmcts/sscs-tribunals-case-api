package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentDetails;

@Value
public class SessionEvidence {
    private String uploadEv;
    private String link;
    private String size;

    @JsonCreator
    public SessionEvidence(String uploadEv, String link, String size) {
        this.uploadEv = uploadEv;
        this.link = link;
        this.size = size;
    }

    public SessionEvidence(DocumentDetails documentDetails) {
        // TODO: set the right values
        this.uploadEv = documentDetails.getEvidenceType();
        this.link = documentDetails.getEvidenceProvidedBy();
        this.size = documentDetails.getDateReceived();
    }
}
