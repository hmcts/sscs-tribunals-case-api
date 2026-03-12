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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.BearerTokenMissingException;
import uk.gov.hmcts.reform.auth.checker.core.service.ServiceResolver;
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
    public void shouldAuthorise() {
        String serviceAuthHeader = "anyString";
        when(serviceAuthorisationApi.getServiceName(serviceAuthHeader)).thenReturn(anyString());

        assertTrue(authorisationService.authorise(serviceAuthHeader));

        verify(serviceAuthorisationApi).getServiceName(serviceAuthHeader);
    }

    @Test
    public void shouldThrowRuntimeExceptionWhenNotAuthorised() {
        String serviceAuthHeader = "anyString";
        when(serviceAuthorisationApi.getServiceName(serviceAuthHeader)).thenThrow(FeignException.class);

        assertThrows(RuntimeException.class, () -> authorisationService.authorise(serviceAuthHeader));
    }

    @Test
    public void should_throw_unauthenticated_exception_when_auth_header_is_missing() {
        assertThrows(
                UnauthorizedException.class,
                () -> authorisationService.assertIsAllowedToHandleCallback(null),
                "Missing ServiceAuthorization header"
        );
        verify(serviceAuthorisationApi, never()).getServiceName(anyString());
    }

    @Test
    public void should_throw_invalid_token_exception_when_invalid_token_is_received() {
        doThrow(InvalidTokenException.class).when(serviceAuthorisationApi).getServiceName(anyString());
        assertThrows(InvalidTokenException.class,
                () -> authorisationService.assertIsAllowedToHandleCallback(SERVICE_HEADER));
    }

    @Test
    public void should_not_throw_any_exception_when_service_is_allowed_to_trigger_callback() {
        when(serviceAuthorisationApi.getServiceName(CCD_DATA)).thenReturn(CCD_DATA);
        assertDoesNotThrow(() -> authorisationService.assertIsAllowedToHandleCallback(CCD_DATA));
    }

    @Test
    public void should_throw_unauthorized_exception_when_service_is_not_allowed_to_trigger_callback() {
        given(serviceAuthorisationApi.getServiceName("test_service")).willReturn("not_allowed_service");
        assertThrows(ForbiddenException.class,
                () -> authorisationService.assertIsAllowedToHandleCallback("test_service"),
                "Service not_allowed_service does not have permissions to request case creation");
    }

    @Test
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
    public void shouldHandleFeignExceptions(int status, String message) {
        when(serviceAuthorisationApi.getServiceName(any()))
                .thenThrow(createFeignException(status, message));
        assertThrows(AuthorisationException.class, () -> authorisationService.authorise(SERVICE_HEADER));
    }

    @Test
    public void should_throw_BearerTokenMissingException_when_header_missing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BearerTokenMissingException.class, () -> authorisationService.authorise(request));
    }

    private FeignException createFeignException(int status, String message) {
        var feignRequest =
                Request.create(GET, "URL", Map.of(), "body".getBytes(), defaultCharset(), new RequestTemplate());
        return new FeignException.FeignServerException(status, message, feignRequest, "body".getBytes(), null);
    }
}
