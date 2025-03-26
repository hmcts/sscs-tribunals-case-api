package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;


@RunWith(MockitoJUnitRunner.class)
public class AuthorisationServiceTest {

    @Mock
    private ServiceAuthorisationApi serviceAuthorisationApi;

    private AuthorisationService authorisationService;

    @Before
    public void setUp() {
        authorisationService = new AuthorisationService(serviceAuthorisationApi);
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

}
