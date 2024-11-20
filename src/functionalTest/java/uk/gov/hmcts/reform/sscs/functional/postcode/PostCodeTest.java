package uk.gov.hmcts.reform.sscs.functional.postcode;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
public class PostCodeTest {

    @Value("${test-url}")
    private String testUrl;

    @Test
    public void postcodeReturnsRegionalCentre() {
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();

        String response = RestAssured
                .given()
                .when()
                .get("/regionalcentre/" + "AB1 1AB")
                .then()
                .statusCode(HttpStatus.OK.value())
                .and()
                .extract().body().asString();
        assertThat(response).isEqualTo("{\"regionalCentre\": \"Glasgow\"}");
    }

    @Test
    public void postcodeReturnsNotFound() {
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();

        RestAssured
                .given()
                .when()
                .get("/regionalcentre/" + "AA1")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
