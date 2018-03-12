package uk.gov.hmcts.sscs.service;

import static junit.framework.TestCase.assertNotNull;
import static org.assertj.core.util.Maps.newHashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.hmcts.sscs.domain.corecase.Appellant;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.domain.corecase.Name;
import uk.gov.hmcts.sscs.domain.corecase.Subscription;
import uk.gov.hmcts.sscs.domain.reminder.ReminderResponse;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.service.ccd.CaseDataUtils;
import uk.gov.hmcts.sscs.service.ccd.ReadCoreCaseDataService;

@RunWith(MockitoJUnitRunner.class)
public class CcdServiceTest {
    private static final String CASE_WORKER_ID = "123";
    private static final String TOKEN = "token123";
    private static final String BENEFIT_TYPE = "1325";
    private static  final String SURNAME = "Test";

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

    private String tokenPath;

    private String ccdPath;

    @Mock
    private ReadCoreCaseDataService readCoreCaseDataService;


    public void setup() throws Exception {
        ccdService = new CcdService(coreCaseDataClient, authClient, idamClient, CASE_WORKER_ID,
                readCoreCaseDataService);

        given(authClient.sendRequest(eq("lease"), eq(HttpMethod.POST),
                eq(""))).willReturn(serviceToken);

        given(coreCaseDataClient.sendRequest(eq("Bearer " + userToken),eq(serviceToken),
                eq(tokenPath), eq(HttpMethod.GET), any(Map.class)))
                .willReturn(new ResponseEntity(newHashMap("token",TOKEN), OK));
        given(coreCaseDataClient.post(eq("Bearer " + userToken),eq(serviceToken),
                eq(ccdPath),any(Map.class)))
                .willReturn(new ResponseEntity<>(CREATED));
        given(idamClient.post("testing-support/lease"))
                .willReturn(userToken);
    }

    @Test
    public void shouldSendCaseToCcd() throws Exception {
        tokenPath = "caseworkers/123/jurisdictions/SSCS/case-types/"
                + "Benefit/event-triggers/appealReceived/token";

        ccdPath = "caseworkers/123/jurisdictions/SSCS/case-types/Benefit/cases";

        setup();

        Map<String,Object> appeal = new HashMap<>();
        appeal.put("id","123");
        appeal.put("jurisdiction","SSCS");
        appeal.put("state","ResponseRequested");

        CcdCase ccdCase = new CcdCase();
        Appellant appellant = new Appellant();
        appellant.setName(new Name("Mr", "Harry", "Kane"));
        ccdCase.setAppellant(appellant);

        HttpStatus status = ccdService.createCase(ccdCase);
        assertEquals(CREATED, status);
        verify(coreCaseDataClient).sendRequest(eq("Bearer " + userToken),eq(serviceToken),
                eq(tokenPath), eq(HttpMethod.GET), any(Map.class));
        verify(coreCaseDataClient).post(eq("Bearer " + userToken),eq(serviceToken),
                eq(ccdPath), captor.capture());

        assertEquals(ccdCase, captor.getValue().get("data"));
    }

    @Test
    public void shouldStartEventInCcd() throws Exception {
        tokenPath = "caseworkers/123/jurisdictions/SSCS/case-types/"
                + "Benefit/cases/123/event-triggers/hearingReminderNotification/token";

        ccdPath = "caseworkers/123/jurisdictions/SSCS/case-types/Benefit/cases/123/events";

        setup();

        CcdCase ccdCase = new CcdCase();
        Appellant appellant = new Appellant();
        appellant.setName(new Name("Mr", "Harry", "Kane"));
        ccdCase.setAppellant(appellant);

        ReminderResponse reminder = new ReminderResponse("123", "hearingReminderNotification");

        HttpStatus status = ccdService.createEvent(ccdCase, reminder);
        assertEquals(CREATED, status);
        verify(coreCaseDataClient).sendRequest(eq("Bearer " + userToken),eq(serviceToken),
                eq(tokenPath), eq(HttpMethod.GET), any(Map.class));
        verify(coreCaseDataClient).post(eq("Bearer " + userToken), eq(serviceToken),
                eq(ccdPath), captor.capture());

        assertEquals(ccdCase, captor.getValue().get("data"));
    }

    @Test
    public void shouldGetCaseFromCcd() throws Exception {
        setup();
        mockCaseData();

        CaseData caseData = ccdService.findCcdCaseByAppealNumber(anyString());

        verify(readCoreCaseDataService).getCcdCaseData(anyString());

        assertNotNull(caseData);
    }

    @Test
    public void shouldUnsubscribeFromCcd() throws Exception {
        tokenPath = "caseworkers/123/jurisdictions/SSCS/case-types/"
                + "Benefit/event-triggers/appealReceived/token";
        ccdPath = "caseworkers/123/jurisdictions/SSCS/case-types/Benefit/cases";

        setup();
        mockCaseData();

        String benefitType = ccdService.unsubscribe(anyString(), "reason");

        verify(readCoreCaseDataService).getCcdCaseData(anyString());

        assertEquals(BENEFIT_TYPE, benefitType);
    }

    @Test
    public void shouldUpdateSubscriptionInCcd() throws Exception {
        tokenPath = "caseworkers/123/jurisdictions/SSCS/case-types/"
                + "Benefit/event-triggers/appealReceived/token";
        ccdPath = "caseworkers/123/jurisdictions/SSCS/case-types/Benefit/cases";

        setup();
        mockCaseData();

        String benefitType = ccdService.updateSubscription(anyString(), new Subscription());

        verify(readCoreCaseDataService).getCcdCaseData(anyString());

        assertEquals(BENEFIT_TYPE, benefitType);
    }

    @Test
    public void shouldReturnCaseGivenSurnameAndAppealNumber() throws Exception {
        setup();
        mockCaseData();

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), SURNAME);

        verify(readCoreCaseDataService).getCcdCaseData(anyString());

        assertNotNull(caseData);
        assertEquals(SURNAME, caseData.getAppeal().getAppellant().getName().getLastName());
    }

    @Test
    public void shouldReturnNullIfSurnameInvalid() throws Exception {
        setup();
        mockCaseData();

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), "XXX");

        verify(readCoreCaseDataService).getCcdCaseData(anyString());

        assertNull(caseData);
    }

    private void mockCaseData() {
        when(readCoreCaseDataService.getCcdCaseData(anyString())).thenReturn(CaseDataUtils.buildCaseData());

        //when(caseDetailsToCcdCaseMapper.map(caseDetails)).thenReturn(getCcdCase());
    }
}
