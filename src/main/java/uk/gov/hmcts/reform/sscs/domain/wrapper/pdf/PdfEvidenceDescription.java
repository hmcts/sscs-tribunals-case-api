package uk.gov.hmcts.reform.sscs.domain.wrapper.pdf;

import java.util.List;

public class PdfEvidenceDescription {
    private final PdfAppealDetails appealDetails;
    private final String description;
    private final List<String> fileNames;

    public PdfEvidenceDescription(PdfAppealDetails appealDetails, String description, List<String> fileNames) {
        this.appealDetails = appealDetails;
        this.description = description;
        this.fileNames = fileNames;
    }

    public PdfAppealDetails getAppealDetails() {
        return appealDetails;
    }

    public String getDescription() {
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

        PdfEvidenceDescription that = (PdfEvidenceDescription) o;

        if (appealDetails != null ? !appealDetails.equals(that.appealDetails) : that.appealDetails != null) {
            return false;
        }
        if (description != null ? !description.equals(that.description) : that.description != null) {
            return false;
        }
        return fileNames != null ? fileNames.equals(that.fileNames) : that.fileNames == null;
    }

    @Override
    public int hashCode() {
        int result = appealDetails != null ? appealDetails.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (fileNames != null ? fileNames.hashCode() : 0);
        return result;
    }
}
