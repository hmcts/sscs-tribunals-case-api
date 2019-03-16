package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.util.Helper;

public class SubmitDraftTest {

    @Before
    public void setUp() {
        baseURI = "http://localhost:8080";
        useRelaxedHTTPSValidation();
    }

    @Test
    public void givenDraft_shouldBeStoredInCcd() {
        SyaCaseWrapper draftAppeal = new SyaCaseWrapper();
        draftAppeal.setBenefitType(new SyaBenefitType("PIP", "pip benefit"));

        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .body(Helper.asJsonString(draftAppeal))
                .post("/drafts")
                .then()
                .statusCode(HttpStatus.CREATED_201)
                .assertThat().body("id", not(isEmptyOrNullString()));
    }
}
