package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.springframework.http.HttpMethod.POST;

import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import uk.gov.hmcts.sscs.exception.AuthException;

@RunWith(MockitoJUnitRunner.class)
public class AuthClientTest {

    @Mock
    private OtpGenerator otpGenerator;

    @Mock
    private RestTemplate restTemplate;

    private AuthClient authClient;

    @Before
    public void setUp() {
        authClient = new AuthClient("KWCNVXMFJ6PIZIDX","http://localhost:4502/",otpGenerator,restTemplate);
    }

    @Test
    public void shouldCallAuthApiWithGivenRequestParams() throws Exception {
        given(otpGenerator.issueOneTimePassword("KWCNVXMFJ6PIZIDX")).willReturn("123456");
        String authUrl = "http://localhost:4502/lease?microservice=sscs&oneTimePassword=123456";
        given(restTemplate.exchange(eq(authUrl), eq(POST), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>("lmf43rskmgfk34t4t43", HttpStatus.OK));

        String serviceToken = authClient.sendRequest("lease", POST, "");

        assertEquals("lmf43rskmgfk34t4t43", serviceToken);
    }

    @Test(expected = AuthException.class)
    public void shouldHandleException() throws Exception {
        given(otpGenerator.issueOneTimePassword("KWCNVXMFJ6PIZIDX")).willReturn("123456");
        String authUrl = "http://localhost:4502/lease?microservice=sscs&oneTimePassword=123456";
        given(restTemplate.exchange(eq(authUrl), eq(POST), any(HttpEntity.class), eq(String.class)))
                .willThrow(new RuntimeException("error!!"));

        authClient.sendRequest("lease", POST, "");
    }

    @Test(expected = AuthException.class)
    public void shouldHandleNoSuchAlgorithmException() throws Exception {
        given(otpGenerator.issueOneTimePassword("KWCNVXMFJ6PIZIDX"))
                .willThrow(new NoSuchAlgorithmException("Auth error!!"));
        authClient.sendRequest("lease", POST, "");
    }
}
