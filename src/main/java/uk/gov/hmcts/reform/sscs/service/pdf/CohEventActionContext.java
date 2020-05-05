package uk.gov.hmcts.reform.sscs.service.pdf;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.service.pdf.data.UploadedEvidence;

public class CohEventActionContext {
    private final UploadedEvidence pdf;
    private final SscsCaseDetails document;

    public CohEventActionContext(UploadedEvidence pdf, SscsCaseDetails document) {
        this.pdf = pdf;
        this.document = document;
    }

    public UploadedEvidence getPdf() {
        return pdf;
    }

    public SscsCaseDetails getDocument() {
        return document;
    }
}
