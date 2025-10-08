package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.docmosis.config.PdfDocumentConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;

public class DocmosisPdfGenerationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private PdfDocumentConfig pdfDocumentConfig;

    private DocmosisPdfGenerationService pdfGenerationService;

    public static final Map<String, Object> PLACEHOLDERS = caseDataMap();

    public static final String FILE_CONTENT = "Welcome to PDF document service";

    @Before
    public void setup() {
        openMocks(this);
        pdfGenerationService = new DocmosisPdfGenerationService("bla", "bla2", restTemplate);
    }

    private static Map<String, Object> caseDataMap() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("PBANumber", "PBA123456");

        return dataMap;
    }

    @Test
    public void givenADocumentHolder_thenGenerateAPdf_withWelshLogo() {
        PLACEHOLDERS.put(pdfDocumentConfig.getHmctsWelshImgKey(), pdfDocumentConfig.getHmctsWelshImgVal());
        doReturn(createResponseEntity()).when(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));

        byte[] result = pdfGenerationService.generatePdf(DocumentHolder.builder().template(new Template("bla", "bla2")).placeholders(PLACEHOLDERS).build());
        assertThat(result, is(notNullValue()));
        assertThat(result, is(equalTo(FILE_CONTENT.getBytes())));
    }

    @Test
    public void givenADocumentHolder_thenGenerateAPdf() {
        doReturn(createResponseEntity()).when(restTemplate).postForEntity(anyString(), any(), eq(byte[].class));

        byte[] result = pdfGenerationService.generatePdf(DocumentHolder.builder().template(new Template("bla", "bla2")).placeholders(PLACEHOLDERS).build());
        assertThat(result, is(notNullValue()));
        assertThat(result, is(equalTo(FILE_CONTENT.getBytes())));
    }

    private ResponseEntity<byte[]> createResponseEntity() {
        return new ResponseEntity<>(FILE_CONTENT.getBytes(), HttpStatus.OK);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyTemplateName_thenThrowIllegalArgumentException() {
        pdfGenerationService.generatePdf(DocumentHolder.builder().template(null).placeholders(PLACEHOLDERS).build());
    }

    @Test(expected = NullPointerException.class)
    public void emptyPlaceholders_thenThrowIllegalArgumentException() {
        pdfGenerationService.generatePdf(DocumentHolder.builder().template(new Template("bla", "bla2")).placeholders(null).build());
    }
}
