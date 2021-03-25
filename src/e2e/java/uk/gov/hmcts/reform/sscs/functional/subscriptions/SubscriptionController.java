package uk.gov.hmcts.reform.sscs.functional.subscriptions;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
        String response = RestAssured
                .given()
                .when()
                .get("/token/abcde12345")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .and()
                .extract().body().asString();
        assertThat(response).isEqualTo("{\"token\":"
                + "{\"decryptedToken\":\"de-crypted-token\",\"benefitType\":\"002\","
                + "\"subscriptionId\":\"subscriptionId\","
                + "\"appealId\":\"dfdsf435345\"}}");
    }
}
