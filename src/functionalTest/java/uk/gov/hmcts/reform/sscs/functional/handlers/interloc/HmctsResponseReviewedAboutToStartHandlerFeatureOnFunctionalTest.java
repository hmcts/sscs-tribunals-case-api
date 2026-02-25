package uk.gov.hmcts.reform.sscs.functional.handlers.interloc;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CM_INTERLOC_CONFIDENTIALITY_PARTY_ENABLED", matches = "true")
public class HmctsResponseReviewedAboutToStartHandlerFeatureOnFunctionalTest extends BaseHandler {

    @BeforeEach
    public void setUpTest() {
        super.setUp();
    }

    @Test
    public void givenCmInterlocConfidentialityFeatureEnabled_whenAboutToStartHmctsResponseReviewed_thenSelectPartyDefaultsToBlank() throws Exception {
        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest("handlers/interloc/hmctsResponseReviewedAboutToStartCallback.json");

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(jsonCallbackForTest)
            .post("/ccdAboutToStart")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .assertThat()
            .body("data.originalSender.value.code", equalTo(""))
            .body("data.originalSender.list_items", hasItem(hasEntry("code", "appellant")));
    }
}
