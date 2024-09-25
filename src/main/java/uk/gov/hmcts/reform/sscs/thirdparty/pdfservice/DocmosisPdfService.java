package uk.gov.hmcts.reform.sscs.thirdparty.pdfservice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;

@Service
public class DocmosisPdfService implements PdfService {
    private final DocmosisPdfGenerationService docmosisPdfGenerationService;

    public DocmosisPdfService(DocmosisPdfGenerationService docmosisPdfGenerationService) {
        this.docmosisPdfGenerationService = docmosisPdfGenerationService;
    }

    @Override
    public byte[] createPdf(Object pdfSummary, String templatePath) {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> placeholders = objectMapper.convertValue(
                pdfSummary,
                new TypeReference<Map<String, Object>>() {
                }
        );
        return docmosisPdfGenerationService.generatePdf(DocumentHolder.builder()
                .template(new Template(templatePath, ""))
                .placeholders(placeholders)
                .build());
    }
}
