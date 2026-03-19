package uk.gov.hmcts.reform.sscs.functional.handlers.directionissued;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.APPEAL_TO_PROCEED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.CONFIDENTIALITY_REFUSED_SEND_TO_ADMIN;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.ISSUE_AND_SEND_TO_ADMIN;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.PROVIDE_INFORMATION;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseTestHandler;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DirectionIssuedAboutToStartHandlerFunctionalTest extends BaseTestHandler {

    @Nested
    class CmDirectionTypeToggleOn {
        @Test
        @EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
        public void givenConfidentialityReferral_shouldIncludeConfidentialityDirectionTypes() throws Exception {
            String jsonCallbackForTest = BaseTestHandler.getJsonCallbackForTest(
                "handlers/directionissued/directionIssuedAboutToStartCallback.json");

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToStart")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat().body("data.directionTypeDl.value.code", equalTo(""))
                .assertThat().body("data.directionTypeDl.list_items",
                    hasItem(hasEntry("code", APPEAL_TO_PROCEED.toString())))
                .assertThat().body("data.directionTypeDl.list_items",
                    hasItem(hasEntry("code", PROVIDE_INFORMATION.toString())))
                .assertThat().body("data.directionTypeDl.list_items",
                    hasItem(hasEntry("code", ISSUE_AND_SEND_TO_ADMIN.toString())))
                .assertThat().body("data.directionTypeDl.list_items",
                    hasItem(hasEntry("code", CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN.toString())))
                .assertThat().body("data.directionTypeDl.list_items",
                    hasItem(hasEntry("code", CONFIDENTIALITY_REFUSED_SEND_TO_ADMIN.toString())))
                .assertThat().body("data.directionTypeDl.list_items.size()", equalTo(5));
        }
    }

    @Nested
    class CmDirectionTypeToggleOff {
        @Test
        @DisabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
        public void givenConfidentialityReferralAndFeatureDisabled_shouldNotIncludeConfidentialityDirectionTypes() throws Exception {
            String jsonCallbackForTest = BaseTestHandler.getJsonCallbackForTest(
                "handlers/directionissued/directionIssuedAboutToStartCallback.json");

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(jsonCallbackForTest)
                .post("/ccdAboutToStart")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .assertThat().body("data.directionTypeDl.value.code", equalTo(""))
                .assertThat().body("data.directionTypeDl.list_items",
                    hasItem(hasEntry("code", APPEAL_TO_PROCEED.toString())))
                .assertThat().body("data.directionTypeDl.list_items",
                    hasItem(hasEntry("code", PROVIDE_INFORMATION.toString())))
                .assertThat().body("data.directionTypeDl.list_items",
                    hasItem(hasEntry("code", ISSUE_AND_SEND_TO_ADMIN.toString())))
                .assertThat().body("data.directionTypeDl.list_items",
                    not(hasItem(hasEntry("code", CONFIDENTIALITY_GRANTED_SEND_TO_ADMIN.toString()))))
                .assertThat().body("data.directionTypeDl.list_items",
                    not(hasItem(hasEntry("code", CONFIDENTIALITY_REFUSED_SEND_TO_ADMIN.toString()))))
                .assertThat().body("data.directionTypeDl.list_items.size()", equalTo(3));
        }
    }
}
