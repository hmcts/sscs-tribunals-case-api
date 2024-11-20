package uk.gov.hmcts.reform.sscs.service.exceptions;

import static feign.Request.HttpMethod.GET;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Request.HttpMethod;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.sscs.model.HmcFailureMessage;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.service.AppInsightsService;

@Slf4j
public class FeignClientErrorDecoder implements ErrorDecoder {
    public static final Pattern HEARING_PATH_REGEX = Pattern.compile("(.*?/hearing/)(\\d+)");
    public static final Pattern HEARINGS_PATH_REGEX = Pattern.compile("(.*?/hearings/)(\\d+)");
    private final AppInsightsService appInsightsService;
    private final ObjectMapper objectMapper;

    public FeignClientErrorDecoder(AppInsightsService appInsightsService, ObjectMapper objectMapper) {
        this.appInsightsService = appInsightsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        HmcFailureMessage failMsg = extractFailMsg(methodKey, response);

        log.error("Error in calling Feign client. Status code "
                + response.status() + ", methodKey = " + methodKey);

        if (failMsg == null) {
            appInsightsService.sendAppInsightsEvent(getOriginalErrorMessage(response));
        } else {
            try {
                appInsightsService.sendAppInsightsEvent(failMsg);
            } catch (JsonProcessingException e) {
                log.debug("Error serialising event message, falling back to String: {} ", failMsg);
                appInsightsService.sendAppInsightsEvent(failMsg.toString());
            }
        }
        logServiceFailureError(response);

        return new ResponseStatusException(HttpStatus.valueOf(response.status()),
                "Error in calling the client method:" + methodKey);
    }

    private void logServiceFailureError(Response response) {
        Request originalRequest = response.request();
        HttpMethod httpMethod = originalRequest.httpMethod();
        if (GET.equals(httpMethod)) {
            log.error("Error occurred during call to HMC hearing service."
                    + " Status code : " + response.status()
                    + ". Reason : " + response.reason()
                    + ". Message : " + getOriginalErrorMessage(response)
                    + ". Case ID : " + getCaseIdFromPath(response, HEARINGS_PATH_REGEX));
        }
    }

    private HmcFailureMessage extractFailMsg(String methodKey, Response response) {
        Request originalRequest = response.request();
        HttpMethod httpMethod = originalRequest.httpMethod();
        HmcFailureMessage failMsg = null;

        if (httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) {
            HearingRequestPayload payload = null;
            try {
                payload = mapToPostHearingRequest(originalRequest);
            } catch (JsonProcessingException e) {
                log.error("JsonProcessingException when mapping hearing request: "
                        + response.status() + ", methodKey = " + methodKey);
                log.error("Error details: {}", new String(originalRequest.body(), StandardCharsets.UTF_8));
            }
            if (payload != null) {
                failMsg = buildFailureMessage(httpMethod.toString(),
                        Long.valueOf(payload.getCaseDetails().getCaseId()),
                        LocalDateTime.now(),
                        String.valueOf(response.status()),
                        getOriginalErrorMessage(response));
            }
        } else if (GET.equals(httpMethod)) {
            Long caseId = getPathId(response);

            failMsg = buildFailureMessage(httpMethod.toString(),
                    caseId,
                    LocalDateTime.now(),
                    String.valueOf(response.status()),
                    getOriginalErrorMessage(response));
        } else {
            Long caseId = getQueryId(response);

            failMsg = buildFailureMessage(httpMethod.toString(),
                    caseId,
                    LocalDateTime.now(),
                    String.valueOf(response.status()),
                    getOriginalErrorMessage(response));
        }

        return failMsg;
    }

    private Long getPathId(Response response) {
        String url = response.request().requestTemplate().url();
        Matcher matches = HEARING_PATH_REGEX.matcher(url);
        if (matches.find()) {
            return Long.parseLong(matches.group(2));
        }
        return null;
    }

    private Long getCaseIdFromPath(Response response, Pattern patter) {
        String url = response.request().requestTemplate().url();
        Matcher matches = patter.matcher(url);
        if (matches.find()) {
            return Long.parseLong(matches.group(2));
        }
        return null;
    }

    private long getQueryId(Response response) {
        return Long.parseLong(response.request()
                .requestTemplate()
                .queries().get("id")
                .iterator()
                .next());
    }

    private String getOriginalErrorMessage(Response response) {
        try (InputStream bodyIs = response.body().asInputStream()) {
            return new String(bodyIs.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Unable to resolve original error message: %s".formatted(e.getMessage());
        }
    }

    private HmcFailureMessage buildFailureMessage(String method, Long caseId, LocalDateTime timestamp,
                                                  String errorCode, String errorMessage) {
        return HmcFailureMessage.builder()
                .requestType(method)
                .caseID(caseId)
                .timeStamp(timestamp)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    private HearingRequestPayload mapToPostHearingRequest(Request request) throws JsonProcessingException {
        HearingRequestPayload hearingRequestPayload;
        String requestBody = new String(request.body(), StandardCharsets.UTF_8);
        try {
            hearingRequestPayload = objectMapper.readValue(requestBody, HearingRequestPayload.class);
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException when mapping for: {}", requestBody);
            throw e;
        }
        return hearingRequestPayload;
    }

}
