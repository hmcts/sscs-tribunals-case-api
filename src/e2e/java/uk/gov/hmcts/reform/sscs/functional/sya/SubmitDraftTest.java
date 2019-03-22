package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.util.SyaServiceHelper;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
public class SubmitDraftTest {

    @Value("${test-url}")
    private String testUrl;

    @Value("${test-oauth2Token}")
    String oauth2Token;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
    }

    @Test
    public void givenDraft_shouldBeStoredInCcd() {
        SyaCaseWrapper draftAppeal = new SyaCaseWrapper();
        draftAppeal.setBenefitType(new SyaBenefitType("PIP", "pip benefit"));

        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .header(new Header(AUTHORIZATION, oauth2Token))
                .body(SyaServiceHelper.asJsonString(draftAppeal))
                .post("/drafts")
                .then()
                .statusCode(HttpStatus.CREATED_201)
                .assertThat().body("id", not(isEmptyOrNullString()));
    }
}
