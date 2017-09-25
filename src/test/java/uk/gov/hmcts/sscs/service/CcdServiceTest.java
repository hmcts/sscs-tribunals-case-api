package uk.gov.hmcts.sscs.service;

import static org.assertj.core.util.Maps.newHashMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class CcdServiceTest {
    public static final String CASE_WORKER_ID = "123";
    public static final String TOKEN = "token123";
    private CcdService ccdService;

    @Mock
    private AuthClient authClient;

    @Mock
    private IdamClient idamClient;

    @Mock
    private CoreCaseDataClient coreCaseDataClient;

    @Captor
    private ArgumentCaptor<Map> captor;

    private String userToken = "user-caseToken";

    private String serviceToken = "service-caseToken";

    @Before
    public void setUp() {
        ccdService = new CcdService(coreCaseDataClient, authClient,
                idamClient, CASE_WORKER_ID);
    }

    @Test
    public void shouldSendAppealCaseToCcd() throws Exception {
        given(authClient.sendRequest(eq("lease"), eq(HttpMethod.POST),
                eq(""))).willReturn(serviceToken);
        Map<String,Object> appeal = new HashMap<>();
        appeal.put("id","123");
        appeal.put("jurisdiction","SSCS");
        appeal.put("state","ResponseRequested");
        String tokenPath = "caseworkers/123/jurisdictions/SSCS/case-types/"
                +
                "Benefit/event-triggers/appealReceived/token";
        String ccdPath = "caseworkers/123/jurisdictions/SSCS/case-types/Benefit/cases";

        String appealsJson = "{\"id\":113,"
                +
                "\"jurisdiction\":\"SSCS\","
                +
                "\"state\":\"ResponseRequested\"}";
        given(coreCaseDataClient.sendRequest(eq("Bearer " + userToken),eq(serviceToken),
                eq(tokenPath), eq(HttpMethod.GET), any(Map.class)))
                .willReturn(new ResponseEntity(newHashMap("token",TOKEN), OK));
        given(coreCaseDataClient.post(eq("Bearer " + userToken),eq(serviceToken),
                eq(ccdPath),any(Map.class)))
                .willReturn(new ResponseEntity<>(CREATED));
        given(idamClient.post("testing-support/lease"))
                .willReturn(userToken);

        HttpStatus status = ccdService.saveCase(appealsJson);
        assertEquals(CREATED, status);
        verify(coreCaseDataClient).sendRequest(eq("Bearer " + userToken),eq(serviceToken),
                eq(tokenPath), eq(HttpMethod.GET), any(Map.class));
        verify(coreCaseDataClient).post(eq("Bearer " + userToken),eq(serviceToken),
                eq(ccdPath),captor.capture());
        Map<String,Object> appealMap = new ObjectMapper().readValue(appealsJson, Map.class);

        assertEquals(appealMap, captor.getValue().get("data"));
    }
}
