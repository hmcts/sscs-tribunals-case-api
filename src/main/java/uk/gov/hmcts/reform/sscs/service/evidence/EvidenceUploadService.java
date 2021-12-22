package uk.gov.hmcts.reform.sscs.service.evidence;

import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.collections4.ListUtils.union;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty.OTHER_PARTY_APPOINTEE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty.OTHER_PARTY_REP;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.service.pdf.StoreEvidenceDescriptionService.TEMP_UNIQUE_ID;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.setHasUnprocessedAudioVideoEvidenceFlag;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
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
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;
import uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil;

@Slf4j
@Service
public class EvidenceUploadService {
    private final CcdService ccdService;
    private final IdamService idamService;
    private final OnlineHearingService onlineHearingService;
    private final StoreEvidenceDescriptionService storeEvidenceDescriptionService;
    private final FileToPdfConversionService fileToPdfConversionService;
    private final EvidenceManagementService evidenceManagementService;
    private final PdfStoreService pdfStoreService;

    private static final String UPDATED_SSCS = "Updated SSCS";
    public static final String DM_STORE_USER_ID = "sscs";
    private static final Enum<EventType> EVENT_TYPE = EventType.UPLOAD_DOCUMENT;
    private final AddedDocumentsUtil addedDocumentsUtil;
    private static final DraftHearingDocumentExtractor draftHearingDocumentExtractor = new DraftHearingDocumentExtractor();

    @Autowired
    public EvidenceUploadService(CcdService ccdService,
                                 IdamService idamService, OnlineHearingService onlineHearingService,
                                 StoreEvidenceDescriptionService storeEvidenceDescriptionService,
                                 FileToPdfConversionService fileToPdfConversionService,
                                 EvidenceManagementService evidenceManagementService, PdfStoreService pdfStoreService,
                                 AddedDocumentsUtil addedDocumentsUtil) {
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.onlineHearingService = onlineHearingService;
        this.storeEvidenceDescriptionService = storeEvidenceDescriptionService;
        this.fileToPdfConversionService = fileToPdfConversionService;
        this.evidenceManagementService = evidenceManagementService;
        this.pdfStoreService = pdfStoreService;
        this.addedDocumentsUtil = addedDocumentsUtil;
    }

