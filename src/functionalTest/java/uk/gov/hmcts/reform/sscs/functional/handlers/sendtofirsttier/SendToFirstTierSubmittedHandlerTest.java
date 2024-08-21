package uk.gov.hmcts.reform.sscs.functional.handlers.sendtofirsttier;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_functional.properties")
@SpringBootTest
public class SendToFirstTierSubmittedHandlerTest extends BaseHandler {
    protected static final String CASE_ID_TO_BE_REPLACED = "12345656789";
    @Autowired
    private ObjectMapper mapper;

    @DisplayName("Given about to submit callback for send to first tier event, should set fields")
    @Test
    public void testSendToFirstTierSubmitted() throws IOException {
        String jsonCallback = getJsonCallbackForTest("callback/sendToFirstTierRequest.json");
        Callback<SscsCaseData> callback = deserializer.deserialize(jsonCallback);

        SscsCaseDetails caseDetails = createCase();
        String caseId = caseDetails.getId().toString();
        callback = addCaseIdtoCallback(callback, caseId);

        String response = RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header("ServiceAuthorization", idamTokens.getServiceAuthorization()))
            .header(new Header("Authorization", idamTokens.getIdamOauth2Token()))
            .body(callback)
            .post("/ccdSubmittedEvent")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .log().all(true).extract().body().asString();

        JsonNode root = mapper.readTree(response);
        SscsCaseData result = mapper.readValue(root.path("data").toPrettyString(), new TypeReference<>(){});

        assertThat(result.getDwpState()).describedAs(caseId)
                .isNull();
        assertThat(result.getInterlocReferralReason()).describedAs(caseId)
                .isNull();
        assertThat(result.getInterlocReviewState()).describedAs(caseId)
                .isNull();
    }

    private Callback<SscsCaseData> addCaseIdtoCallback(Callback<SscsCaseData> sscsCaseDataCallback, String id) {
        String jsonCallback = serializeSscsCallback(sscsCaseDataCallback);
        jsonCallback = jsonCallback.replace(CASE_ID_TO_BE_REPLACED, id);
        return deserializer.deserialize(jsonCallback);
    }
}
