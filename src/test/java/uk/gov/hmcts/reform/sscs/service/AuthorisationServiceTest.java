package uk.gov.hmcts.reform.sscs.service;

import static feign.Request.HttpMethod.GET;
import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.AuthCheckerException;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.BearerTokenInvalidException;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.BearerTokenMissingException;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.UnauthorisedServiceException;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.core.service.ServiceResolver;
import uk.gov.hmcts.reform.auth.parser.idam.core.service.token.ServiceTokenInvalidException;
import uk.gov.hmcts.reform.auth.parser.idam.core.service.token.ServiceTokenParsingException;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.UnauthorizedException;
import uk.gov.hmcts.reform.sscs.service.exceptions.AuthorisationException;
import uk.gov.hmcts.reform.sscs.service.exceptions.ClientAuthorisationException;

@ExtendWith(MockitoExtension.class)
public class AuthorisationServiceTest {

    private static final String SERVICE_HEADER = "test-header";
    private static final String CCD_DATA = "ccd_data";

    @Mock
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @Mock
    private ServiceResolver serviceResolver;

    private AuthorisationService authorisationService;

    @BeforeEach
    public void setUp() {
        authorisationService =
                new AuthorisationService(serviceAuthorisationApi, serviceResolver,
                        List.of("ccd_data"), List.of("sscs"), List.of("ccd_data"),
                        List.of("bulk_scan_processor", "bulk_scan_orchestrator"));
        ReflectionTestUtils.setField(authorisationService, "sscsOnlyEndpoints", List.of("/document"));
    }

    @AfterEach
    public void tearDown() {
        reset(serviceAuthorisationApi);
    }

    @Test
    @DisplayName("Should authorise successfully when service API returns a name")
    public void shouldAuthorise() {
        when(serviceAuthorisationApi.getServiceName(SERVICE_HEADER)).thenReturn(anyString());

        assertTrue(authorisationService.authorise(SERVICE_HEADER));

        verify(serviceAuthorisationApi).getServiceName(SERVICE_HEADER);
    }

    @Test
    @DisplayName("Should throw RuntimeException when Feign client returns exception")
    public void shouldThrowRuntimeExceptionWhenNotAuthorised() {
        when(serviceAuthorisationApi.getServiceName(SERVICE_HEADER)).thenThrow(FeignException.class);

        assertThrows(RuntimeException.class, () -> authorisationService.authorise(SERVICE_HEADER));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when auth header is missing")
    public void should_throw_unauthenticated_exception_when_auth_header_is_missing() {
        assertThrows(
                UnauthorizedException.class,
                () -> authorisationService.assertIsAllowedToHandleCallback(null),
                "Missing ServiceAuthorization header"
        );
        verify(serviceAuthorisationApi, never()).getServiceName(anyString());
    }

    @Test
    @DisplayName("Should propagate InvalidTokenException when service token invalid")
    public void should_throw_invalid_token_exception_when_invalid_token_is_received() {
        doThrow(InvalidTokenException.class).when(serviceAuthorisationApi).getServiceName(anyString());
        assertThrows(InvalidTokenException.class,
                () -> authorisationService.assertIsAllowedToHandleCallback(SERVICE_HEADER));
    }

    @Test
    @DisplayName("Should not throw any exception when service is allowed to trigger callback")
    public void should_not_throw_any_exception_when_service_is_allowed_to_trigger_callback() {
        when(serviceAuthorisationApi.getServiceName(CCD_DATA)).thenReturn(CCD_DATA);
        assertDoesNotThrow(() -> authorisationService.assertIsAllowedToHandleCallback(CCD_DATA));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when service is not allowed to trigger callback")
    public void should_throw_unauthorized_exception_when_service_is_not_allowed_to_trigger_callback() {
        given(serviceAuthorisationApi.getServiceName("invalid_service")).willReturn("not_allowed_service");
        assertThrows(ForbiddenException.class,
                () -> authorisationService.assertIsAllowedToHandleCallback("invalid_service"),
                "Service not_allowed_service does not have permissions to request case creation");
    }

    @Test
    @DisplayName("Should throw ClientAuthorisationException when authenticate gets 4xx FeignException")
    public void should_throw_ClientAuthorisationException_when_authenticate_gets_4xx() {
        when(serviceAuthorisationApi.getServiceName(SERVICE_HEADER))
                .thenThrow(createFeignException(401, "Unauthorized"));

        assertThrows(ClientAuthorisationException.class,
                () -> authorisationService.assertIsAllowedToHandleCallback(SERVICE_HEADER));
    }

    @Test
    @DisplayName("Should throw AuthorisationException when authenticate gets non-4xx FeignException")
    public void should_throw_AuthorisationException_when_authenticate_gets_non_4xx() {
        when(serviceAuthorisationApi.getServiceName(SERVICE_HEADER))
                .thenThrow(createFeignException(500, "Internal Server Error"));

        assertThrows(AuthorisationException.class,
                () -> authorisationService.assertIsAllowedToHandleCallback(SERVICE_HEADER));
    }

    @Test
    @DisplayName("Should throw ClientAuthorisationException for 400-level Feign exception")
    public void shouldThrowClientAuthorisationExceptionWhenNotAuthorised() {
        when(serviceAuthorisationApi.getServiceName(SERVICE_HEADER))
                .thenThrow(createFeignException(400, "Bad Request"));
        assertThrows(ClientAuthorisationException.class, () -> authorisationService.authorise(SERVICE_HEADER));
    }

    @ParameterizedTest
    @CsvSource({
        "502, Bad Gateway",
        "307, Temporary Redirect"
    })
    @DisplayName("Should throw AuthorisationException for non-400 Feign exceptions")
    public void shouldHandleFeignExceptions(int status, String message) {
        when(serviceAuthorisationApi.getServiceName(any()))
                .thenThrow(createFeignException(status, message));
        assertThrows(AuthorisationException.class, () -> authorisationService.authorise(SERVICE_HEADER));
    }

    @Test
    @DisplayName("Should throw BearerTokenMissingException when header missing")
    public void should_throw_BearerTokenMissingException_when_header_missing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BearerTokenMissingException.class, () -> authorisationService.authorise(request));
    }

