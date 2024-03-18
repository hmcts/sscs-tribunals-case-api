package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import feign.FeignException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.exceptions.AuthorisationException;
import uk.gov.hmcts.reform.sscs.service.exceptions.ClientAuthorisationException;

public class AuthorisationServiceTest {

    @Mock
    private ServiceAuthorisationApi serviceAuthorisationApi;

    private AuthorisationService service;

    private static final String SERVICE_NAME = "SSCS";

    @Before
    public void setup() {
        openMocks(this);
        service = new AuthorisationService(serviceAuthorisationApi);
    }

    @Test
    public void authoriseClientRequest() {
        when(serviceAuthorisationApi.getServiceName(any())).thenReturn(SERVICE_NAME);

        assertTrue(service.authorise(SERVICE_NAME));
    }

    @Test(expected = ClientAuthorisationException.class)
    public void shouldHandleAnAuthorisationException() {
        when(serviceAuthorisationApi.getServiceName(any())).thenThrow(new CustomFeignException(400, ""));
        service.authorise(SERVICE_NAME);
    }

    @Test(expected = AuthorisationException.class)
    public void shouldHandleAnUnknownFeignException() {
        when(serviceAuthorisationApi.getServiceName(any())).thenThrow(new CustomFeignException(501, ""));
        service.authorise(SERVICE_NAME);
    }

    @Test(expected = AuthorisationException.class)
    public void shouldHandleAnUnknownFeignException2() {
        when(serviceAuthorisationApi.getServiceName(any())).thenThrow(new CustomFeignException(399, ""));
        service.authorise(SERVICE_NAME);
    }

    private class CustomFeignException extends FeignException {
        public CustomFeignException(int status, String message) {
            super(status, message);
        }
    }

}
