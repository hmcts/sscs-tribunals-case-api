package uk.gov.hmcts.reform.sscs.service.pdf.data;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

public class PdfData {
    private final SscsCaseDetails caseDetails;
    private final String otherPartyId;
    private final String otherPartyName;

    public PdfData(SscsCaseDetails caseDetails) {
        this(caseDetails, null, null);
    }

    public PdfData(SscsCaseDetails caseDetails, String otherPartyId, String otherPartyName) {
        this.caseDetails = caseDetails;
        this.otherPartyId = otherPartyId;
        this.otherPartyName = otherPartyName;
    }

    public SscsCaseDetails getCaseDetails() {
        return caseDetails;
    }

    public String getOtherPartyId() {
        return otherPartyId;
    }

    public String getOtherPartyName() {
        return otherPartyName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PdfData pdfData = (PdfData) o;

        return caseDetails != null ? caseDetails.equals(pdfData.caseDetails) : pdfData.caseDetails == null;
    }

    @Override
    public int hashCode() {
        return caseDetails != null ? caseDetails.hashCode() : 0;
    }
}
