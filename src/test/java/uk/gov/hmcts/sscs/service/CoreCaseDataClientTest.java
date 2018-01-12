package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import uk.gov.hmcts.sscs.domain.corecase.CcdCaseResponse;
import uk.gov.hmcts.sscs.exception.CcdException;

@RunWith(MockitoJUnitRunner.class)
public class CoreCaseDataClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<HttpEntity> captor;

    private CoreCaseDataClient coreCaseDataClient;

    private final String ccdUrl = "https://localhost:4451/caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";

    private final String path = "caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";

    @Before
    public void setUp() {
        coreCaseDataClient = new CoreCaseDataClient("https://localhost:4451/",restTemplate);
    }

    @Test
    public void shouldCallAuthApiWithGivenRequestParams() throws Exception {
        String jsonResponse = "{\"id\": 113,\"jurisdiction\": \"SSCS\","
                +
                "\"state\": \"ResponseRequested\",\"case_type_id\": \"Benefit\"";
        given(restTemplate.exchange(eq(ccdUrl), eq(GET), any(HttpEntity.class), eq(Object.class)))
                .willReturn(new ResponseEntity<>(jsonResponse, OK));


        Map<String,Object> body = new HashMap<>();
        body.put("data","{\"id\":\"123\"}");

        ResponseEntity<Object> responseEntity
                = coreCaseDataClient.sendRequest("dfwfwef","sdsvsdfvs",path,GET,body);

        verify(restTemplate).exchange(eq(ccdUrl), eq(GET), captor.capture(), eq(Object.class));
        MultiValueMap<String, String> headers = captor.getValue().getHeaders();

        assertEquals(jsonResponse, responseEntity.getBody());
        assertEquals("dfwfwef",headers.getFirst("Authorization"));
        assertEquals("sdsvsdfvs",headers.getFirst("ServiceAuthorization"));
        assertEquals("application/json", headers.getFirst("Content-Type"));
    }

    @Test(expected = CcdException.class)
    public void shouldHandleException() throws Exception {
        given(restTemplate.exchange(eq(ccdUrl), eq(GET), any(HttpEntity.class), eq(Object.class)))
                .willThrow(new RuntimeException("Error"));

        Map<String,Object> body = new HashMap<>();
        body.put("data","{\"id\":\"123\"}");

        coreCaseDataClient.sendRequest("dfwfwef","sdsvsdfvs",path,GET,body);
    }

    @Test
    public void shouldCallAuthApiWithGivenRequestParamsForGet() throws Exception {
        CcdCaseResponse ccdCaseResponse = new CcdCaseResponse();
        ccdCaseResponse.setId(113);
        ccdCaseResponse.setJurisdiction("SSCS");

        given(restTemplate.getForEntity(eq(ccdUrl), eq(CcdCaseResponse.class)))
                .willReturn(new ResponseEntity<>(ccdCaseResponse, OK));

        ResponseEntity<CcdCaseResponse> responseEntity
                = coreCaseDataClient.get("dfwfwef","sdsvsdfvs",path);

        verify(restTemplate).getForEntity(eq(ccdUrl), eq(CcdCaseResponse.class));

        assertEquals(ccdCaseResponse, responseEntity.getBody());
    }
}
