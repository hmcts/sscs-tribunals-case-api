package uk.gov.hmcts.reform.sscs.functional.handlers;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class UploadDocument {
    private final byte[] data;
    private final String filename;
    private final String documentType;
    private final String bundleAddition;
    private final boolean hasEditedDocumentLink;
}
