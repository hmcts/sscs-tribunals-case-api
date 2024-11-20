package uk.gov.hmcts.reform.sscs.exceptions;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import feign.Request;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.sscs.model.HmcFailureMessage;
import uk.gov.hmcts.reform.sscs.service.AppInsightsService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApi;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.sscs.service"})
@ActiveProfiles("integration")
@TestPropertySource(locations = "classpath:config/application_it.properties")
public class FeignClientErrorDecoderIntegrationTest {

    private static final String PATH_HEARING = "/hearing";
    private static final String STATUS = "status";
    private static final String IDAM_OAUTH2_TOKEN = "test-idam-token";
    private static final String SERVICE_AUTHORIZATION_TOKEN = "test-s2s-token";
    private static final String FIELD_ID = "id";
    private static final String CASE_ID = "123";

    @Autowired
    private HmcHearingApi hmcHearingApi;

    private static WireMockServer wireMockServer;

    @MockBean
    private AppInsightsService appInsightsService;

    private ArgumentCaptor<HmcFailureMessage> argument;

    @BeforeAll
    static void setUp() {
        wireMockServer = new WireMockServer(options().port(10010));
        wireMockServer.start();
    }

    @BeforeEach
    void setUpEach() {
        argument = ArgumentCaptor.forClass(HmcFailureMessage.class);
    }

    @AfterAll
    static void cleanUp() {
        wireMockServer.stop();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void testMockReturnBadRequest400(int statusCode, HttpStatus expectedHttpStatus, String expectedErrorMessage)
            throws JsonProcessingException {
        wireMockServer.stubFor(WireMock.get(
                urlEqualTo(PATH_HEARING + "/" + CASE_ID))
            .willReturn(aResponse()
                .withStatus(statusCode)
                .withBody(expectedErrorMessage.getBytes(StandardCharsets.UTF_8))));

        assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(
                () -> hmcHearingApi.getHearingRequest(
                    IDAM_OAUTH2_TOKEN,
                    SERVICE_AUTHORIZATION_TOKEN,
                    null,
                    CASE_ID,
                    null))
            .extracting(STATUS).isEqualTo(expectedHttpStatus);

        verify(appInsightsService, times(1)).sendAppInsightsEvent(argument.capture());

        assertEquals(Request.HttpMethod.GET.name(), argument.getValue().getRequestType());
        assertEquals(Long.parseLong(CASE_ID), argument.getValue().getCaseID());
        assertEquals(String.valueOf(expectedHttpStatus.value()), argument.getValue().getErrorCode());
        assertEquals(expectedErrorMessage, argument.getValue().getErrorMessage());
    }

    private static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(400, HttpStatus.BAD_REQUEST, "{ \"errors\" : \"Bad Request data\" }"),
            Arguments.of(401, HttpStatus.UNAUTHORIZED, "{ \"errors\" : \"Authorization failed\" }"),
            Arguments.of(403, HttpStatus.FORBIDDEN, "{ \"errors\" : \"Forbidden access\" }"),
            Arguments.of(404, HttpStatus.NOT_FOUND, "{ \"errors\" : \"No data found\" }")
        );
    }
}
