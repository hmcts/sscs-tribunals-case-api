package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.exceptions.DocumentServiceResponseException;

@Slf4j
@Service
public class ServiceRequestExecutor {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final RestTemplate restTemplate;
    private final AuthTokenGenerator serviceAuthTokenGenerator;
    private final IdamService idamService;

    public ServiceRequestExecutor(RestTemplate restTemplate, AuthTokenGenerator serviceAuthTokenGenerator, IdamService idamService) {
        this.restTemplate = restTemplate;
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
        this.idamService = idamService;
    }

    public PreSubmitCallbackResponse<SscsCaseData> post(
        final Callback<SscsCaseData> payload,
        final String endpoint
    ) {

        requireNonNull(payload, "payload must not be null");
        requireNonNull(endpoint, "endpoint must not be null");

        final String serviceAuthorizationToken = serviceAuthTokenGenerator.generate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, idamService.getIdamOauth2Token());
        headers.set("user-id", idamService.getIdamTokens().getUserId());
        headers.set(SERVICE_AUTHORIZATION, serviceAuthorizationToken);

        HttpEntity<Callback<SscsCaseData>> requestEntity = new HttpEntity<>(payload, headers);

        PreSubmitCallbackResponse<SscsCaseData> response;

        log.info("About to hit the service with url: " + endpoint + " for case id: " + payload.getCaseDetails().getId());

        try {
            response =
                restTemplate
                    .exchange(
                        endpoint,
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<PreSubmitCallbackResponse<SscsCaseData>>() {
                        }
                    ).getBody();

        } catch (RestClientResponseException e) {
            throw new DocumentServiceResponseException("Couldn't call service using API: ", e);
        }

        return response;

    }

}