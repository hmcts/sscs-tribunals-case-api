package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.useRelaxedHTTPSValidation;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.util.Helper;

public class SubmitDraftTest {
    @Test
    public void givenDraft_shouldBeStoredInCcd() {
        useRelaxedHTTPSValidation();

        SyaCaseWrapper syaCaseWrapper = new SyaCaseWrapper();
        syaCaseWrapper.setBenefitType(new SyaBenefitType("PIP", "pip benefit"));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Helper.asJsonString(syaCaseWrapper))
                .expect()
                .statusCode(HttpStatus.CREATED_201)
                .when()
                .post("/drafts");
    }
}
