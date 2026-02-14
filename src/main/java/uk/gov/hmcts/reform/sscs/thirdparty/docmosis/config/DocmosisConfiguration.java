package uk.gov.hmcts.reform.sscs.thirdparty.docmosis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.CcdPdfService;
import uk.gov.hmcts.reform.sscs.thirdparty.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.thirdparty.docmosis.service.DocumentManagementService;

@Configuration
@ConditionalOnProperty("service.pdf-service.uri")
public class DocmosisConfiguration {

    @Value("${service.pdf-service.uri}")
    private String pdfServiceEndpoint;

    @Value("${service.pdf-service.accessKey}")
    private String pdfServiceAccessKey;

    @Bean
    public DocumentManagementService documentManagementService(RestTemplate restTemplate,
                                                               CcdPdfService ccdPdfService,
                                                               IdamService idamService) {
        return new DocumentManagementService(new DocmosisPdfGenerationService(pdfServiceEndpoint,
                pdfServiceAccessKey, restTemplate), ccdPdfService, idamService);
    }
}
