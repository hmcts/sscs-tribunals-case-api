package uk.gov.hmcts.reform.sscs.service.pdf;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfAppealDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfAppellantStatement;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.CcdPdfService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.pdf.data.AppellantStatementPdfData;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.PdfService;

@Slf4j
@Service
public class StoreAppellantStatementService extends StorePdfService<PdfAppellantStatement, AppellantStatementPdfData> {

    private static final String APPELLANT_STATEMENT = "Appellant statement ";
    private static final String REPRESENTATIVE_STATEMENT = "Representative statement ";

    @Autowired
    public StoreAppellantStatementService(
        @Qualifier("oldPdfService") PdfService pdfService,
        @Value("${personalStatement.html.template.path}") String pdfTemplatePath,
        @Value("${personalStatement.html.welsh.template.path}") String welshPdfTemplatePath,
        CcdPdfService ccdPdfService,
        IdamService idamService,
        PdfStoreService pdfStoreService) {
        super(pdfService, pdfTemplatePath, welshPdfTemplatePath, ccdPdfService, idamService, pdfStoreService);
    }

    @Override
    protected String documentNamePrefix(SscsCaseDetails caseDetails, String onlineHearingId,
                                        AppellantStatementPdfData data) {
        return workOutIfAppellantOrRepsStatement(caseDetails, data)
            + getCountOfNextStatement(caseDetails.getData().getScannedDocuments(),
            caseDetails.getData().getSscsDocument()) + " - ";
    }

    @NotNull
    private String workOutIfAppellantOrRepsStatement(SscsCaseDetails caseDetails, AppellantStatementPdfData data) {
        Subscription repsSubs = caseDetails.getData().getSubscriptions().getRepresentativeSubscription();
        String statementPrefix = APPELLANT_STATEMENT;
        if (repsSubs != null) {
            String tya = data.getStatement().getTya();
            if (tya != null && tya.equals(repsSubs.getTya())) {
                statementPrefix = REPRESENTATIVE_STATEMENT;
            }
        }
        return statementPrefix;
    }

    public static long getCountOfNextStatement(List<ScannedDocument> scannedDocuments, List<SscsDocument> sscsDocument) {
        if ((scannedDocuments == null || scannedDocuments.isEmpty())
            && (sscsDocument == null || sscsDocument.isEmpty())) {
            return 1;
        }
        long statementNextCount = 0;
        if (scannedDocuments != null) {
            statementNextCount = scannedDocuments.stream()
                .filter(doc -> doc.getValue() != null)
                .filter(doc -> StringUtils.isNotBlank(doc.getValue().getFileName()))
                .filter(doc -> docFileNameIsStatement(doc.getValue().getFileName())).count();
        }
        if (sscsDocument != null) {
            statementNextCount += sscsDocument.stream()
                .filter(doc -> doc.getValue() != null)
                .filter(doc -> StringUtils.isNotBlank(doc.getValue().getDocumentFileName()))
                .filter(doc -> docFileNameIsStatement(doc.getValue().getDocumentFileName())).count();
        }
        return statementNextCount + 1;
    }

    private static boolean docFileNameIsStatement(String fileName) {
        return fileName.startsWith(APPELLANT_STATEMENT) || fileName.startsWith(REPRESENTATIVE_STATEMENT);
    }

    @Override
    protected PdfAppellantStatement getPdfContent(AppellantStatementPdfData data, String onlineHearingId,
                                                  PdfAppealDetails appealDetails) {
        return new PdfAppellantStatement(appealDetails, data.getStatement().getBody());
    }
}
