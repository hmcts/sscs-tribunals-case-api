package uk.gov.hmcts.reform.sscs.functional.ccd;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler.getJsonCallbackForTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.functional.mya.BaseFunctionTest;

@Slf4j
public class CcdMidEventControllerFunctionalTest extends BaseFunctionTest {

    public static final String ADJOURN_CASE_GAPS_CALLBACK_JSON = "handlers/adjourncase/adjournCaseGapsCallback.json";
    public static final String WRITE_FINAL_DECISION_CALLBACK_JSON = "handlers/writefinaldecision/writeFinalDecisionCallback.json";
    @Autowired
    protected ObjectMapper objectMapper;

    public CcdMidEventControllerFunctionalTest() {
        baseURI = baseUrl;
        useRelaxedHTTPSValidation();
    }

    @DisplayName("Should return 400 for no body")
    @Test
    public void shouldReturn400ForNoBody() throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(""), "");
        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(400);
    }

    @DisplayName("Should return 500 for unauthorised")
    @Test
    public void testUnauthorised() throws IOException {
        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(false)
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", "Invalid Value"))
                .header(new Header("Authorization", idamService.getIdamOauth2Token()))
                .body(getJsonCallbackForTest(WRITE_FINAL_DECISION_CALLBACK_JSON))
                //.useRelaxedHttpsValidation()
                .post("/ccdMidEvent")
                .then()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @DisplayName("Should return HttpStatus.SC_OK with valid request")
    @Test
    public void shouldReturnStatusOkForValidRequest() throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(
            getJsonCallbackForTest(WRITE_FINAL_DECISION_CALLBACK_JSON)), "");
        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    }

    @DisplayName("Adjourn case populate venue dropdown should populate next hearing venue dropdown")
    @Test
    public void testAdjournCasePopulateVenueDropdown() throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(
            getJsonCallbackForTest(WRITE_FINAL_DECISION_CALLBACK_JSON)), "AdjournCasePopulateVenueDropdown");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        DynamicList adjournCaseNextHearingVenueSelected = ccdEventResponse.getData().getAdjournment().getNextHearingVenueSelected();
        assertThat(adjournCaseNextHearingVenueSelected.getValue().getCode()).isEqualTo("");
        assertThat(adjournCaseNextHearingVenueSelected.getValue().getLabel()).isEqualTo("");
        assertThat(adjournCaseNextHearingVenueSelected.getListItems()).hasSizeGreaterThan(2);
    }

    @DisplayName("Preview final decision should populate final decision preview document")
    @Test
    public void testPreviewFinalDecision() throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(
            getJsonCallbackForTest(WRITE_FINAL_DECISION_CALLBACK_JSON)), "PreviewFinalDecision");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        assertThat(ccdEventResponse.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument()).isNotNull();
    }

    @DisplayName("Preview adjourn case should populate adjourn case preview document")
    @Test
    public void testPreviewAdjournCaseGaps() throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(getJsonCallbackForTest(
            ADJOURN_CASE_GAPS_CALLBACK_JSON)), "PreviewAdjournCase");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        assertThat(ccdEventResponse.getData().getAdjournment().getPreviewDocument()).isNotNull();
    }

    @DisplayName("Admin restore cases should return an error for data without restore cases date")
    @Test
    public void testAdminRestoreCasesGaps() throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(getJsonCallbackForTest(
            ADJOURN_CASE_GAPS_CALLBACK_JSON)), "AdminRestoreCases");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        assertThat(ccdEventResponse.getWarnings()).isEmpty();
        assertThat(ccdEventResponse.getErrors())
            .hasSize(1)
            .containsOnly("Unable to extract restoreCaseFileName");
    }

    private CcdEventResponse getCcdEventResponse(HttpResponse httpResponse) throws IOException {
        String response = EntityUtils.toString(httpResponse.getEntity());
        return objectMapper.readValue(response, CcdEventResponse.class);
    }
}
