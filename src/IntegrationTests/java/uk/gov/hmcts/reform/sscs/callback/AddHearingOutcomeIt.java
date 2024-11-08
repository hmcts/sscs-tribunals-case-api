package uk.gov.hmcts.reform.sscs.callback;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration")
@TestPropertySource(locations = "classpath:config/application_it.properties")
public class AddHearingOutcomeIt extends AbstractEventIt {
    private static final String PATH_HEARING = "/hearings";
    private static final String PATH_CASE_ID = "12345656789";
    private static final String QUERY_PARAM = "?status=COMPLETED";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String JSON_RESPONSE = "application/json;charset=UTF-8";
    private static final String WIREMOCK_TOKEN_RESULT = "{\"access_token\":\"TOKEN\",\"token_type\":\"Bearer\","
            + "\"scope\": \"openid profile roles\",\"expires_in\":28800}";
    private static final String WIREMOCK_USERINFO_RESULT = "{\"id\":\"1\",\"email\":\"email\","
            + "\"roles\": [\"role\"]}";
    private static final String EMPTY_HMC_RESPONSE = "{\"caseRef\":12345656789, \"caseHearings\":[]}";

    private static final String JWT_TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
            + ".eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlRlc3QgVE9LRU4iLCJpYXQiOjE1MTYyMzkwMjJ9"
            + ".jnE_Fpsnpfvr7KjlQhduoIb5WlklXGwxuMlLoGIhDo4";

    private static WireMockServer hmcServer;
    private static WireMockServer idamServer;
    private static WireMockServer s2sServer;

    @BeforeEach
    public void setup() throws IOException {
        setup("callback/addHearingOutcomeCallback.json");
    }

    @BeforeAll
    public static void setupWireMocks() {
        hmcServer = new WireMockServer(options().port(10010));
        hmcServer.start();
        idamServer = new WireMockServer(options().port(10002));
        idamServer.start();
        s2sServer = new WireMockServer(options().port(10004));
        s2sServer.start();
        idamServer.stubFor(WireMock.post(
                        urlEqualTo("/o/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE_HEADER, JSON_RESPONSE)
                        .withBody(WIREMOCK_TOKEN_RESULT)));
        idamServer.stubFor(WireMock.get(
                        urlEqualTo("/o/userinfo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE_HEADER, JSON_RESPONSE)
                        .withBody(WIREMOCK_USERINFO_RESULT)));
        s2sServer.stubFor(WireMock.post(
                        urlEqualTo("/lease"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE_HEADER, JSON_RESPONSE)
                        .withBody(JWT_TEST_TOKEN)));
    }

    @AfterAll
    static void cleanUp() {
        hmcServer.stop();
        idamServer.stop();
        s2sServer.stop();
    }

    @Test
    public void addHearingOutcomeShouldLoadDropdownValues() throws Exception {
        hmcServer.stubFor(WireMock.get(
                        urlEqualTo(PATH_HEARING + "/" + PATH_CASE_ID + QUERY_PARAM))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getJson("json/hmcResponseForTest.json"))));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertFalse(result.getData().getHearingOutcomeValue().getCompletedHearings().getListItems().isEmpty());
        assertEquals("2030011049", result.getData().getHearingOutcomeValue().getCompletedHearings().getListItems().get(0).getCode());
    }

    @Test
    public void noHearingsOnCaseShouldReturnError() throws Exception {
        hmcServer.stubFor(WireMock.get(
                        urlEqualTo(PATH_HEARING + "/" + PATH_CASE_ID + QUERY_PARAM))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(EMPTY_HMC_RESPONSE)));
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().contains("There are no completed hearings on the case."));
    }


    @Test
    public void hmcErrorShouldBeCaughtByHandler() throws Exception {
        hmcServer.stubFor(WireMock.get(
                        urlEqualTo(PATH_HEARING + "/" + PATH_CASE_ID + QUERY_PARAM))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal server error")));
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().contains("There was an error while retrieving hearing details; please try again after some time."));
    }

    @Test void callToAboutToSubmitEventHandler_willSaveNewHearingOutcome() throws Exception {
        setup("callback/addHearingOutcomeAboutToSubmitCallback.json");

        hmcServer.stubFor(WireMock.get(
                        urlEqualTo(PATH_HEARING + "/" + PATH_CASE_ID + QUERY_PARAM))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getJson("json/hmcResponseForTest.json"))));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertNull(result.getData().getHearingOutcomeValue().getHearingOutcomeId());
        assertNull(result.getData().getHearingOutcomeValue().getDidPoAttendHearing());
        assertNull(result.getData().getHearingOutcomeValue().getCompletedHearings());
        assertEquals(1, result.getData().getHearingOutcomes().size());
        assertEquals("2030026198", result.getData().getHearingOutcomes().get(0).getValue().getHearingOutcomeId());
        assertEquals("2030011049", result.getData().getHearingOutcomes().get(0).getValue().getCompletedHearingId());
        assertEquals(YesNo.YES, result.getData().getHearingOutcomes().get(0).getValue().getDidPoAttendHearing());
        assertEquals(LocalDateTime.of(2024,2,21,11,00), result.getData().getHearingOutcomes().get(0).getValue().getHearingStartDateTime());
        assertEquals(HearingChannel.FACE_TO_FACE, result.getData().getHearingOutcomes().get(0).getValue().getHearingChannelId());
        assertEquals("372653", result.getData().getHearingOutcomes().get(0).getValue().getEpimsId());
    }

    @Test void callToAboutToSubmitEventHandler_withIncorrectHearingIdShouldReturnError() throws Exception {
        setup("callback/addHearingOutcomeWithMissingHearingDetailsCallback.json");

        hmcServer.stubFor(WireMock.get(
                        urlEqualTo(PATH_HEARING + "/" + PATH_CASE_ID + QUERY_PARAM))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getJson("json/hmcResponseForTest.json"))));

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertTrue(result.getErrors().contains("Cannot find hearing details for hearing 21 Feb 2024, 11:00:00 AM, Cardiff with hearing ID: 1030011048"));
    }

}