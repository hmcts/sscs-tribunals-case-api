package uk.gov.hmcts.reform.sscs.service.pdf;

import static java.time.LocalDate.now;
import static uk.gov.hmcts.reform.sscs.service.pdf.data.UploadedEvidence.pdf;
import static uk.gov.hmcts.reform.sscs.service.pdf.util.PdfDateUtil.reformatDate;

import java.util.List;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.UpdateDocParams;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfAppealDetails;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.CcdPdfService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;
import uk.gov.hmcts.reform.sscs.service.pdf.data.PdfData;
import uk.gov.hmcts.reform.sscs.service.pdf.data.UploadedEvidence;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.PdfService;

@Slf4j
public abstract class StorePdfService<E, D extends PdfData> {
    private final PdfService pdfService;
    private final String pdfTemplatePath;
    private final String welshPdfTemplatePath;
    private final CcdPdfService ccdPdfService;
    private final IdamService idamService;
    private final PdfStoreService pdfStoreService;

    StorePdfService(PdfService pdfService,
                    String pdfTemplatePath,
                    String welshPdfTemplatePath,
                    CcdPdfService ccdPdfService,
                    IdamService idamService,
                    PdfStoreService pdfStoreService) {
        this.pdfService = pdfService;
        this.pdfTemplatePath = pdfTemplatePath;
        this.welshPdfTemplatePath = welshPdfTemplatePath;
        this.ccdPdfService = ccdPdfService;
        this.idamService = idamService;
        this.pdfStoreService = pdfStoreService;
    }

    public MyaEventActionContext storePdfAndUpdate(Long caseId, String onlineHearingId, D data) {
        SscsCaseDetails caseDetails = data.getCaseDetails();
        String documentNamePrefix = documentNamePrefix(caseDetails, onlineHearingId, data);
        if (pdfHasNotAlreadyBeenCreated(caseDetails, documentNamePrefix)) {
            log.info("Creating pdf for [" + caseId + "]");
            return storePdfAndUpdate(caseId, onlineHearingId, idamService.getIdamTokens(), data, documentNamePrefix);
        } else {
            log.info("Loading pdf for [" + caseId + "]");
            return new MyaEventActionContext(loadPdf(caseDetails, documentNamePrefix), caseDetails);
        }
    }

    private MyaEventActionContext storePdfAndUpdate(Long caseId, String onlineHearingId, IdamTokens idamTokens, D data,
                                                    String documentNamePrefix) {
        SscsCaseDetails caseDetails = data.getCaseDetails();
        PdfAppealDetails pdfAppealDetails = getPdfAppealDetails(caseId, caseDetails);
        boolean isWelsh = caseDetails.getData().isLanguagePreferenceWelsh();

        log.info("Storing pdf for [" + caseId + "]");
        byte[] pdfBytes = pdfService.createPdf(getPdfContent(data, onlineHearingId, pdfAppealDetails),
                isWelsh ? welshPdfTemplatePath : pdfTemplatePath);

        SscsCaseData caseData = caseDetails.getData();
        String pdfName = getPdfName(documentNamePrefix, caseData.getCcdCaseId());
        log.info("Adding pdf to ccd for [" + caseId + "]");
        final UpdateDocParams params = UpdateDocParams.builder()
                .fileName(pdfName)
                .caseId(caseId)
                .pdf(pdfBytes)
                .caseData(caseData)
                .otherPartyId(data.getOtherPartyId())
                .otherPartyName(data.getOtherPartyName())
                .documentType("Other evidence")
                .build();
        SscsCaseData sscsCaseData = ccdPdfService.mergeDocIntoCcd(params, idamTokens);

        return new MyaEventActionContext(pdf(pdfBytes, pdfName), data.getCaseDetails().toBuilder()
                .data(sscsCaseData).build());
    }

    public MyaEventActionContext storePdf(Long caseId, String onlineHearingId, D data) {
        SscsCaseDetails caseDetails = data.getCaseDetails();
        String documentNamePrefix = documentNamePrefix(caseDetails, onlineHearingId, data);
        if (pdfHasNotAlreadyBeenCreated(caseDetails, documentNamePrefix)) {
            log.info("Creating pdf for [" + caseId + "]");
            return storePdf(caseId, onlineHearingId, data, documentNamePrefix);
        } else {
            log.info("Loading pdf for [" + caseId + "]");
            return new MyaEventActionContext(loadPdf(caseDetails, documentNamePrefix), caseDetails);
        }
    }

    private MyaEventActionContext storePdf(Long caseId, String onlineHearingId, D data, String documentNamePrefix) {
        SscsCaseDetails caseDetails = data.getCaseDetails();
        PdfAppealDetails pdfAppealDetails = getPdfAppealDetails(caseId, caseDetails);
        boolean isWelsh = caseDetails.getData().isLanguagePreferenceWelsh();

        log.info("Storing pdf for [" + caseId + "]");
        byte[] pdfBytes = pdfService.createPdf(getPdfContent(data, onlineHearingId, pdfAppealDetails),
                isWelsh ? welshPdfTemplatePath : pdfTemplatePath);

        SscsCaseData caseData = caseDetails.getData();
        String pdfName = getPdfName(documentNamePrefix, caseData.getCcdCaseId());
        log.info("Adding pdf to ccd for [" + caseId + "]");

        UpdateDocParams params = UpdateDocParams.builder()
                .fileName(pdfName)
                .pdf(pdfBytes)
                .caseId(caseId)
                .caseData(caseData)
                .documentType("Other evidence")
                .otherPartyId(data.getOtherPartyId())
                .otherPartyName(data.getOtherPartyName())
                .build();
        SscsCaseData sscsCaseData = ccdPdfService.updateDoc(params);

        return new MyaEventActionContext(pdf(pdfBytes, pdfName), data.getCaseDetails().toBuilder()
                .data(sscsCaseData).build());
    }

