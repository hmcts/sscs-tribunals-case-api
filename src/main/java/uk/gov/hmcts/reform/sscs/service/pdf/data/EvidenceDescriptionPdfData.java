package uk.gov.hmcts.reform.sscs.service.pdf.data;

import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.EvidenceDescription;

public class EvidenceDescriptionPdfData extends PdfData {

    private final EvidenceDescription description;
    private final List<String> fileNames;

    public EvidenceDescriptionPdfData(SscsCaseDetails caseDetails, EvidenceDescription description, List<String> fileNames) {
        super(caseDetails, null, null);
        this.description = description;
        this.fileNames = fileNames;
    }

    public EvidenceDescriptionPdfData(SscsCaseDetails caseDetails, EvidenceDescription description, List<String> fileNames, String otherPartyId, String otherPartyName) {
        super(caseDetails, otherPartyId, otherPartyName);
        this.description = description;
        this.fileNames = fileNames;
    }

    public EvidenceDescription getDescription() {
        return description;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EvidenceDescriptionPdfData that = (EvidenceDescriptionPdfData) o;

        if (description != null ? !description.equals(that.description) : that.description != null) {
            return false;
        }
        return fileNames != null ? fileNames.equals(that.fileNames) : that.fileNames == null;
    }

    @Override
    public int hashCode() {
        int result = description != null ? description.hashCode() : 0;
        result = 31 * result + (fileNames != null ? fileNames.hashCode() : 0);
        return result;
    }
}
