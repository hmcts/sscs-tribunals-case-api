package uk.gov.hmcts.reform.sscs.service.pdf;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.getOtherPartyName;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isOtherParty;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isOtherPartyAppointee;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isOtherPartyRep;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.withTyaPredicate;

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
    private static final String OTHER_PARTY_STATEMENT = "Other party statement ";
    private static final String OTHER_PARTY_REP_STATEMENT = "Other party representative statement ";

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
        if (isOtherParty(data.getCaseDetails().getData(), withTyaPredicate(data.getStatement().getTya()))) {
            statementPrefix = String.format(OTHER_PARTY_STATEMENT + "%s ", getOtherPartyName(data.getCaseDetails().getData(), withTyaPredicate(data.getStatement().getTya())));
        } else if (isOtherPartyRep(data.getCaseDetails().getData(), withTyaPredicate(data.getStatement().getTya()))) {
            statementPrefix = String.format(OTHER_PARTY_REP_STATEMENT + "%s ", getOtherPartyName(data.getCaseDetails().getData(), withTyaPredicate(data.getStatement().getTya())));
        } else if (isOtherPartyAppointee(data.getCaseDetails().getData(), withTyaPredicate(data.getStatement().getTya()))) {
            statementPrefix = String.format(OTHER_PARTY_STATEMENT + "%s ", "Appointee " + getOtherPartyName(data.getCaseDetails().getData(), withTyaPredicate(data.getStatement().getTya())));
        }
        return statementPrefix;
    }

    public static long getCountOfNextStatement(List<ScannedDocument> scannedDocuments, List<SscsDocument> sscsDocument) {
        long scannedDocumentCount = emptyIfNull(scannedDocuments).stream()
            .filter(doc -> doc.getValue() != null)
            .filter(doc -> StringUtils.isNotBlank(doc.getValue().getFileName()))
            .filter(doc -> docFileNameIsStatement(doc.getValue().getFileName())).count();
        long sscsDocumentCount = emptyIfNull(sscsDocument).stream()
            .filter(doc -> doc.getValue() != null)
            .filter(doc -> StringUtils.isNotBlank(doc.getValue().getDocumentFileName()))
            .filter(doc -> docFileNameIsStatement(doc.getValue().getDocumentFileName())).count();
        return scannedDocumentCount + sscsDocumentCount + 1;
    }

    private static boolean docFileNameIsStatement(String fileName) {
        return fileName.startsWith(APPELLANT_STATEMENT)
                || fileName.startsWith(REPRESENTATIVE_STATEMENT)
                || fileName.startsWith(OTHER_PARTY_STATEMENT)
                || fileName.startsWith(OTHER_PARTY_REP_STATEMENT);
    }

    @Override
    protected PdfAppellantStatement getPdfContent(AppellantStatementPdfData data, String onlineHearingId,
                                                  PdfAppealDetails appealDetails) {
        return new PdfAppellantStatement(appealDetails, data.getStatement().getBody());
    }
}
