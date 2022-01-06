package uk.gov.hmcts.reform.sscs.service.pdf.data;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Statement;

public class AppellantStatementPdfData extends PdfData {

    private final Statement statement;

    public AppellantStatementPdfData(SscsCaseDetails caseDetails, Statement statement) {
        super(caseDetails, null, null);
        this.statement = statement;
    }

    public AppellantStatementPdfData(SscsCaseDetails caseDetails, Statement statement, String otherPartyId, String otherPartyName) {
        super(caseDetails, otherPartyId, otherPartyName);
        this.statement = statement;
    }

    public Statement getStatement() {
        return statement;
    }
}
