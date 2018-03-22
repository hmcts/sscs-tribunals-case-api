package uk.gov.hmcts.sscs.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class GetSavedCase {
    private String sscsAppealNumber = "123456789a";

    private final String tcaInstance = System.getenv("TEST_URL");

    @Test
    public void retrievecasefromCcd() {
        RestAssured.baseURI = tcaInstance;
        RestAssured.useRelaxedHTTPSValidation();
        
        String response = RestAssured
                .given()
                .when()
                .get("/appeals/" + sscsAppealNumber)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .and()
                .extract().body().asString();
        assertThat(response).doesNotContain("Error while getting case from ccdConnect");
        assertThat(response).contains("No appeal for given id");
    }
}
