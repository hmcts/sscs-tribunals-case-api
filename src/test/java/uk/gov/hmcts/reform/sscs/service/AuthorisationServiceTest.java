package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assert.assertFalse;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.UnauthorizedException;


@RunWith(MockitoJUnitRunner.class)
public class AuthorisationServiceTest {

    private static final String SERVICE_HEADER = "test-header";

    private static final String CCD_DATA = "ccd_data";

    @Mock
    private ServiceAuthorisationApi serviceAuthorisationApi;

    private AuthorisationService authorisationService;

    @Before
    public void setUp() {
        authorisationService = new AuthorisationService(serviceAuthorisationApi, "ccd_data");
    }

    @After
    public void tearDown() {
        reset(serviceAuthorisationApi);
    }

    @Test
    public void shouldAuthorise() {
        String serviceAuthHeader = "anyString";
        when(serviceAuthorisationApi.getServiceName(serviceAuthHeader)).thenReturn(anyString());

        authorisationService.authorise(serviceAuthHeader);

        verify(serviceAuthorisationApi).getServiceName(serviceAuthHeader);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExcpetionWhenNotAuthorise() {
        String serviceAuthHeader = "anyString";
        when(serviceAuthorisationApi.getServiceName(serviceAuthHeader)).thenThrow(FeignException.class);

        authorisationService.authorise(serviceAuthHeader);

        assertFalse(true);
    }

    @Test
    public void should_throw_unauthenticated_exception_when_auth_header_is_missing() {
        // when
        Throwable exception = catchThrowable(() -> authorisationService.authenticate(null));

        // then
        assertThat(exception)
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Missing ServiceAuthorization header");

        // and
        verify(serviceAuthorisationApi, never()).getServiceName(anyString());
    }

    @Test
    public void should_throw_invalid_token_exception_when_invalid_token_is_received() {
        // given
        willThrow(InvalidTokenException.class).given(serviceAuthorisationApi).getServiceName(anyString());

        // when
        Throwable exception = catchThrowable(() -> authorisationService.authenticate(SERVICE_HEADER));

        // then
        assertThat(exception).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void should_authenticate_and_return_service_name_when_valid_token_is_received() {
        // given
        given(serviceAuthorisationApi.getServiceName(SERVICE_HEADER)).willReturn(CCD_DATA);

        // when
        String serviceName = authorisationService.authenticate(SERVICE_HEADER);

        // then
        assertThat(serviceName).isEqualTo(CCD_DATA);
    }

    @Test
    public void should_not_throw_any_exception_when_service_is_allowed_to_trigger_callback() {
        assertThatCode(() -> authorisationService.assertIsAllowedToHandleCallback(CCD_DATA)).doesNotThrowAnyException();
    }

    @Test
    public void should_throw_unauthorized_exception_when_service_is_not_allowed_to_trigger_callback() {
        assertThatCode(() -> authorisationService.assertIsAllowedToHandleCallback("test_service"))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("Service test_service does not have permissions to request case creation");
    }
}
