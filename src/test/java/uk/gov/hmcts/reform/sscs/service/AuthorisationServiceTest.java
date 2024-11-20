package uk.gov.hmcts.reform.sscs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;


@ExtendWith(MockitoExtension.class)
public class AuthorisationServiceTest {

    @Mock
    private ServiceAuthorisationApi serviceAuthorisationApi;

    private AuthorisationService authorisationService;

    @BeforeEach
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

    @Test
    public void shouldThrowExcpetionWhenNotAuthorise() {
        assertThrows(RuntimeException.class, () -> {
            String serviceAuthHeader = "anyString";
            when(serviceAuthorisationApi.getServiceName(serviceAuthHeader)).thenThrow(FeignException.class);

            authorisationService.authorise(serviceAuthHeader);

            assertFalse(true);
        });
    }

}