    private SscsDocumentDetails createNewDocumentDetails(Document document) {
        String createdOn = getCreatedDate(document);
        DocumentLink documentLink = DocumentLink.builder()
            .documentUrl(document.links.self.href)
            .documentFilename(document.originalDocumentName)
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

    public boolean submitHearingEvidence(String identifier, EvidenceDescription description, MultipartFile file) {
        return onlineHearingService.getCcdCaseByIdentifier(identifier)
                .map(caseDetails -> {
                    String md5Checksum = "";
                    String filename = "";
                    try {
                        md5Checksum = DigestUtils.md5DigestAsHex(file.getBytes()).toUpperCase();
                        filename = file.getOriginalFilename();
                        log.info("uploadEvidence: for case ID Case({}): User has uploaded the file ({}) with a checksum of ({})", caseDetails.getId(), filename, md5Checksum);
                    } catch (IOException ioException) {
                        log.error(ioException.getMessage()
                                + ". Something has gone wrong for caseId: ", caseDetails.getId()
                                + " when logging uploadEvidence for file (" + filename
                                + ") with a checksum of (" + md5Checksum + ")");
                        return false;
                    }


                    List<MultipartFile> convertedFiles = fileToPdfConversionService.convert(singletonList(file));

                    Document document = evidenceManagementService.upload(convertedFiles, DM_STORE_USER_ID).getEmbedded().getDocuments().get(0);

                    List<SscsDocument> currentDocuments = draftHearingDocumentExtractor.getDocuments().apply(caseDetails.getData());
                    ArrayList<SscsDocument> newDocuments = (currentDocuments == null) ? new ArrayList<>() : new ArrayList<>(currentDocuments);
                    newDocuments.add(new SscsDocument(createNewDocumentDetails(document)));

                    draftHearingDocumentExtractor.setDocuments().accept(caseDetails.getData(), newDocuments);

                    SscsCaseData sscsCaseData = caseDetails.getData();
                    Long ccdCaseId = caseDetails.getId();
                    EvidenceDescriptionPdfData data = new EvidenceDescriptionPdfData(caseDetails, description,
                            getFileNames(sscsCaseData),
                            getOtherPartyId(sscsCaseData, withEmailPredicate(description.getIdamEmail())),
                            getOtherPartyName(sscsCaseData, withEmailPredicate(description.getIdamEmail())));
                    MyaEventActionContext storePdfContext = storeEvidenceDescriptionService.storePdf(
                        ccdCaseId, identifier, data);
                    submitHearingWhenNoCoreCase(caseDetails, sscsCaseData, ccdCaseId, storePdfContext,
                        data.getDescription().getIdamEmail());

                    return true;
                })
                .orElse(false);
    }

    private void submitHearingWhenNoCoreCase(SscsCaseDetails caseDetails, SscsCaseData sscsCaseData, Long ccdCaseId,
                                             MyaEventActionContext storePdfContext, String idamEmail) {

        String filename = getFilenameForTheNextUploadEvidence(caseDetails, ccdCaseId, storePdfContext, idamEmail);

        appendEvidenceUploadsToStatementAndStoreIt(sscsCaseData, storePdfContext,
            filename, idamEmail);

        sscsCaseData.setDraftSscsDocument(Collections.emptyList());
        sscsCaseData.setEvidenceHandled("No");
        ccdService.updateCase(sscsCaseData, ccdCaseId, UPLOAD_DOCUMENT.getCcdType(),
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

    private void appendEvidenceUploadsToStatementAndStoreIt(SscsCaseData sscsCaseData,
                                                            MyaEventActionContext storePdfContext,
                                                            String filename,
                                                            String idamEmail) {
        removeStatementDocFromDocumentTab(sscsCaseData, storePdfContext.getDocument().getData().getSscsDocument());
        List<SscsDocument> audioVideoMedia = pullAudioVideoFilesFromDraft(storePdfContext.getDocument().getData().getDraftSscsDocument());

        if (audioVideoMedia.size() > 0) {
            sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE.getId());
            if (!REVIEW_BY_JUDGE.getId().equals(sscsCaseData.getInterlocReviewState())) {
                sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW.getId());
            }
        }

        List<byte[]> contentUploads = getContentListFromTheEvidenceUploads(storePdfContext);
        ByteArrayResource statementContent = getContentFromTheStatement(storePdfContext);
        byte[] combinedContent = appendEvidenceUploadsToStatement(statementContent.getByteArray(), contentUploads,
                sscsCaseData.getCcdCaseId());
        SscsDocument combinedPdfEvidence = pdfStoreService.store(combinedContent, filename, "Other evidence").get(0);
        buildUploadedDocumentByGivenSscsDoc(sscsCaseData, combinedPdfEvidence, audioVideoMedia, idamEmail);
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

    protected List<SscsDocument> pullAudioVideoFilesFromDraft(List<SscsDocument> sscsDocuments) {
        List<SscsDocument> audioVideoFiles = new ArrayList<>();

        Iterator<SscsDocument> iterator = sscsDocuments.iterator();
        while (iterator.hasNext()) {
            SscsDocument document = iterator.next();
            String filename = document.getValue().getDocumentFileName();
            if (filename != null && (endsWithIgnoreCase(filename, "mp3") || endsWithIgnoreCase(filename, "mp4"))) {
                audioVideoFiles.add(document);
                iterator.remove();
            }
        }
        return audioVideoFiles;
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


    protected void buildUploadedDocumentByGivenSscsDoc(SscsCaseData sscsCaseData, SscsDocument draftSscsDocument,
                                                       List<SscsDocument> audioVideoMedia, String idamEmail) {
        LocalDate ld = LocalDate.parse(draftSscsDocument.getValue().getDocumentDateAdded(),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime ldt = LocalDateTime.of(ld, LocalDateTime.now().toLocalTime());

        if (audioVideoMedia != null && !audioVideoMedia.isEmpty()) {
            updateCaseDataWithNewAudioVideoUpload(audioVideoMedia, sscsCaseData, idamEmail, ldt, draftSscsDocument);
        } else {
            updateCaseDataWithNewPdfUpload(sscsCaseData, ldt, draftSscsDocument, idamEmail);
        }
        setHasUnprocessedAudioVideoEvidenceFlag(sscsCaseData);
    }

    private void updateCaseDataWithNewPdfUpload(SscsCaseData sscsCaseData, LocalDateTime ldt, SscsDocument draftSscsDocument, String idamEmail) {
        ScannedDocument scannedDocument = ScannedDocument.builder()
                .value(ScannedDocumentDetails.builder()
                        .type("other")
                        .url(draftSscsDocument.getValue().getDocumentLink())
                        .fileName(draftSscsDocument.getValue().getDocumentFileName())
                        .scannedDate(ldt.toString())
                        .originalSenderOtherPartyId(getOtherPartyId(sscsCaseData, withEmailPredicate(idamEmail)))
                        .originalSenderOtherPartyName(getOtherPartyName(sscsCaseData, withEmailPredicate(idamEmail)))
                        .build())
                .build();

        List<ScannedDocument> scannedDocuments = new ArrayList<>();
        scannedDocuments.add(scannedDocument);
        List<ScannedDocument> newScannedDocumentsList = union(emptyIfNull(sscsCaseData.getScannedDocuments()),
                emptyIfNull(scannedDocuments));
        sscsCaseData.setScannedDocuments(newScannedDocumentsList);
    }

    private void updateCaseDataWithNewAudioVideoUpload(List<SscsDocument> audioVideoMedia, SscsCaseData sscsCaseData, String idamEmail, LocalDateTime ldt, SscsDocument draftSscsDocument) {
        List<AudioVideoEvidence> audioVideoEvidence = new ArrayList<>();
        UploadParty uploader = workOutAudioVideoUploadParty(sscsCaseData, idamEmail);
        String originalSenderOtherPartyId = getOtherPartyId(sscsCaseData, withEmailPredicate(idamEmail));
        String originalSenderPartyName = getOtherPartyName(sscsCaseData, withEmailPredicate(idamEmail));

        for (SscsDocument audioVideoDocument : audioVideoMedia) {
            SscsDocumentDetails sscsDocumentDetails = audioVideoDocument.getValue();
            audioVideoEvidence.add(AudioVideoEvidence.builder()
                .value(AudioVideoEvidenceDetails.builder()
                    .originalSenderOtherPartyId(originalSenderOtherPartyId)
                    .originalSenderOtherPartyName(originalSenderPartyName)
                    .documentLink(sscsDocumentDetails.getDocumentLink())
                    .dateAdded(ldt.toLocalDate())
                    .fileName(sscsDocumentDetails.getDocumentFileName())
                    .partyUploaded(uploader)
                    .documentType(AudioVideoEvidenceUtil.getDocumentTypeValue(
                        sscsDocumentDetails.getDocumentLink().getDocumentFilename()))
                    .statementOfEvidencePdf(draftSscsDocument.getValue().getDocumentLink())
                    .build())
                .build());
            System.out.println(sscsDocumentDetails.getDocumentLink().getDocumentFilename());
        }

        addedDocumentsUtil.computeDocumentsAddedThisEvent(sscsCaseData, audioVideoEvidence.stream()
            .map(evidence -> evidence.getValue().getDocumentType())
            .collect(Collectors.toUnmodifiableList()), EVENT_TYPE);

        if (!audioVideoEvidence.isEmpty()) {
            List<AudioVideoEvidence> newAudioVideoEvidenceList = union(emptyIfNull(sscsCaseData.getAudioVideoEvidence()),
                emptyIfNull(audioVideoEvidence));
            sscsCaseData.setAudioVideoEvidence(newAudioVideoEvidenceList);
        }
    }

    private String workOutFileNamePrefix(SscsCaseDetails caseDetails, String idamEmail) {
        final UploadParty uploadParty = workOutAudioVideoUploadParty(caseDetails.getData(), idamEmail);
        if (uploadParty == OTHER_PARTY) {
            return uploadParty.getLabel() + " - " + getOtherPartyName(caseDetails.getData(), withEmailPredicate(idamEmail));
        }
        if (uploadParty == OTHER_PARTY_REP) {
            return "Other party - Representative " + getOtherPartyName(caseDetails.getData(), withEmailPredicate(idamEmail));
        }
        if (uploadParty == OTHER_PARTY_APPOINTEE) {
            return "Other party - Appointee " + getOtherPartyName(caseDetails.getData(), withEmailPredicate(idamEmail));
        }

        return uploadParty.getLabel();
    }

    private UploadParty workOutAudioVideoUploadParty(SscsCaseData sscsCaseData, String idamEmail) {
        UploadParty uploader = UploadParty.APPELLANT;
        Subscriptions subscriptions = sscsCaseData.getSubscriptions();
        if (subscriptions != null) {
            if (nonNull(subscriptions.getAppointeeSubscription()) && idamEmail.equalsIgnoreCase(subscriptions.getAppointeeSubscription().getEmail())) {
                uploader = UploadParty.APPOINTEE;
            } else if (nonNull(subscriptions.getRepresentativeSubscription()) && idamEmail.equalsIgnoreCase(subscriptions.getRepresentativeSubscription().getEmail())) {
                uploader = UploadParty.REP;
            } else if (nonNull(subscriptions.getJointPartySubscription()) && idamEmail.equalsIgnoreCase(subscriptions.getJointPartySubscription().getEmail())) {
                uploader = UploadParty.JOINT_PARTY;
            }
        }
        if (isOtherParty(sscsCaseData, withEmailPredicate(idamEmail))) {
            uploader = OTHER_PARTY;
        } else if (isOtherPartyRep(sscsCaseData, withEmailPredicate(idamEmail))) {
            uploader = OTHER_PARTY_REP;
        } else if (isOtherPartyAppointee(sscsCaseData, withEmailPredicate(idamEmail))) {
            uploader = OTHER_PARTY_APPOINTEE;
        }
        return uploader;
    }

    private List<String> getFileNames(SscsCaseData sscsCaseData) {
        return sscsCaseData.getDraftSscsDocument().stream()
                .map(document -> document.getValue().getDocumentFileName())
                .collect(toList());
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
}
