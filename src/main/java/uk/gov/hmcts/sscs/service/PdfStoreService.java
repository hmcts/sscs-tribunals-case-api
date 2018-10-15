package uk.gov.hmcts.sscs.service;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.sscs.model.ByteArrayMultipartFile;

@Service
@Slf4j
public class PdfStoreService {
    private final EvidenceManagementService evidenceManagementService;

    @Autowired
    public PdfStoreService(EvidenceManagementService evidenceManagementService) {
        this.evidenceManagementService = evidenceManagementService;
    }

    public List<SscsDocument> store(byte[] content, String fileName) {
        ByteArrayMultipartFile file = new ByteArrayMultipartFile(content, fileName, APPLICATION_PDF);
        try {
            UploadResponse upload = evidenceManagementService.upload(singletonList(file));
            String location = upload.getEmbedded().getDocuments().get(0).links.self.href;

            DocumentLink documentLink = DocumentLink.builder().documentUrl(location).build();
            SscsDocumentDetails sscsDocumentDetails = SscsDocumentDetails.builder()
                    .documentFileName(fileName)
                    .documentDateAdded(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
                    .documentLink(documentLink)
                    .build();
            SscsDocument pdfDocument = SscsDocument.builder().value(sscsDocumentDetails).build();

            return Collections.singletonList(pdfDocument);
        } catch (RestClientException exc) {
            log.info("Failed to store pdf document but carrying on [" + fileName + "]");
            log.debug("Exception from document store", exc);
            return Collections.emptyList();
        }
    }
}
