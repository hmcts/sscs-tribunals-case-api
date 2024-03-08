package uk.gov.hmcts.reform.sscs.model.evidenceshare;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;

@Value
@Builder
public class PdfDocument {
    Pdf pdf;
    AbstractDocument document;
}
