package uk.gov.hmcts.reform.sscs.functional.tya;


import io.restassured.RestAssured;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;


@Slf4j
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class DownloadDecisionNotice extends BaseHandler {

    @Autowired
    private PdfStoreService pdfStoreService;

    @Value("${test-url}")
    private String testUrl;

    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    @Ignore
    public void testDownloadDecisionNotice() throws Exception {

        List<SscsDocument> uploaded = uploadDocument();
        String url = uploaded.get(0).getValue().getDocumentLink().getDocumentUrl();

        log.info("Get document from dm-store for {}", url);

        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();

        RestAssured
                .given()
                .queryParam("url", url + "/binary")
                .when()
                .get("document")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    private List<SscsDocument> uploadDocument() throws IOException {
        URL resource = getClass().getClassLoader().getResource("tya/decision-notice.pdf");
        File file = new File(Objects.requireNonNull(resource).getPath());
        return pdfStoreService.store(Files.readAllBytes(file.toPath()), "decision-notice.pdf", DocumentType.FINAL_DECISION_NOTICE.getValue());
    }
}
