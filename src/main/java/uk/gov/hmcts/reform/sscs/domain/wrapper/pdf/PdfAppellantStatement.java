package uk.gov.hmcts.reform.sscs.domain.wrapper.pdf;

public class PdfAppellantStatement {
    private final PdfAppealDetails appealDetails;
    private final String statement;

    public PdfAppellantStatement(PdfAppealDetails appealDetails, String statement) {
        this.appealDetails = appealDetails;
        this.statement = statement;
    }

    public PdfAppealDetails getAppealDetails() {
        return appealDetails;
    }

    public String getStatement() {
        return statement;
    }
}