    @Test
    @DisplayName("Should throw BearerTokenInvalidException when service token invalid")
    public void should_throw_BearerTokenInvalidException_when_service_token_invalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER, "invalid-token");
        when(serviceResolver.getTokenDetails("invalid-token")).thenThrow(new ServiceTokenInvalidException());

        assertThrows(BearerTokenInvalidException.class, () -> authorisationService.authorise(request));
    }

    @Test
    @DisplayName("Should throw AuthCheckerException when token parsing fails")
    public void should_throw_AuthCheckerException_when_token_parsing_fails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER, "malformed-token");
        when(serviceResolver.getTokenDetails("malformed-token")).thenThrow(new ServiceTokenParsingException());

        assertThrows(AuthCheckerException.class, () -> authorisationService.authorise(request));
    }

    @ParameterizedTest(name = "{0} with URI {1} should be allowed")
    @CsvSource({
        "sscs, /document",
        "ccd_data, /ccdCallback",
        "bulk_scan_processor, /forms"
    })
    @DisplayName("Should allow access for valid service and matching URI")
    void should_allow_access_for_valid_service_and_uri(String serviceName, String uri) {
        MockHttpServletRequest request = buildRequest(serviceName, uri);

        assertDoesNotThrow(() -> authorisationService.authorise(request));
    }

    @ParameterizedTest(name = "{0} with URI {1} should NOT be allowed")
    @CsvSource({
        "sscs, /wrong",
        "ccd_data, /wrong",
        "bulk_scan_processor, /wrong",
        "bulk_scan_orchestrator, /wrong",
        "unknown_service, /anything"
    })
    @DisplayName("Should reject access for invalid service or non-matching URI")
    void should_reject_access_for_invalid_service_or_uri(String serviceName, String uri) {
        MockHttpServletRequest request = buildRequest(serviceName, uri);

        assertThrows(UnauthorisedServiceException.class,
                () -> authorisationService.authorise(request));
    }

    private FeignException createFeignException(int status, String message) {
        var feignRequest =
                Request.create(GET, "URL", Map.of(), "body".getBytes(), defaultCharset(), new RequestTemplate());
        if (status >= 400 && status <= 499) {
            return new FeignException.FeignClientException(status, message, feignRequest, "body".getBytes(), null);
        } else {
            return new FeignException.FeignServerException(status, message, feignRequest, "body".getBytes(), null);
        }
    }

    private MockHttpServletRequest buildRequest(String serviceName, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        request.addHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER, SERVICE_HEADER);

        Service service = new Service(serviceName);
        when(serviceResolver.getTokenDetails(SERVICE_HEADER)).thenReturn(service);

        ReflectionTestUtils.setField(authorisationService, "sscsOnlyEndpoints", List.of("/document"));
        ReflectionTestUtils.setField(authorisationService, "ccdOnlyEndpoints", List.of("/ccdCallback"));
        ReflectionTestUtils.setField(authorisationService, "bulkScanOnlyEndpoints", List.of("/forms"));

        return request;
    }
}
