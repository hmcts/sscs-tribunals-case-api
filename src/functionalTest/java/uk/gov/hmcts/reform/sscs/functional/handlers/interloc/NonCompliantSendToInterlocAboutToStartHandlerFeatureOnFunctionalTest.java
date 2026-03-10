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
@EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
public class NonCompliantSendToInterlocAboutToStartHandlerFeatureOnFunctionalTest extends BaseHandler {

    @BeforeEach
    public void setUpTest() {
        super.setUp();
    }

    @Test
    public void givenCmInterlocConfidentialityFeatureEnabled_whenAboutToStartNonCompliantSendToInterloc_thenSelectPartyDefaultsToBlank() throws Exception {
        String jsonCallbackForTest = BaseHandler.getJsonCallbackForTest("handlers/interloc/nonCompliantSendToInterlocAboutToStartCallback.json");

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(jsonCallbackForTest)
            .post("/ccdAboutToStart")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .assertThat()
            .body("data.selectedConfidentialityParty.value.code", equalTo(""))
            .body("data.selectedConfidentialityParty.list_items", hasItem(hasEntry("code", "appellant")));
    }
}
