package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.sscs.exception.IdamException;

@RunWith(MockitoJUnitRunner.class)
public class IdamClientTest {

    @Mock
    private RestTemplate restTemplate;

    private IdamClient idamClient;

    @Before
    public void setUp() {
        idamClient = new IdamClient("user123","http://localhost:4501/","SSCS",restTemplate);
    }

    @Test
    public void shouldSendRequestToIdamApi() throws Exception {
        String authUrl = "http://localhost:4501/testing-support/lease";
        given(restTemplate.postForEntity(eq(authUrl), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>("lmf43rskmgfk34t4t43", HttpStatus.OK));

        String serviceToken = idamClient.post("testing-support/lease");

        assertEquals("lmf43rskmgfk34t4t43", serviceToken);
    }

    @Test(expected = IdamException.class)
    public void shouldHandleException() throws Exception {
        String authUrl = "http://localhost:4501/testing-support/lease";
        given(restTemplate.postForEntity(eq(authUrl), any(HttpEntity.class), eq(String.class)))
                .willThrow(new RuntimeException("error"));

        idamClient.post("testing-support/lease");
    }
}
