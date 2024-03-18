package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.TribunalsCaseApiApplication;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;

@SpringBootTest(classes = TribunalsCaseApiApplication.class)
@TestPropertySource(locations = "classpath:config/application_es_it.properties")
public class DocmosisPdfGenerationIt {

    public static final String FILE_CONTENT = "Welcome to PDF document service";
    public static final Map<String, Object> PLACEHOLDERS = caseDataMap();

    public static final String PDF_SERVICE_URI = "https://docmosis-development.platform.hmcts.net/rs/render";

    private DocmosisPdfGenerationService pdfGenerationService;

    @Autowired
    private RestTemplate restTemplate;

    @MockBean
    protected AirLookupService airLookupService;

    private MockRestServiceServer mockServer;

    private static Map<String, Object> caseDataMap() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("ccdId", "123456");

        return dataMap;
    }

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        pdfGenerationService = new DocmosisPdfGenerationService(PDF_SERVICE_URI, "bla2", restTemplate);
    }

    @Test
    public void generatePdfDocument() {
        mockServer.expect(requestTo(PDF_SERVICE_URI))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(FILE_CONTENT, MediaType.APPLICATION_JSON));

        byte[] result = pdfGenerationService.generatePdf(DocumentHolder.builder().template(new Template("dl6-template.doc", "dl6")).placeholders(PLACEHOLDERS).build());
        assertNotNull(result);
        assertArrayEquals(result, FILE_CONTENT.getBytes());
    }

    @Test
    public void generatePdfDocument400() {
        mockServer.expect(requestTo(PDF_SERVICE_URI))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withBadRequest());

        try {
            pdfGenerationService.generatePdf(DocumentHolder.builder().template(new Template("dl6-template.doc", "dl6")).placeholders(PLACEHOLDERS).build());
            fail("should have thrown bad-request exception");
        } catch (PdfGenerationException e) {
            HttpStatus httpStatus = ((HttpClientErrorException) e.getCause()).getStatusCode();
            assertEquals(HttpStatus.BAD_REQUEST, httpStatus);
        }
    }
}
