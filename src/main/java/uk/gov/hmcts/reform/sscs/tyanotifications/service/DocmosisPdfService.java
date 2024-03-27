package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;

@Service
public class DocmosisPdfService {
    private final DocmosisPdfGenerationService docmosisPdfGenerationService;

    public DocmosisPdfService(DocmosisPdfGenerationService docmosisPdfGenerationService) {
        this.docmosisPdfGenerationService = docmosisPdfGenerationService;
    }

    public byte[] createPdf(Object pdfSummary, String templatePath) {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> placeholders = objectMapper.convertValue(
            pdfSummary,
            new TypeReference<Map<String, Object>>() {
            }
        );
        return createPdfFromMap(placeholders, templatePath);
    }

    public byte[] createPdfFromMap(Map<String, Object> placeholders, String templatePath) {
        return docmosisPdfGenerationService.generatePdf(DocumentHolder.builder()
            .template(new Template(templatePath, ""))
            .placeholders(placeholders)
            .build());
    }
}
