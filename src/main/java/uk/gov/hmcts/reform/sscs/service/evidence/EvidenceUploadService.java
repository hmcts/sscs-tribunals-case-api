package uk.gov.hmcts.reform.sscs.service.evidence;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.collections4.ListUtils.union;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.service.pdf.StoreEvidenceDescriptionService.TEMP_UNIQUE_ID;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Evidence;
import uk.gov.hmcts.reform.sscs.domain.wrapper.EvidenceDescription;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.conversion.FileToPdfConversionService;
import uk.gov.hmcts.reform.sscs.service.exceptions.EvidenceUploadException;
import uk.gov.hmcts.reform.sscs.service.pdf.MyaEventActionContext;
import uk.gov.hmcts.reform.sscs.service.pdf.StoreEvidenceDescriptionService;
import uk.gov.hmcts.reform.sscs.service.pdf.data.EvidenceDescriptionPdfData;
import uk.gov.hmcts.reform.sscs.thirdparty.documentmanagement.DocumentManagementService;

@Slf4j
@Service
public class EvidenceUploadService {
    private final DocumentManagementService documentManagementService;
    private final CcdService ccdService;
    private final IdamService idamService;
    private final OnlineHearingService onlineHearingService;
    private final StoreEvidenceDescriptionService storeEvidenceDescriptionService;
    private final FileToPdfConversionService fileToPdfConversionService;
    private final EvidenceManagementService evidenceManagementService;
    private final PdfStoreService pdfStoreService;

    private static final String UPDATED_SSCS = "Updated SSCS";
    public static final String DM_STORE_USER_ID = "sscs";

    private static final DraftHearingDocumentExtractor draftHearingDocumentExtractor = new DraftHearingDocumentExtractor();

    @Autowired
    public EvidenceUploadService(DocumentManagementService documentManagementService,
                                 CcdService ccdService,
                                 IdamService idamService, OnlineHearingService onlineHearingService,
                                 StoreEvidenceDescriptionService storeEvidenceDescriptionService,
                                 FileToPdfConversionService fileToPdfConversionService,
                                 EvidenceManagementService evidenceManagementService, PdfStoreService pdfStoreService) {
        this.documentManagementService = documentManagementService;
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.onlineHearingService = onlineHearingService;
        this.storeEvidenceDescriptionService = storeEvidenceDescriptionService;
        this.fileToPdfConversionService = fileToPdfConversionService;
        this.evidenceManagementService = evidenceManagementService;
        this.pdfStoreService = pdfStoreService;
    }

    public Optional<Evidence> uploadDraftHearingEvidence(String identifier, MultipartFile file) {
        return uploadEvidence(identifier, file, draftHearingDocumentExtractor,
            document -> new SscsDocument(createNewDocumentDetails(document)), UPLOAD_DRAFT_DOCUMENT,
            "SSCS - upload document from MYA");
    }

    private SscsDocumentDetails createNewDocumentDetails(Document document) {
        String createdOn = getCreatedDate(document);
        DocumentLink documentLink = DocumentLink.builder()
                .documentUrl(document.links.self.href)
                .build();

        return SscsDocumentDetails.builder()
                .documentType("Other evidence")
                .documentFileName(document.originalDocumentName)
                .documentDateAdded(createdOn)
                .documentLink(documentLink)
                .build();
    }

