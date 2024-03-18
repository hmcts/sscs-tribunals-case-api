package uk.gov.hmcts.reform.sscs.evidenceshare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;

@Configuration
public class PdfGenerationServiceConfig {
    @Bean
    public DocmosisPdfGenerationService docmosisPdfGenerationService(
        @Value("${service.pdf-service.uri}") String pdfServiceEndpoint,
        @Value("${service.pdf-service.accessKey}") String pdfServiceAccessKey,
        RestTemplate restTemplate) {
        return new DocmosisPdfGenerationService(pdfServiceEndpoint, pdfServiceAccessKey, restTemplate);
    }
}
