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
import uk.gov.hmcts.reform.sscs.service.pdf.CohEventActionContext;
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
    private final EvidenceUploadEmailService evidenceUploadEmailService;
    private final FileToPdfConversionService fileToPdfConversionService;
    private final EvidenceManagementService evidenceManagementService;
    private final PdfStoreService pdfStoreService;

    private static final String UPDATED_SSCS = "Updated SSCS";
    public static final String DM_STORE_USER_ID = "sscs";

    private static final DraftHearingDocumentExtractor draftHearingDocumentExtractor = new DraftHearingDocumentExtractor();
    private static final QuestionDocumentExtractor questionDocumentExtractor = new QuestionDocumentExtractor();

    @Autowired
    public EvidenceUploadService(DocumentManagementService documentManagementService, CcdService ccdService,
                                 IdamService idamService, OnlineHearingService onlineHearingService,
                                 StoreEvidenceDescriptionService storeEvidenceDescriptionService, EvidenceUploadEmailService evidenceUploadEmailService,
                                 FileToPdfConversionService fileToPdfConversionService,
                                 EvidenceManagementService evidenceManagementService, PdfStoreService pdfStoreService) {
        this.documentManagementService = documentManagementService;
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.onlineHearingService = onlineHearingService;
        this.storeEvidenceDescriptionService = storeEvidenceDescriptionService;
        this.evidenceUploadEmailService = evidenceUploadEmailService;
        this.fileToPdfConversionService = fileToPdfConversionService;
        this.evidenceManagementService = evidenceManagementService;
        this.pdfStoreService = pdfStoreService;
    }

    public Optional<Evidence> uploadDraftHearingEvidence(String identifier, MultipartFile file) {
        return uploadEvidence(identifier, file, draftHearingDocumentExtractor,
            document -> new SscsDocument(createNewDocumentDetails(document)), UPLOAD_DRAFT_DOCUMENT,
            "SSCS - upload document from MYA");
    }

    public Optional<Evidence> uploadDraftQuestionEvidence(String identifier, String questionId, MultipartFile file) {
        return uploadEvidence(identifier, file, questionDocumentExtractor, document ->
            new CorDocument(new CorDocumentDetails(createNewDocumentDetails(document), questionId)),
            UPLOAD_COR_DOCUMENT, "SSCS - cor evidence uploaded");
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
                    CohEventActionContext storePdfContext = storeEvidenceDescriptionService.storePdf(
                        ccdCaseId, identifier, data);
                    if (sscsCaseData.getAppeal() == null || sscsCaseData.getAppeal().getHearingType() == null
                        || sscsCaseData.getAppeal().getHearingType().equals("cor")) {
                        submitHearingForACorCase(caseDetails, sscsCaseData, ccdCaseId, storePdfContext);
                    } else {
                        submitHearingWhenNoCoreCase(caseDetails, sscsCaseData, ccdCaseId, storePdfContext,
                            data.getDescription().getIdamEmail());
                    }
                    return true;
                })
                .orElse(false);
    }

    private void submitHearingWhenNoCoreCase(SscsCaseDetails caseDetails, SscsCaseData sscsCaseData, Long ccdCaseId,
                                             CohEventActionContext storePdfContext, String idamEmail) {

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
                                                       CohEventActionContext storePdfContext, String idamEmail) {
        String appellantOrRepsFileNamePrefix = workOutAppellantOrRepsFileNamePrefix(caseDetails, idamEmail);
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
                                                                       CohEventActionContext storePdfContext,
                                                                       String filename) {
        removeStatementDocFromDocumentTab(sscsCaseData, storePdfContext.getDocument().getData().getSscsDocument());
        List<byte[]> contentUploads = getContentListFromTheEvidenceUploads(storePdfContext);
        ByteArrayResource statementContent = getContentFromTheStatement(storePdfContext);
        byte[] combinedContent = appendEvidenceUploadsToStatement(statementContent.getByteArray(), contentUploads);
        SscsDocument combinedPdfEvidence = pdfStoreService.store(combinedContent, filename, "Other evidence").get(0);
        return buildScannedDocumentByGivenSscsDoc(combinedPdfEvidence);
    }

    private ByteArrayResource getContentFromTheStatement(CohEventActionContext storePdfContext) {
        return (ByteArrayResource) storePdfContext.getPdf().getContent();
    }

    private List<byte[]> getContentListFromTheEvidenceUploads(CohEventActionContext storePdfContext) {
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


    private byte[] appendEvidenceUploadsToStatement(byte[] statement, List<byte[]> uploads) {
        if (statement != null && uploads != null) {
            PDDocument statementDoc = getLoadSafe(statement);
            final PDFMergerUtility merger = new PDFMergerUtility();
            uploads.forEach(upload -> {
                PDDocument uploadDoc = getLoadSafe(upload);
                appendDocumentSafe(statementDoc, merger, uploadDoc);
                closeSafe(uploadDoc);
            });
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
            throw new RuntimeException("Error when closing Doc..", e);
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

    private static PDDocument getLoadSafe(byte[] statement) {
        try {
            return PDDocument.load(statement);
        } catch (IOException e) {
            throw new RuntimeException("Error when getting PDDocument statement..", e);
        }
    }

    private void removeUniqueIdAndSetFilenameForTheEvidenceDesc(SscsDocument evidenceDescriptionDocument) {
        String fileNameCleanOfUniqueId = removeTemporalUniqueIdFromGivenString(evidenceDescriptionDocument.getValue()
            .getDocumentFileName());
        evidenceDescriptionDocument.getValue().setDocumentFileName(fileNameCleanOfUniqueId);
        DocumentLink dlFilename = evidenceDescriptionDocument.getValue().getDocumentLink();
        DocumentLink newDocLink = dlFilename.toBuilder()
            .documentFilename(fileNameCleanOfUniqueId)
            .documentUrl(dlFilename.getDocumentUrl())
            .documentBinaryUrl(dlFilename.getDocumentBinaryUrl())
            .build();
        evidenceDescriptionDocument.getValue().setDocumentLink(newDocLink);
    }

    @NotNull
    private String removeTemporalUniqueIdFromGivenString(String filename) {
        return filename.replace(TEMP_UNIQUE_ID, "").trim();
    }

    private void removeStatementDocFromDocumentTab(SscsCaseData sscsCaseData, List<SscsDocument> sscsDocument) {
        sscsDocument.removeIf(doc -> doc.getValue().getDocumentFileName().startsWith(TEMP_UNIQUE_ID)
                || doc.getValue().getDocumentLink().getDocumentFilename().startsWith(TEMP_UNIQUE_ID));
        sscsCaseData.setSscsDocument(sscsDocument);
    }

    private SscsDocument findAndReturnTheNewEvidenceDescDoc(List<SscsDocument> sscsDocument) {
        return sscsDocument.stream()
                .filter(doc -> doc.getValue().getDocumentFileName().startsWith(TEMP_UNIQUE_ID))
                .findFirst().orElseThrow(() -> new RuntimeException("Evidence description file cannot be found"));
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
    private String workOutAppellantOrRepsFileNamePrefix(SscsCaseDetails caseDetails, String idamEmail) {
        String fileNamePrefix = "Appellant";
        Subscriptions subscriptions = caseDetails.getData().getSubscriptions();
        if (subscriptions != null) {
            Subscription repSubs = subscriptions.getRepresentativeSubscription();
            if (repSubs != null && StringUtils.isNotBlank(repSubs.getEmail())) {
                if (repSubs.getEmail().equalsIgnoreCase(idamEmail)) {
                    fileNamePrefix = "Representative";
                }
            }
        }
        return fileNamePrefix;
    }

    private void submitHearingForACorCase(SscsCaseDetails caseDetails, SscsCaseData sscsCaseData, Long ccdCaseId,
                                          CohEventActionContext storePdfContext) {
        log.info("Submitting draft document for case [" + ccdCaseId + "]");
        List<SscsDocument> draftSscsDocument = storePdfContext.getDocument().getData()
            .getDraftSscsDocument();
        List<SscsDocument> newSscsDocumentsList = union(
                emptyIfNull(storePdfContext.getDocument().getData().getSscsDocument()),
                emptyIfNull(draftSscsDocument)
        );
        SscsDocument evidenceDescriptionDocument = findAndReturnTheNewEvidenceDescDoc(newSscsDocumentsList);
        removeUniqueIdAndSetFilenameForTheEvidenceDesc(evidenceDescriptionDocument);
        sscsCaseData.setSscsDocument(newSscsDocumentsList);
        sscsCaseData.setDraftSscsDocument(Collections.emptyList());

        ccdService.updateCase(sscsCaseData, ccdCaseId, UPLOAD_COR_DOCUMENT.getCcdType(),
            "SSCS - cor evidence uploaded", UPDATED_SSCS, idamService.getIdamTokens());

        evidenceUploadEmailService.sendToDwp(storePdfContext.getPdf(), draftSscsDocument, caseDetails);
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

    public boolean deleteQuestionEvidence(String onlineHearingId, String evidenceId) {
        return deleteEvidence(onlineHearingId, evidenceId, questionDocumentExtractor);
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

                        ccdService.updateCase(caseDetails.getData(), caseDetails.getId(), UPLOAD_COR_DOCUMENT.getCcdType(), "SSCS - cor evidence deleted", UPDATED_SSCS, idamService.getIdamTokens());

                        documentManagementService.delete(evidenceId);
                    }
                    return true;
                }).orElse(false);
    }

    private interface DocumentExtract<E> {
        Function<SscsCaseData, List<E>> getDocuments();

        BiConsumer<SscsCaseData, List<E>> setDocuments();

        Function<E, SscsDocumentDetails> findDocument();
    }

    private static class QuestionDocumentExtractor implements DocumentExtract<CorDocument> {
        @Override
        public Function<SscsCaseData, List<CorDocument>> getDocuments() {
            return SscsCaseData::getDraftCorDocument;
        }

        @Override
        public BiConsumer<SscsCaseData, List<CorDocument>> setDocuments() {
            return SscsCaseData::setDraftCorDocument;
        }

        @Override
        public Function<CorDocument, SscsDocumentDetails> findDocument() {
            return document -> document.getValue().getDocument();
        }
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
        private final List<Evidence> hearingEvidence;
        private final Map<String, List<Evidence>> draftQuestionEvidence;
        private final Map<String, List<Evidence>> questionEvidence;

        LoadedEvidence(SscsCaseDetails caseDetails) {
            this.caseDetails = caseDetails;
            draftHearingEvidence = extractHearingEvidences(caseDetails.getData().getDraftSscsDocument());
            hearingEvidence = extractHearingEvidences(caseDetails.getData().getSscsDocument());
            draftQuestionEvidence = extractQuestionEvidence(caseDetails.getData().getDraftCorDocument());
            questionEvidence = extractQuestionEvidence(caseDetails.getData().getCorDocument());
        }

        public SscsCaseDetails getCaseDetails() {
            return caseDetails;
        }

        public List<Evidence> getDraftHearingEvidence() {
            return draftHearingEvidence;
        }

        public List<Evidence> getHearingEvidence() {
            return hearingEvidence;
        }

        public Map<String, List<Evidence>> getDraftQuestionEvidence() {
            return draftQuestionEvidence;
        }

        public Map<String, List<Evidence>> getQuestionEvidence() {
            return questionEvidence;
        }

        private List<Evidence> extractHearingEvidences(List<SscsDocument> sscsDocuments) {
            List<SscsDocument> hearingDocuments = emptyIfNull(sscsDocuments);
            return hearingDocuments.stream().map(sscsDocumentToEvidence()).collect(toList());
        }

        private Map<String, List<Evidence>> extractQuestionEvidence(List<CorDocument> corDocument) {
            List<CorDocument> questionDocuments = emptyIfNull(corDocument);
            return questionDocuments.stream()
                    .collect(groupingBy(corDocumentDetails -> corDocumentDetails.getValue().getQuestionId(), mapping(corDocumentToEvidence(), toList())));
        }

        private Function<CorDocument, Evidence> corDocumentToEvidence() {
            return corDocument -> {
                SscsDocumentDetails sscsDocumentDetails = corDocument.getValue().getDocument();
                return extractEvidence(sscsDocumentDetails);
            };
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
