package uk.gov.hmcts.reform.sscs.service.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.sscs.model.HmcFailureMessage;
import uk.gov.hmcts.reform.sscs.model.Message;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.service.AppInsightsService;

@Slf4j
@ExtendWith(MockitoExtension.class)
class FeignClientErrorDecoderTest {

    private static final String ID = "id";
    private static final Long CASE_ID = 1000000000L;
    private static final String ERROR_MSG = " \"Error in calling the client method:someMethod\"";
    private static final Map<String, Collection<String>> headers = new HashMap<>();

    private FeignClientErrorDecoder feignClientErrorDecoder;
    private HearingRequestPayload hearingRequestPayload;
    private ArgumentCaptor<HmcFailureMessage> hmcFailureMessageArgumentCaptor;
    @Mock
    private AppInsightsService appInsightsService;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        feignClientErrorDecoder = new FeignClientErrorDecoder(appInsightsService, OBJECT_MAPPER);
        hearingRequestPayload = new HearingRequestPayload();
        hearingRequestPayload.setCaseDetails(new CaseDetails());
        hearingRequestPayload.getCaseDetails().setCaseId(String.valueOf(CASE_ID));
        hmcFailureMessageArgumentCaptor = ArgumentCaptor.forClass(HmcFailureMessage.class);
    }

    @ParameterizedTest
    @MethodSource("get4xxErrorCodeTestArguments")
    void should_handle_4xx_post_put_error(int statusCode, String expected) throws IOException {
        Request request =
            Request.create(Request.HttpMethod.POST, "url",
                           headers, Request.Body.create(toJsonString(hearingRequestPayload)), null);

        Response response = buildResponse(request, statusCode);

        Throwable throwable = feignClientErrorDecoder.decode("someMethod", response);
        verify(appInsightsService)
            .sendAppInsightsEvent(hmcFailureMessageArgumentCaptor.capture());

        assertThat(throwable).isInstanceOf(ResponseStatusException.class);
        assertEquals(request.httpMethod().toString(), hmcFailureMessageArgumentCaptor.getValue().getRequestType());
        assertEquals(CASE_ID, hmcFailureMessageArgumentCaptor.getValue().getCaseID());
        assertEquals(String.valueOf(statusCode), hmcFailureMessageArgumentCaptor.getValue().getErrorCode());
        assertEquals(new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8),
            hmcFailureMessageArgumentCaptor.getValue().getErrorMessage());

        assertThat(throwable.getMessage()).contains(expected);
    }

    @ParameterizedTest
    @MethodSource("get4xxErrorCodeTestArguments")
    void should_handle_4xx_get_delete_error(int statusCode, String expected) throws IOException {
        Map<String, Collection<String>> queries = new HashMap<>();
        queries.computeIfAbsent(ID, k -> new ArrayList<>()).add(String.valueOf(CASE_ID));
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplate.queries(queries);

        Request request =
            Request.create(Request.HttpMethod.DELETE, "url",
                           headers, Request.Body.empty(), requestTemplate);
        Response response = buildResponse(request, statusCode);

        Throwable throwable = feignClientErrorDecoder.decode("someMethod", response);
        verify(appInsightsService, times(1)).sendAppInsightsEvent(hmcFailureMessageArgumentCaptor.capture());

        assertThat(throwable).isInstanceOf(ResponseStatusException.class);
        assertEquals(request.httpMethod().toString(), hmcFailureMessageArgumentCaptor.getValue().getRequestType());
        assertEquals(CASE_ID, hmcFailureMessageArgumentCaptor.getValue().getCaseID());
        assertEquals(String.valueOf(statusCode), hmcFailureMessageArgumentCaptor.getValue().getErrorCode());
        assertEquals(new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8),
            hmcFailureMessageArgumentCaptor.getValue().getErrorMessage());

        assertThat(throwable.getMessage()).contains(expected);
    }

    @Test
    void testShouldFallbackToStringIfJsonProcessingExceptionOccurs() throws JsonProcessingException {
        Request request =
            Request.create(Request.HttpMethod.POST, "url",
                headers, Request.Body.create(toJsonString(hearingRequestPayload)), null);

        Response response = buildResponse(request, HttpStatus.INTERNAL_SERVER_ERROR.value());

        doThrow(JsonProcessingException.class).when(appInsightsService)
            .sendAppInsightsEvent(any(HmcFailureMessage.class));

        feignClientErrorDecoder.decode("someMethod", response);

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(appInsightsService)
            .sendAppInsightsEvent(stringArgumentCaptor.capture());

        assertThat(stringArgumentCaptor.getValue()).contains("HmcFailureMessage{requestType='POST', caseID=1000000000");
    }

    @Test
    void should_handle_500_error() {
        Request request =
            Request.create(Request.HttpMethod.PUT, "url",
                           headers, Request.Body.create(toJsonString(hearingRequestPayload)), null);
        Response response = Response.builder()
            .request(request)
            .status(500)
            .reason("Internal server error")
            .body("Some body", StandardCharsets.UTF_8)
            .build();

        Throwable throwable = feignClientErrorDecoder.decode("someMethod", response);

        assertThat(throwable).isInstanceOf(ResponseStatusException.class);
        assertThat(throwable.getMessage()).contains("500 INTERNAL_SERVER_ERROR \"Error in calling the client method:someMethod\"");
    }

    @Test
    void testSendAppInsight() throws JsonProcessingException {
        Request request =
            Request.create(Request.HttpMethod.POST, "url",
                           headers, Request.Body.create(toJsonString(hearingRequestPayload)), null);
        Response response = buildResponse(request, 400);

        doThrow(mock(JsonProcessingException.class)).when(appInsightsService).sendAppInsightsEvent(any(Message.class));

        Throwable throwable = feignClientErrorDecoder.decode("someMethod", response);
        assertThat(throwable).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void testShouldThrowMappingException() {
        Request request =
            Request.create(Request.HttpMethod.POST, "url",
                           headers, Request.Body.create(toJsonString(new CaseDetails())), null);
        Response response = buildResponse(request, 400);

        Throwable throwable = feignClientErrorDecoder.decode("someMethod", response);
        assertThat(throwable).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void testHttpGetErrorHandling() {
        RequestTemplate requestTemplate = new RequestTemplate();
        requestTemplate.uri("/hearings/1234");

        Request request =
            Request.create(Request.HttpMethod.GET, "url/1234",
                           headers, Request.Body.create(toJsonString(hearingRequestPayload)), requestTemplate);
        Response response = Response.builder()
            .request(request)
            .status(500)
            .reason("Internal server error")
            .body("Some body", StandardCharsets.UTF_8)
            .build();

        Throwable throwable = feignClientErrorDecoder.decode("someMethod", response);

        assertThat(throwable).isInstanceOf(ResponseStatusException.class);
        assertThat(throwable.getMessage()).contains("500 INTERNAL_SERVER_ERROR \"Error in calling the client method:someMethod\"");
    }

    private String toJsonString(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String jsonString = "";
        try {
            jsonString = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            log.error("JsonProcessingException when mapping for: {}", object);
        }
        return jsonString;
    }


    private static Stream<Arguments> get4xxErrorCodeTestArguments() {
        return Stream.of(
            Arguments.of(400, HttpStatus.BAD_REQUEST + ERROR_MSG),
            Arguments.of(401, HttpStatus.UNAUTHORIZED + ERROR_MSG),
            Arguments.of(403, HttpStatus.FORBIDDEN + ERROR_MSG),
            Arguments.of(404, HttpStatus.NOT_FOUND + ERROR_MSG)
        );
    }

    private Response buildResponse(Request req, int statusCode) {
        String reason = "";
        String bodyMsg = "";

        if (statusCode == 400) {
            reason = HttpStatus.BAD_REQUEST.name();
            bodyMsg = "{ \"errors\" : \"Bad Request data\" }";
        } else if (statusCode == 401) {
            reason = HttpStatus.UNAUTHORIZED.name();
            bodyMsg = "{ \"errors\" : \"Authorization failed\" }";
        } else if (statusCode == 403) {
            reason = HttpStatus.FORBIDDEN.name();
            bodyMsg = "{ \"errors\" : \"Forbidden access\" }";
        } else if (statusCode == 404) {
            reason = HttpStatus.NOT_FOUND.name();
            bodyMsg = "{ \"errors\" : \"No data found\" }";
        }

        return Response.builder()
            .request(req)
            .status(statusCode)
            .reason(reason)
            .body(bodyMsg, StandardCharsets.UTF_8)
            .build();
    }
}
