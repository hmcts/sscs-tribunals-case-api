package uk.gov.hmcts.reform.sscs.functional.controller;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.sscs.functional.ccd.UpdateCaseInCcdTest.buildSscsCaseDataForTestingWithValidMobileNumbers;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class CcdCallbackControllerTest {

    @Value("${test-url}")
    private String testUrl;
    @Autowired
    private IdamService idamService;
    @Autowired
    private CcdService ccdService;
    private Long ccdCaseId;
    private IdamTokens idamTokens;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
        ccdCaseId = createCaseInCcdInterlocutoryReviewState().getId();
        idamTokens = idamService.getIdamTokens();
    }

    private SscsCaseDetails createCaseInCcdInterlocutoryReviewState() {

        return ccdService.createCase(buildSscsCaseDataForTestingWithValidMobileNumbers(),
            EventType.NON_COMPLIANT.getCcdType(), "non compliant created test case",
            "non compliant created test case", idamTokens);
    }

    @Test
    public void givenSubmittedCallbackForActionFurtherEvidence_shouldUpdateFieldAndTriggerEvent() throws Exception {
        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .body(getJsonCallbackForTest())
            .post("/ccdSubmittedEvent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true)
            .assertThat().body("data.interlocReviewState", equalTo("interlocutoryReview"));
    }

    private String getJsonCallbackForTest() throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
            .getResource("actionFurtherEvidenceCallback.json")).getFile();
        String jsonCallback = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        return jsonCallback.replace("CASE_ID_TO_BE_REPLACED", ccdCaseId.toString());
    }
}

