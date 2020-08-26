package uk.gov.hmcts.reform.sscs.functional.tya;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@Slf4j
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class GetAppealStatus extends BaseHandler {

    @Value("${test-url}")
    private String testUrl;

    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testDwpRespond() throws Exception {

        SscsCaseDetails sscsCaseDetails = createCaseInWithDwpState();

        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();

        log.info("Get appeals for case {}", sscsCaseDetails.getId());

        String response = RestAssured
                .given()
                .when()
                .get("appeals?mya=true&caseId=" + sscsCaseDetails.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .and()
                .extract().body().asString();
        assertThat(response).contains("status\":\"WITH_DWP");
    }

    @Test
    public void testResponseReceived() throws Exception {
        SscsCaseDetails sscsCaseDetails = createCaseInResponseReceivedState();

        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();

        log.info("Get appeals for case {}", sscsCaseDetails.getId());

        String response = RestAssured
                .given()
                .when()
                .get("appeals?mya=true&caseId=" + sscsCaseDetails.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .and()
                .extract().body().asString();
        assertThat(response).contains("status\":\"DWP_RESPOND");

    }
}
