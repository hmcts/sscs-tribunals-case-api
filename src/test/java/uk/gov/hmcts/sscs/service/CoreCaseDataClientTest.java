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
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.exception.CcdException;

@RunWith(MockitoJUnitRunner.class)
public class CoreCaseDataClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<HttpEntity> captor;

    private CoreCaseDataClient coreCaseDataClient;

    @Before
    public void setUp() {
        coreCaseDataClient = new CoreCaseDataClient("https://localhost:4451/",restTemplate);
    }

    @Test
    public void shouldCallAuthApiWithGivenRequestParamsForSendRequest() throws Exception {
        String ccdUrl = "https://localhost:4451/caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";
        String responseXml = "{\"id\": 113,\"jurisdiction\": \"SSCS\","
                +
                "\"state\": \"ResponseRequested\",\"case_type_id\": \"Benefit\"";
        given(restTemplate.exchange(eq(ccdUrl), eq(GET), any(HttpEntity.class), eq(Object.class)))
                .willReturn(new ResponseEntity<>(responseXml, OK));

        String path = "caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";
        Map<String,Object> body = new HashMap<>();
        body.put("data","{\"id\":\"123\"}");

        ResponseEntity<Object> responseEntity
                = coreCaseDataClient.sendRequest("dfwfwef","sdsvsdfvs",path,GET,body);

        verify(restTemplate).exchange(eq(ccdUrl), eq(GET), captor.capture(), eq(Object.class));
        MultiValueMap<String, String> headers = captor.getValue().getHeaders();

        assertEquals(responseXml, responseEntity.getBody());
        assertEquals("dfwfwef",headers.getFirst("Authorization"));
        assertEquals("sdsvsdfvs",headers.getFirst("ServiceAuthorization"));
        assertEquals("application/json", headers.getFirst("Content-Type"));
    }

    @Test(expected = CcdException.class)
    public void shouldHandleExceptionForSendRequest() throws Exception {
        String ccdUrl = "https://localhost:4451/caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";
        given(restTemplate.exchange(eq(ccdUrl), eq(GET), any(HttpEntity.class), eq(Object.class)))
                .willThrow(new RuntimeException("Error"));

        String path = "caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";
        Map<String,Object> body = new HashMap<>();
        body.put("data","{\"id\":\"123\"}");

        coreCaseDataClient.sendRequest("dfwfwef","sdsvsdfvs",path,GET,body);
    }

    @Test
    public void shouldSendPostRequest() throws Exception {
        String ccdUrl = "https://localhost:4451/caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";
        String responseXml = "{\"id\": 113,\"jurisdiction\": \"SSCS\","
                + "\"state\": \"ResponseRequested\",\"case_type_id\": \"Benefit\"";
        given(restTemplate.postForEntity(eq(ccdUrl), any(HttpEntity.class), eq(Object.class)))
                .willReturn(new ResponseEntity<>(responseXml, OK));

        String path = "caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";
        Map<String,Object> body = new HashMap<>();
        body.put("data","{\"id\":\"123\"}");

        ResponseEntity<Object> responseEntity = coreCaseDataClient.post("dfwfwef","sdsvsdfvs",path,body);

        verify(restTemplate).postForEntity(eq(ccdUrl), captor.capture(), eq(Object.class));
        MultiValueMap<String, String> headers = captor.getValue().getHeaders();

        assertEquals(responseXml, responseEntity.getBody());
        assertEquals("dfwfwef",headers.getFirst("Authorization"));
        assertEquals("sdsvsdfvs",headers.getFirst("ServiceAuthorization"));
        assertEquals("application/json", headers.getFirst("Content-Type"));
    }

    @Test(expected = CcdException.class)
    public void shouldHandleExceptionForPostRequest() throws Exception {
        String ccdUrl = "https://localhost:4451/caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";
        given(restTemplate.postForEntity(eq(ccdUrl), any(HttpEntity.class), eq(Object.class)))
                .willThrow(new RuntimeException("Error"));

        String path = "caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";
        Map<String,Object> body = new HashMap<>();
        body.put("data","{\"id\":\"123\"}");

        coreCaseDataClient.post("dfwfwef","sdsvsdfvs", path, body);
    }

    @Test
    public void shouldSendGetRequest() throws Exception {
        String ccdUrl = "https://localhost:4451/caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";
        CcdCase ccdCase = new CcdCase();
        given(restTemplate.getForEntity(eq(ccdUrl), eq(CcdCase.class)))
                .willReturn(new ResponseEntity<>(ccdCase, OK));

        String path = "caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";
        Map<String,Object> body = new HashMap<>();
        body.put("data","{\"id\":\"123\"}");

        ResponseEntity<CcdCase> responseEntity = coreCaseDataClient.get(path);

        verify(restTemplate).getForEntity(eq(ccdUrl), eq(CcdCase.class));

        assertEquals(ccdCase, responseEntity.getBody());
    }

    @Test(expected = CcdException.class)
    public void shouldHandleExceptionForGetRequest() throws Exception {
        String ccdUrl = "https://localhost:4451/caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";
        given(restTemplate.getForEntity(eq(ccdUrl), eq(CcdCase.class)))
                .willThrow(new RuntimeException("Error"));

        String path = "caseworkers/123/jurisdictions/SSCS/case-types/BENEFIT/cases/113";
        Map<String,Object> body = new HashMap<>();
        body.put("data","{\"id\":\"123\"}");

        coreCaseDataClient.get(path);
    }
}