    private String getCreatedDate(Document document) {
        return document.createdOn.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ISO_DATE);
    }

    private <E> Optional<Evidence> uploadEvidence(String identifier, MultipartFile file,
                                                  DocumentExtract<E> documentExtract,
                                                  Function<Document, E> createNewDocument, EventType eventType,
                                                  String summary) {
        return onlineHearingService.getCcdCaseByIdentifier(identifier)
                .map(caseDetails -> {

                    List<MultipartFile> convertedFiles = fileToPdfConversionService.convert(singletonList(file));

                    Document document = evidenceManagementService.upload(convertedFiles, DM_STORE_USER_ID).getEmbedded().getDocuments().get(0);

                    List<E> currentDocuments = documentExtract.getDocuments().apply(caseDetails.getData());
                    ArrayList<E> newDocuments = (currentDocuments == null) ? new ArrayList<>() : new ArrayList<>(currentDocuments);
                    newDocuments.add(createNewDocument.apply(document));

                    documentExtract.setDocuments().accept(caseDetails.getData(), newDocuments);

                    ccdService.updateCase(caseDetails.getData(), caseDetails.getId(), eventType.getCcdType(), summary, UPDATED_SSCS, idamService.getIdamTokens());

                    return new Evidence(document.links.self.href, document.originalDocumentName, getCreatedDate(document));
                });
    }

    public boolean submitHearingEvidence(String identifier, EvidenceDescription description) {
        return onlineHearingService.getCcdCaseByIdentifier(identifier)
                .map(caseDetails -> {
                    SscsCaseData sscsCaseData = caseDetails.getData();
                    Long ccdCaseId = caseDetails.getId();
                    EvidenceDescriptionPdfData data = new EvidenceDescriptionPdfData(caseDetails, description,
                        getFileNames(sscsCaseData));
                    MyaEventActionContext storePdfContext = storeEvidenceDescriptionService.storePdf(
                        ccdCaseId, identifier, data);
                    submitHearingWhenNoCoreCase(caseDetails, sscsCaseData, ccdCaseId, storePdfContext,
                        data.getDescription().getIdamEmail());

                    return true;
                })
                .orElse(false);
    }

    public boolean deleteDraftHearingEvidence(String identifier, String evidenceId) {
        return deleteEvidence(identifier, evidenceId, draftHearingDocumentExtractor);
    }

    private <E> boolean deleteEvidence(String identifier, String evidenceId, DocumentExtract<E> documentExtract) {
        return onlineHearingService.getCcdCaseByIdentifier(identifier)
                .map(caseDetails -> {
                    List<E> documents = documentExtract.getDocuments().apply(caseDetails.getData());

                    if (documents != null) {
                        List<E> newDocuments = documents.stream()
                                .filter(corDocument -> !documentExtract.findDocument().apply(corDocument).getDocumentLink().getDocumentUrl().endsWith(evidenceId))
                                .collect(toList());
                        documentExtract.setDocuments().accept(caseDetails.getData(), newDocuments);

                        ccdService.updateCase(caseDetails.getData(), caseDetails.getId(), UPLOAD_DRAFT_DOCUMENT.getCcdType(), "SSCS - evidence deleted", UPDATED_SSCS, idamService.getIdamTokens());

                        documentManagementService.delete(evidenceId);
                    }
                    return true;
                }).orElse(false);
    }

    private void submitHearingWhenNoCoreCase(SscsCaseDetails caseDetails, SscsCaseData sscsCaseData, Long ccdCaseId,
                                             MyaEventActionContext storePdfContext, String idamEmail) {

        String filename = getFilenameForTheNextUploadEvidence(caseDetails, ccdCaseId, storePdfContext, idamEmail);
        ScannedDocument mergedEvidencesDoc = appendEvidenceUploadsToStatementAndStoreIt(sscsCaseData, storePdfContext,
            filename);
        mergeNewUnprocessedCorrespondenceToTheExistingInTheCase(caseDetails, sscsCaseData, mergedEvidencesDoc);
        sscsCaseData.setDraftSscsDocument(Collections.emptyList());
        sscsCaseData.setEvidenceHandled("No");
        ccdService.updateCase(sscsCaseData, ccdCaseId, ATTACH_SCANNED_DOCS.getCcdType(),
            "SSCS - upload evidence from MYA",
            "Uploaded a further evidence document", idamService.getIdamTokens());
    }

    private String getFilenameForTheNextUploadEvidence(SscsCaseDetails caseDetails, Long ccdCaseId,
                                                       MyaEventActionContext storePdfContext, String idamEmail) {
        String appellantOrRepsFileNamePrefix = workOutFileNamePrefix(caseDetails, idamEmail);
        SscsCaseData data = storePdfContext.getDocument().getData();
        long uploadCounter = getCountOfNextUploadDoc(data.getScannedDocuments(), data.getSscsDocument());
        return String.format("%s upload %s - %s.pdf", appellantOrRepsFileNamePrefix, uploadCounter, ccdCaseId);
    }

    private void mergeNewUnprocessedCorrespondenceToTheExistingInTheCase(SscsCaseDetails caseDetails,
                                                                         SscsCaseData sscsCaseData,
                                                                         ScannedDocument scannedDocs) {
        List<ScannedDocument> newScannedDocumentsList = union(emptyIfNull(caseDetails.getData().getScannedDocuments()),
                emptyIfNull(Collections.singletonList(scannedDocs)));
        sscsCaseData.setScannedDocuments(newScannedDocumentsList);
    }

    private ScannedDocument appendEvidenceUploadsToStatementAndStoreIt(SscsCaseData sscsCaseData,
                                                                       MyaEventActionContext storePdfContext,
                                                                       String filename) {
        removeStatementDocFromDocumentTab(sscsCaseData, storePdfContext.getDocument().getData().getSscsDocument());
        List<byte[]> contentUploads = getContentListFromTheEvidenceUploads(storePdfContext);
        ByteArrayResource statementContent = getContentFromTheStatement(storePdfContext);
        byte[] combinedContent = appendEvidenceUploadsToStatement(statementContent.getByteArray(), contentUploads,
                sscsCaseData.getCcdCaseId());
        SscsDocument combinedPdfEvidence = pdfStoreService.store(combinedContent, filename, "Other evidence").get(0);
        return buildScannedDocumentByGivenSscsDoc(combinedPdfEvidence);
    }

    private ByteArrayResource getContentFromTheStatement(MyaEventActionContext storePdfContext) {
        return (ByteArrayResource) storePdfContext.getPdf().getContent();
    }

    private List<byte[]> getContentListFromTheEvidenceUploads(MyaEventActionContext storePdfContext) {
        List<byte[]> draftPdfContentList = new ArrayList<>();
        List<SscsDocument> drafts = storePdfContext.getDocument().getData().getDraftSscsDocument();
        drafts.forEach(draft -> {
            String documentUrl = draft.getValue().getDocumentLink().getDocumentUrl();
            byte[] draftPdfContent = getContentInBytesForGivenDocumentStoreUrl(documentUrl);
            draftPdfContentList.add(draftPdfContent);
        });
        return draftPdfContentList;
    }

    private byte[] getContentInBytesForGivenDocumentStoreUrl(String draftDocUrl) {
        byte[] draftPdfContent;
        try {
            draftPdfContent = evidenceManagementService.download(new URI(draftDocUrl), "sscs");
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error when downloading document from Evidence Management Service..", e);
        }
        return draftPdfContent;
    }


    private byte[] appendEvidenceUploadsToStatement(byte[] statement, List<byte[]> uploads, String caseId) {

        if (statement != null && uploads != null) {

            PDDocument statementDoc = getLoadSafe(statement, "statementDoc", caseId);
            final PDFMergerUtility merger = new PDFMergerUtility();

            for (int i = 0; i < uploads.size(); i++) {
                PDDocument uploadDoc = getLoadSafe(uploads.get(i), "uploadDoc" + i, caseId);
                appendDocumentSafe(statementDoc, merger, uploadDoc);
                closeSafe(uploadDoc);
            }

            ByteArrayOutputStream combinedContent = new ByteArrayOutputStream();
            saveDocSafe(statementDoc, combinedContent);
            closeSafe(statementDoc);
            return combinedContent.toByteArray();

        } else {
            throw new RuntimeException("Can not combine empty statement or evidence documents");
        }
    }

    private static void closeSafe(PDDocument statementDoc) {
        try {
            statementDoc.close();
        } catch (IOException e) {
            throw new EvidenceUploadException("Error when closing Doc..", e);
        }
    }

    private static void saveDocSafe(PDDocument statementDoc, ByteArrayOutputStream combinedContent) {
        try {
            statementDoc.save(combinedContent);
        } catch (IOException e) {
            throw new RuntimeException("Error when saving Doc..", e);
        }
    }

    private static void appendDocumentSafe(PDDocument statementDoc, PDFMergerUtility merger, PDDocument uploadDoc) {
        try {
            merger.appendDocument(statementDoc, uploadDoc);
        } catch (IOException e) {
            throw new RuntimeException("Error when appending docs..", e);
        }
    }

    public static PDDocument getLoadSafe(byte[] statement, String docType, String caseId) {
        try {
            return PDDocument.load(statement);
        } catch (IOException e) {
            throw new EvidenceUploadException("Error when getting PDDocument " + docType + " for caseId " + caseId
                    + " with bytes length " + statement.length, e);
        }
    }

    private void removeStatementDocFromDocumentTab(SscsCaseData sscsCaseData, List<SscsDocument> sscsDocument) {
        sscsDocument.removeIf(doc -> doc.getValue().getDocumentFileName().startsWith(TEMP_UNIQUE_ID)
                || doc.getValue().getDocumentLink().getDocumentFilename().startsWith(TEMP_UNIQUE_ID));
        sscsCaseData.setSscsDocument(sscsDocument);
    }

    private long getCountOfNextUploadDoc(List<ScannedDocument> scannedDocuments, List<SscsDocument> sscsDocument) {
        if ((scannedDocuments == null || scannedDocuments.isEmpty())
            && (sscsDocument == null || sscsDocument.isEmpty())) {
            return 1;
        }
        long statementNextCount = 0;
        if (scannedDocuments != null) {
            statementNextCount = scannedDocuments.stream()
                .filter(doc -> doc.getValue() != null)
                .filter(doc -> StringUtils.isNotBlank(doc.getValue().getFileName()))
                .filter(doc -> isTheDocFileNameAUpload(doc.getValue().getFileName())).count();
        }
        if (sscsDocument != null) {
            statementNextCount += sscsDocument.stream()
                .filter(doc -> doc.getValue() != null)
                .filter(doc -> StringUtils.isNotBlank(doc.getValue().getDocumentFileName()))
                .filter(doc -> isTheDocFileNameAUpload(doc.getValue().getDocumentFileName())).count();
        }
        return statementNextCount + 1;
    }

    private static boolean isTheDocFileNameAUpload(String fileName) {
        return fileName.startsWith("Appellant upload") || fileName.startsWith("Representative upload");
    }

    private ScannedDocument buildScannedDocumentByGivenSscsDoc(SscsDocument draftSscsDocument) {
        LocalDate ld = LocalDate.parse(draftSscsDocument.getValue().getDocumentDateAdded(),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime ldt = LocalDateTime.of(ld, LocalDateTime.now().toLocalTime());
        return ScannedDocument.builder()
            .value(ScannedDocumentDetails.builder()
                .type("other")
                .url(draftSscsDocument.getValue().getDocumentLink())
                .fileName(draftSscsDocument.getValue().getDocumentFileName())
                .scannedDate(ldt.toString())
                .build())
            .build();
    }

    @NotNull
    private String workOutFileNamePrefix(SscsCaseDetails caseDetails, String idamEmail) {
        String fileNamePrefix = "Appellant";
        Subscriptions subscriptions = caseDetails.getData().getSubscriptions();
        if (subscriptions != null) {
            Subscription repSubs = subscriptions.getRepresentativeSubscription();
            Subscription jpSubs = subscriptions.getJointPartySubscription();
            if (repSubs != null && idamEmail.equalsIgnoreCase(repSubs.getEmail())) {
                fileNamePrefix = "Representative";
            } else if (jpSubs != null && idamEmail.equalsIgnoreCase(jpSubs.getEmail())) {
                fileNamePrefix = "Joint party";
            }
        }
        return fileNamePrefix;
    }

    private List<String> getFileNames(SscsCaseData sscsCaseData) {
        return sscsCaseData.getDraftSscsDocument().stream()
                .map(document -> document.getValue().getDocumentFileName())
                .collect(toList());
    }

    public List<Evidence> listDraftHearingEvidence(String identifier) {
        return loadEvidence(identifier)
                .map(LoadedEvidence::getDraftHearingEvidence)
                .orElse(emptyList());
    }

    private Optional<LoadedEvidence> loadEvidence(String identifier) {
        return onlineHearingService.getCcdCaseByIdentifier(identifier)
                .map(LoadedEvidence::new);
    }

    private interface DocumentExtract<E> {
        Function<SscsCaseData, List<E>> getDocuments();

        BiConsumer<SscsCaseData, List<E>> setDocuments();

        Function<E, SscsDocumentDetails> findDocument();
    }

    private static class DraftHearingDocumentExtractor implements DocumentExtract<SscsDocument> {
        @Override
        public Function<SscsCaseData, List<SscsDocument>> getDocuments() {
            return SscsCaseData::getDraftSscsDocument;
        }

        @Override
        public BiConsumer<SscsCaseData, List<SscsDocument>> setDocuments() {
            return SscsCaseData::setDraftSscsDocument;
        }

        @Override
        public Function<SscsDocument, SscsDocumentDetails> findDocument() {
            return SscsDocument::getValue;
        }
    }

    private static class LoadedEvidence {
        private final SscsCaseDetails caseDetails;
        private final List<Evidence> draftHearingEvidence;

        LoadedEvidence(SscsCaseDetails caseDetails) {
            this.caseDetails = caseDetails;
            draftHearingEvidence = extractHearingEvidences(caseDetails.getData().getDraftSscsDocument());
        }

        public SscsCaseDetails getCaseDetails() {
            return caseDetails;
        }

        public List<Evidence> getDraftHearingEvidence() {
            return draftHearingEvidence;
        }

        private List<Evidence> extractHearingEvidences(List<SscsDocument> sscsDocuments) {
            List<SscsDocument> hearingDocuments = emptyIfNull(sscsDocuments);
            return hearingDocuments.stream().map(sscsDocumentToEvidence()).collect(toList());
        }

        private Function<SscsDocument, Evidence> sscsDocumentToEvidence() {
            return sscsDocument -> {
                SscsDocumentDetails sscsDocumentDetails = sscsDocument.getValue();
                return extractEvidence(sscsDocumentDetails);
            };
        }

        private Evidence extractEvidence(SscsDocumentDetails sscsDocumentDetails) {
            DocumentLink documentLink = sscsDocumentDetails.getDocumentLink();
            return new Evidence(
                    documentLink != null ? documentLink.getDocumentUrl() : null,
                    sscsDocumentDetails.getDocumentFileName(),
                    sscsDocumentDetails.getDocumentDateAdded());
        }
    }
}
