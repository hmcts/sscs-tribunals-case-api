package uk.gov.hmcts.reform.sscs.service;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.exceptions.DocumentServiceResponseException;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class ServiceRequestExecutorTest {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String ENDPOINT = "http://endpoint";
    private static final String SERVICE_TOKEN = randomAlphabetic(32);
    private static final String ACCESS_TOKEN = randomAlphabetic(32);
    private static final String USER_ID = "123";

    @Mock private AuthTokenGenerator serviceAuthTokenGenerator;
    @Mock private RestTemplate restTemplate;

    @Mock private IdamService idamService;
    @Mock private Callback<SscsCaseData> callback;
    @Mock private CaseDetails<SscsCaseData> caseDetails;
    @Mock private PreSubmitCallbackResponse<SscsCaseData> callbackResponse;
    @Mock private ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> responseEntity;


    private ServiceRequestExecutor serviceRequestExecutor;

    @Before
    public void setUp() {
        serviceRequestExecutor = new ServiceRequestExecutor(
                restTemplate,
                serviceAuthTokenGenerator,
                idamService
        );

        when(serviceAuthTokenGenerator.generate()).thenReturn(SERVICE_TOKEN);
        when(idamService.getIdamOauth2Token()).thenReturn(ACCESS_TOKEN);
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().userId(USER_ID).build());
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getId()).thenReturn(1234L);
    }

    @Test
    public void should_invoke_endpoint_with_given_payload_and_return_200_with_no_errors() {

        when(restTemplate
                .exchange(
                        any(String.class),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(ParameterizedTypeReference.class)
                )
        ).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(callbackResponse);

        PreSubmitCallbackResponse<SscsCaseData> response =
                serviceRequestExecutor.post(
                        callback,
                        ENDPOINT

                );

        assertThat(response).isNotNull();
        assertThat(response).isEqualTo(callbackResponse);

        ArgumentCaptor<HttpEntity> requestEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(
                eq(ENDPOINT),
                eq(HttpMethod.POST),
                requestEntityCaptor.capture(),
                any(ParameterizedTypeReference.class)
        );

        HttpEntity actualRequestEntity = requestEntityCaptor.getValue();

        final String actualContentTypeHeader = actualRequestEntity.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        final String actualAcceptHeader = actualRequestEntity.getHeaders().getFirst(HttpHeaders.ACCEPT);
        final String actualUserIdHeader = actualRequestEntity.getHeaders().getFirst("user-id");
        final String actualServiceAuthorizationHeader = actualRequestEntity.getHeaders().getFirst(SERVICE_AUTHORIZATION);
        final String actualAuthorizationHeader = actualRequestEntity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        final Callback<SscsCaseData> actualPostBody = (Callback<SscsCaseData>) actualRequestEntity.getBody();

        assertThat(actualContentTypeHeader).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(actualAcceptHeader).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(actualServiceAuthorizationHeader).isEqualTo(SERVICE_TOKEN);
        assertThat(actualAuthorizationHeader).isEqualTo(ACCESS_TOKEN);
        assertThat(actualUserIdHeader).isEqualTo(USER_ID);
        assertThat(actualPostBody).isEqualTo(callback);

    }

    @Test
    public void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> serviceRequestExecutor.post(null, ENDPOINT))
                .hasMessage("payload must not be null")
                .isExactlyInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> serviceRequestExecutor.post(callback, null))
                .hasMessage("endpoint must not be null")
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void should_handle_http_server_exception_when_calling_api() {

        HttpServerErrorException underlyingException = mock(HttpServerErrorException.class);

        when(restTemplate
                .exchange(
                        eq(ENDPOINT),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(ParameterizedTypeReference.class)
                )).thenThrow(underlyingException);

        assertThatThrownBy(() -> serviceRequestExecutor.post(callback, ENDPOINT))
                .isExactlyInstanceOf(DocumentServiceResponseException.class)
                .hasMessageContaining("Couldn't call service using API")
                .hasCause(underlyingException);

    }

    @Test
    public void should_handle_http_client_exception_when_calling_api() {
        HttpClientErrorException underlyingException = mock(HttpClientErrorException.class);

        when(restTemplate
                .exchange(
                        eq(ENDPOINT),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(ParameterizedTypeReference.class)
                )).thenThrow(underlyingException);

        assertThatThrownBy(() -> serviceRequestExecutor.post(callback, ENDPOINT))
                .isExactlyInstanceOf(DocumentServiceResponseException.class)
                .hasMessageContaining("Couldn't call service using API")
                .hasCause(underlyingException);
    }


}