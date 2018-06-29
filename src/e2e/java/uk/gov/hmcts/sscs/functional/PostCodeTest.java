package uk.gov.hmcts.sscs.functional;

import io.restassured.RestAssured;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class PostCodeTest {
    public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json;charset=UTF-8";

    private final String tcaInstance = System.getenv("TEST_URL");
    private final String localInstance = "http://localhost:8080";

    @Test
    public void postcodeReturnsRegionalCentre() {
        RestAssured.baseURI = StringUtils.isNotBlank(tcaInstance) ? tcaInstance : localInstance;
        RestAssured.useRelaxedHTTPSValidation();

        String response = RestAssured
                .given()
                .when()
                .get("/regionalcentre/" + "AB1")
                .then()
                .statusCode(HttpStatus.OK.value())
                .and()
                .extract().body().asString();
        assertThat(response).isEqualTo("{\"regionalCentre\": \"Glasgow\"}");
    }

    @Test
    public void postcodeReturnsNotFound() {
        RestAssured.baseURI = StringUtils.isNotBlank(tcaInstance) ? tcaInstance : localInstance;
        RestAssured.useRelaxedHTTPSValidation();

        RestAssured
                .given()
                .when()
                .get("/regionalcentre/" + "AA1")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
