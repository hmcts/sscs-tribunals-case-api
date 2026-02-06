package uk.gov.hmcts.reform.sscs.functional.handlers.dwpuploadresponse;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import junitparams.JUnitParamsRunner;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class DwpUploadResponseAboutToSubmitHandlerTest extends BaseHandler {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Test
    public void givenAboutToSubmitCallback_pip_shouldPopulateCaseFields() throws Exception {
        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest("callback/dwpUploadResponse.json");

        // Use PIP case values in JSON
        jsonCallbackForTest = jsonCallbackForTest.replace("BENEFIT_CODE_PLACEHOLDER", "PIP");
        jsonCallbackForTest = jsonCallbackForTest.replace("BENEFIT_DESCRIPTION_PLACEHOLDER", "Personal Independence Payment");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat()
                .body("data.issueCode", equalTo("CC"))
                .body("data.benefitCode", equalTo("003"))
                .body("data.caseCode", equalTo("003CC"))
                .body("data.sscsDocument.size()", equalTo(2))
                .body("data.sscsDocument[0].value.documentType", equalTo("appellantEvidence"))
                .body("data.dwpState", equalTo("RESPONSE_SUBMITTED_DWP"));
    }

    @Test
    public void givenAboutToSubmitCallback_uc_shouldPopulateCaseFields() throws Exception {
        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest("callback/dwpUploadResponse.json");

        // Use UC case values in JSON
        jsonCallbackForTest = jsonCallbackForTest.replace("BENEFIT_CODE_PLACEHOLDER", "UC");
        jsonCallbackForTest = jsonCallbackForTest.replace("BENEFIT_DESCRIPTION_PLACEHOLDER", "Universal Credit");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat()
                .body("data.issueCode", equalTo("US"))
                .body("data.benefitCode", equalTo("001"))
                .body("data.caseCode", equalTo("001US"))
                .body("data.sscsDocument.size()", equalTo(2))
                .body("data.sscsDocument[1].value.documentType", equalTo("sscs1"))
                .body("data.dwpState", equalTo("RESPONSE_SUBMITTED_DWP"));
    }
}