    private String getPdfName(String documentNamePrefix, String caseReference) {
        return documentNamePrefix + caseReference + ".pdf";
    }

    private UploadedEvidence loadPdf(SscsCaseDetails caseDetails, String documentNamePrefix) {
        String documentUrl;
        if (this.getClass().getSimpleName().contains(StoreAppellantStatementService.class.getSimpleName())) {
            ScannedDocument document = getScannedDocument(caseDetails, documentNamePrefix);
            documentUrl = document.getValue().getUrl().getDocumentUrl();
        } else {
            SscsDocument document = getSscsDocument(caseDetails, documentNamePrefix);
            documentUrl = document.getValue().getDocumentLink().getDocumentUrl();
        }
        return pdf(pdfStoreService.download(documentUrl),
            getPdfName(documentNamePrefix, caseDetails.getData().getCcdCaseId()));

    }

    private SscsDocument getSscsDocument(SscsCaseDetails caseDetails, String documentNamePrefix) {
        return caseDetails.getData().getSscsDocument().stream()
            .filter(sscsDocument -> sscsDocument.getValue().getDocumentFileName() != null)
            .filter(documentNameMatches(documentNamePrefix))
            .findFirst()
            .orElseThrow(() -> getIllegalStateException(documentNamePrefix));
    }

    private ScannedDocument getScannedDocument(SscsCaseDetails caseDetails, String documentNamePrefix) {
        return caseDetails.getData().getScannedDocuments().stream()
            .filter(scannedDocument -> scannedDocument.getValue() != null)
            .filter(scannedDocument -> StringUtils.isNotBlank(scannedDocument.getValue().getFileName()))
            .filter(scannedDocument -> scannedDocument.getValue().getFileName().startsWith(documentNamePrefix))
            .findFirst()
            .orElseThrow(() -> getIllegalStateException(documentNamePrefix));
    }

    @NotNull
    private IllegalStateException getIllegalStateException(String documentNamePrefix) {
        return new IllegalStateException("Found PDF with name prefix [" + documentNamePrefix + "] "
            + "but cannot load it");
    }

    protected boolean pdfHasNotAlreadyBeenCreated(SscsCaseDetails caseDetails, String documentNamePrefix) {
        if (this.getClass().getSimpleName().contains(StoreAppellantStatementService.class.getSimpleName())) {
            List<ScannedDocument> scannedDocuments = caseDetails.getData().getScannedDocuments();
            return scannedDocumentNoPresent(documentNamePrefix, scannedDocuments);
        }
        return sscsDocumentNotPresent(documentNamePrefix, caseDetails.getData().getSscsDocument());
    }

    private boolean scannedDocumentNoPresent(String documentNamePrefix, List<ScannedDocument> scannedDocuments) {
        return scannedDocuments == null || scannedDocuments.stream()
            .filter(scannedDocument -> scannedDocument.getValue() != null)
            .filter(scannedDocument -> StringUtils.isNotBlank(scannedDocument.getValue().getFileName()))
            .noneMatch(scannedDocument -> scannedDocument.getValue().getFileName().startsWith(documentNamePrefix));
    }

    private boolean sscsDocumentNotPresent(String documentNamePrefix, List<SscsDocument> sscsDocuments) {
        return sscsDocuments == null || sscsDocuments.stream()
            .filter(sscsDocument -> sscsDocument.getValue().getDocumentFileName() != null)
            .noneMatch(documentNameMatches(documentNamePrefix));
    }

    private Predicate<SscsDocument> documentNameMatches(String documentNamePrefix) {
        return sscsDocument -> sscsDocument.getValue().getDocumentFileName().startsWith(documentNamePrefix);
    }

    private PdfAppealDetails getPdfAppealDetails(Long caseId, SscsCaseDetails caseDetails) {
        log.info("Got case details for {}", caseId);
        String appellantTitle = caseDetails.getData().getAppeal().getAppellant().getName().getTitle();
        String appellantFirstName = caseDetails.getData().getAppeal().getAppellant().getName().getFirstName();
        String appellantLastName = caseDetails.getData().getAppeal().getAppellant().getName().getLastName();

        String nino = caseDetails.getData().getAppeal().getAppellant().getIdentity().getNino();
        String caseReference = caseDetails.getId().toString();
        String dateCreated = reformatDate(now());
        boolean hideNino = caseDetails.getData().getBenefitType()
                .filter(benefit -> benefit.equals(Benefit.CHILD_SUPPORT))
                .isPresent();

        if (caseDetails.getData().isLanguagePreferenceWelsh()) {
            return new PdfAppealDetails(appellantTitle, appellantFirstName, appellantLastName, nino, caseReference,
                    dateCreated, hideNino, LocalDateToWelshStringConverter.convert(now()));
        }

        return new PdfAppealDetails(appellantTitle, appellantFirstName, appellantLastName, nino, caseReference,
            dateCreated, hideNino);
    }

    protected abstract String documentNamePrefix(SscsCaseDetails caseDetails, String onlineHearingId, D data);

    protected abstract E getPdfContent(D data, String onlineHearingId, PdfAppealDetails appealDetails);
}
