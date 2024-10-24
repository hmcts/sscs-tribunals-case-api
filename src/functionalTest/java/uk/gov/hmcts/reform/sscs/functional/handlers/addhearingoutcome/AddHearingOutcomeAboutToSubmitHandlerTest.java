package uk.gov.hmcts.reform.sscs.functional.handlers.addhearingoutcome;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class AddHearingOutcomeAboutToSubmitHandlerTest  extends BaseHandler {

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void givenAboutToSubmitCallbackForEvent_shouldSetFields() throws Exception {

        String response = RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
                .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
                .body(getJsonCallbackForTest("handlers/addhearingoutcome/addHearingOutcomeAboutToSubmitCallback.json"))
                .post("/ccdAboutToSubmit")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all(true)
                .extract().body().asString();

        JsonNode root = mapper.readTree(response);
        SscsCaseData result = mapper.readValue(root.path("data").toPrettyString(), new TypeReference<SscsCaseData>(){});

        assertThat(result.getHearingOutcomes().size()).isEqualTo(1);
        assertThat(result.getHearingOutcomeValue()).isEqualTo(HearingOutcomeValue.builder().build());
    }
}
