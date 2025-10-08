package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import feign.FeignException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.domain.UpdateDocParams;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class PdfStoreService {
    private final EvidenceManagementService evidenceManagementService;
    private final EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService;
    private final boolean secureDocStoreEnabled;
    private final IdamService idamService;
    static final String DM_STORE_USER_ID = "sscs";

    @Autowired
    public PdfStoreService(EvidenceManagementService evidenceManagementService,
                           EvidenceManagementSecureDocStoreService evidenceManagementSecureDocStoreService,
                           @Value("${feature.secure-doc-store.enabled:false}") boolean secureDocStoreEnabled,
                           IdamService idamService) {
        this.evidenceManagementService = evidenceManagementService;
        this.evidenceManagementSecureDocStoreService = evidenceManagementSecureDocStoreService;
        this.secureDocStoreEnabled = secureDocStoreEnabled;
        this.idamService = idamService;
    }

    public List<SscsDocument> store(byte[] content, String fileName, String documentType) {
        SscsDocument sscsDocument = this.storeDocument(content, fileName, documentType);
        if (sscsDocument == null) {
            return emptyList();
        }
        return singletonList(sscsDocument);
    }

    public List<SscsDocument> store(byte[] content, String fileName, String documentType, SscsDocumentTranslationStatus documentTranslationStatus) {
        SscsDocument sscsDocument = this.storeDocument(content, fileName, documentType, documentTranslationStatus);
        if (sscsDocument == null) {
            return emptyList();
        }
        return singletonList(sscsDocument);
    }

    public SscsDocument storeDocument(byte[] content) {
        return this.storeDocument(content, null, null);
    }

    public SscsDocument storeDocument(byte[] content, String fileName) {
        return this.storeDocument(content, fileName, null);
    }

    public SscsDocument storeDocument(byte[] content, String fileName, String documentType) {
        return this.storeDocument(content, fileName, documentType, null);
    }

    public SscsDocument storeDocument(byte[] content, String fileName, String documentType, SscsDocumentTranslationStatus documentTranslationStatus) {
        return storeDocument(UpdateDocParams.builder().pdf(content).fileName(fileName).documentType(documentType)
                .documentTranslationStatus(documentTranslationStatus).build());
    }

    public SscsDocument storeDocument(UpdateDocParams updateDocParams) {
        if (secureDocStoreEnabled) {
            return storeSecureDocStore(updateDocParams);
        }
        ByteArrayMultipartFile file = ByteArrayMultipartFile.builder().content(updateDocParams.getPdf()).name(updateDocParams.getFileName())
                .contentType(APPLICATION_PDF).build();
        try {
            log.info("Storing file {} of type {} into docstore", updateDocParams.getFileName(), updateDocParams.getDocumentType());
            uk.gov.hmcts.reform.document.domain.UploadResponse upload = evidenceManagementService.upload(singletonList(file), "sscs");
            String location = upload.getEmbedded().getDocuments().get(0).links.self.href;

            DocumentLink documentLink = DocumentLink.builder().documentUrl(location).build();
            SscsDocumentDetails sscsDocumentDetails = SscsDocumentDetails.builder()
                    .documentFileName(updateDocParams.getFileName())
                    .documentDateAdded(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
                    .documentLink(documentLink)
                    .documentType(updateDocParams.getDocumentType())
                    .documentTranslationStatus(updateDocParams.getDocumentTranslationStatus())
                    .originalSenderOtherPartyId(updateDocParams.getOtherPartyId())
                    .originalSenderOtherPartyName(updateDocParams.getOtherPartyName())
                    .build();

            return SscsDocument.builder().value(sscsDocumentDetails).build();
        } catch (RestClientException e) {
            log.error("Failed to store pdf document but carrying on [" + updateDocParams.getFileName() + "]", e);
            return null;
        }
    }

    public SscsDocument storeSecureDocStore(UpdateDocParams updateDocParams) {
        ByteArrayMultipartFile file = ByteArrayMultipartFile.builder().content(updateDocParams.getPdf()).name(updateDocParams.getFileName())
                .contentType(APPLICATION_PDF).build();
        try {
            log.info("Storing file {} of type {} into secure docstore", updateDocParams.getFileName(), updateDocParams.getDocumentType());
            IdamTokens idamTokens = idamService.getIdamTokens();
            UploadResponse upload = evidenceManagementSecureDocStoreService.upload(singletonList(file), idamTokens);
            String location = upload.getDocuments().get(0).links.self.href;
            String hash = upload.getDocuments().get(0).hashToken;

            DocumentLink documentLink = DocumentLink.builder().documentUrl(location).documentBinaryUrl(location + "/binary")
                    .documentFilename(updateDocParams.getFileName()).documentHash(hash).build();
            SscsDocumentDetails sscsDocumentDetails = SscsDocumentDetails.builder()
                    .documentFileName(updateDocParams.getFileName())
                    .documentDateAdded(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
                    .documentLink(documentLink)
                    .documentType(updateDocParams.getDocumentType())
                    .documentTranslationStatus(updateDocParams.getDocumentTranslationStatus())
                    .originalSenderOtherPartyId(updateDocParams.getOtherPartyId())
                    .originalSenderOtherPartyName(updateDocParams.getOtherPartyName())
                    .build();

            return SscsDocument.builder().value(sscsDocumentDetails).build();
        } catch (RestClientException e) {
            log.error("Failed to store pdf document but carrying on [" + updateDocParams.getFileName() + "]", e);
            return null;
        }
    }

    public byte[] download(String href) {
        if (secureDocStoreEnabled) {
            log.info("Downloading file {} from secure docstore", href);
            try {
                return evidenceManagementSecureDocStoreService.download(href, idamService.getIdamTokens());
            } catch (FeignException e) {
                log.info("Download from secure docstore failed for file {} with message {} : ", href, e.getMessage());
                log.info("Downloading file {} from docstore", href);
                return evidenceManagementService.download(URI.create(href), DM_STORE_USER_ID);
            }
        } else {
            log.info("Downloading file {} from docstore", href);
            return evidenceManagementService.download(URI.create(href), DM_STORE_USER_ID);
        }
    }
}
