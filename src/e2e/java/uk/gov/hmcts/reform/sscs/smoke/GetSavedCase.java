package uk.gov.hmcts.reform.sscs.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class GetSavedCase  extends BaseHandler {

    private final String tcaInstance = System.getenv("TEST_URL");

    @Test
    public void retrieveCaseFromCcd() throws Exception {
        RestAssured.baseURI = tcaInstance;
        RestAssured.useRelaxedHTTPSValidation();

        SscsCaseDetails sscsCaseDetails = createCaseInWithDwpState();
        
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
}
