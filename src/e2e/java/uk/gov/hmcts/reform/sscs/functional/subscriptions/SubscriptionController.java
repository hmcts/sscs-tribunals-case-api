package uk.gov.hmcts.reform.sscs.functional.subscriptions;

import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

public class SubscriptionController {
    @Value("${test-url}")
    private String testUrl;

    @Test
    public void shouldValidateToken() {
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured
                .given()
                .when()
                .get("/token/abcde12345")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }
}
