package uk.gov.hmcts.reform.sscs.functional.ccd;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.functional.mya.BaseFunctionTest;

@Slf4j
public class CcdMidEventControllerFunctionalTest extends BaseFunctionTest {

    @Autowired
    protected ObjectMapper objectMapper;

    public CcdMidEventControllerFunctionalTest() {
        baseURI = baseUrl;
        useRelaxedHTTPSValidation();
    }

    @Test
    public void shouldReturn400ForNoBody() throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(""), "");
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(400));
    }

    @Test
    public void shouldReturn500ForUnauthorised() throws IOException {
        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(false)
                .contentType(ContentType.JSON)
                .header(new Header("ServiceAuthorization", "Invalid Value"))
                .header(new Header("Authorization", idamService.getIdamOauth2Token()))
                .body(getJsonCallbackForTest("handlers/writefinaldecision/writeFinalDecisionCallback.json"))
                //.useRelaxedHttpsValidation()
                .post("/ccdMidEvent")
                .then()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void shouldReturn200WithValidRequest() throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(getJsonCallbackForTest("handlers/writefinaldecision/writeFinalDecisionCallback.json")), "");
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
    }

    @Test
    public void adjournCasePopulateVenueDropdown_shouldPopulateNextHearingVenueDropdown() throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(getJsonCallbackForTest("handlers/writefinaldecision/writeFinalDecisionCallback.json")), "AdjournCasePopulateVenueDropdown");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        DynamicList adjournCaseNextHearingVenueSelected = ccdEventResponse.getData().getAdjournCaseNextHearingVenueSelected();
        assertThat(adjournCaseNextHearingVenueSelected.getValue(), is(new DynamicListItem("", "")));
        assertThat(adjournCaseNextHearingVenueSelected.getListItems().size(), is(greaterThan(2)));
    }

    @Test
    public void previewFinalDecision_shouldPopulateFinalDecisionPreviewDocument() throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(getJsonCallbackForTest("handlers/writefinaldecision/writeFinalDecisionCallback.json")), "PreviewFinalDecision");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(ccdEventResponse.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument(), is(not(nullValue())));
    }

    @Test
    public void previewAdjournCase_shouldPopulateAdjournCasePreviewDocument() throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(getJsonCallbackForTest("handlers/adjourncase/adjournCaseCallback.json")), "PreviewAdjournCase");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(ccdEventResponse.getData().getAdjournCasePreviewDocument(), is(not(nullValue())));
    }

    @Test
    public void adminRestoreCases_shouldReturnAnErrorForDataWithoutRestoreCasesDate() throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(getJsonCallbackForTest("handlers/adjourncase/adjournCaseCallback.json")), "AdminRestoreCases");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(ccdEventResponse.getWarnings().size(), is(0));
        assertThat(ccdEventResponse.getErrors().size(), is(1));
        assertThat(ccdEventResponse.getErrors().iterator().next(), is("Unable to extract restoreCaseFileName"));
    }

    private CcdEventResponse getCcdEventResponse(HttpResponse httpResponse) throws IOException {
        String response = EntityUtils.toString(httpResponse.getEntity());
        return objectMapper.readValue(response, CcdEventResponse.class);
    }
}
