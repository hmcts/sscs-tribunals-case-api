package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.pdf.PdfWatermarker;

@Component
@Slf4j
public class FooterService {

    private static final String DM_STORE_USER_ID = "sscs";
    private final EvidenceManagementService evidenceManagementService;

    @Autowired
    public FooterService(EvidenceManagementService evidenceManagementService) {
        this.evidenceManagementService = evidenceManagementService;
    }

    public DocumentLink addFooter(DocumentLink url, String leftText, String rightText) {

        byte[] oldContent = toBytes(url.getDocumentUrl());
        PdfWatermarker alter = new PdfWatermarker();
        byte[] newContent;
        try {
            newContent = alter.shrinkAndWatermarkPdf(oldContent, leftText, rightText);
        } catch (Exception e) {
            log.error("Caught exception :" + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        ByteArrayMultipartFile file = ByteArrayMultipartFile.builder()
                .content(newContent)
                .name(url.getDocumentFilename())
                .contentType(APPLICATION_PDF).build();

        UploadResponse uploadResponse = evidenceManagementService.upload(singletonList(file), DM_STORE_USER_ID);
        String location = uploadResponse.getEmbedded().getDocuments().get(0).links.self.href;

        return url.toBuilder().documentUrl(location).documentBinaryUrl(location + "/binary").build();
    }

    private byte[] toBytes(String documentUrl) {
        return evidenceManagementService.download(
                URI.create(documentUrl),
                DM_STORE_USER_ID
        );
    }
}
