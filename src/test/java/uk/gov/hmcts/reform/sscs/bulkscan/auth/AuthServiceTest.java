package uk.gov.hmcts.reform.sscs.bulkscan.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.UnauthorizedException;

@RunWith(MockitoJUnitRunner.class)
public class AuthServiceTest {

    private static final String SERVICE_HEADER = "test-header";

    private static final String CCD_DATA = "ccd_data";

    @Mock
    private AuthTokenValidator validator;

    private AuthService service;

    @Before
    public void setUp() {
        service = new AuthService(validator, "ccd_data");
    }

    @After
    public void tearDown() {
        reset(validator);
    }

    @Test
    public void should_throw_unauthenticated_exception_when_auth_header_is_missing() {
        // when
        Throwable exception = catchThrowable(() -> service.authenticate(null));

        // then
        assertThat(exception)
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Missing ServiceAuthorization header");

        // and
        verify(validator, never()).getServiceName(anyString());
    }

    @Test
    public void should_throw_invalid_token_exception_when_invalid_token_is_received() {
        // given
        willThrow(InvalidTokenException.class).given(validator).getServiceName(anyString());

        // when
        Throwable exception = catchThrowable(() -> service.authenticate(SERVICE_HEADER));

        // then
        assertThat(exception).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void should_authenticate_and_return_service_name_when_valid_token_is_received() {
        // given
        given(validator.getServiceName(SERVICE_HEADER)).willReturn(CCD_DATA);

        // when
        String serviceName = service.authenticate(SERVICE_HEADER);

        // then
        assertThat(serviceName).isEqualTo(CCD_DATA);
    }

    @Test
    public void should_not_throw_any_exception_when_service_is_allowed_to_trigger_callback() {
        assertThatCode(() -> service.assertIsAllowedToHandleCallback(CCD_DATA)).doesNotThrowAnyException();
    }

    @Test
    public void should_throw_unauthorized_exception_when_service_is_not_allowed_to_trigger_callback() {
        assertThatCode(() -> service.assertIsAllowedToHandleCallback("test_service"))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("Service test_service does not have permissions to request case creation");
    }
}
