package uk.gov.hmcts.reform.sscs.docmosis.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.PdfDocumentRequest;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;

@Slf4j
public class DocmosisPdfGenerationService implements PdfGenerationService {

    private String pdfServiceEndpoint;

    private String pdfServiceAccessKey;

    private RestTemplate restTemplate;

    String templateEmptyMessage = "document generation template cannot be empty";
    String placeholdersEmptyMessage = "placeholders map cannot be null";

    public DocmosisPdfGenerationService(String pdfServiceEndpoint,
                                        String pdfServiceAccessKey,
                                        RestTemplate restTemplate) {
        this.pdfServiceEndpoint = pdfServiceEndpoint;
        this.pdfServiceAccessKey = pdfServiceAccessKey;
        this.restTemplate = restTemplate;
    }

    @Override
    public byte[] generatePdf(DocumentHolder documentHolder) {

        checkArgument(documentHolder.getTemplate() != null, templateEmptyMessage);

        String templateName = documentHolder.getTemplate().getTemplateName();

        checkArgument(!isNullOrEmpty(templateName), templateEmptyMessage);
        checkNotNull(documentHolder.getPlaceholders(), placeholdersEmptyMessage);

        log.info("Making request to Docmosis pdf service to generate pdf document with template {} "
            + "and placeholders of size [{}] to endpoint {}", templateName, documentHolder.getPlaceholders().size(),
                this.pdfServiceEndpoint);

        try {
            ResponseEntity<byte[]> response =
                restTemplate.postForEntity(pdfServiceEndpoint, request(templateName, documentHolder.getPlaceholders(), documentHolder.isPdfArchiveMode()), byte[].class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to request PDF from Docmosis REST endpoint " + this.pdfServiceEndpoint + " with error " + e.getMessage(), e);
            throw new PdfGenerationException("Failed to request PDF from REST endpoint " + e.getMessage(), e);
        }
    }

    private PdfDocumentRequest request(String templateName, Map<String, Object> placeholders, boolean pdfArchiveMode) {
        return PdfDocumentRequest.builder()
            .accessKey(pdfServiceAccessKey)
            .templateName(templateName)
            .outputName("result.pdf")
            .data(placeholders)
            .pdfArchiveMode(pdfArchiveMode).build();
    }
}
