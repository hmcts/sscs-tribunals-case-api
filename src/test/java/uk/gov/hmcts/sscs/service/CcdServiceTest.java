package uk.gov.hmcts.sscs.service;

import static org.assertj.core.util.Maps.newHashMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
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
import uk.gov.hmcts.sscs.domain.corecase.*;
import uk.gov.hmcts.sscs.domain.reminder.ReminderResponse;

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

    private String tokenPath;

    private String ccdPath;

    public void setup() throws Exception {
        ccdService = new CcdService(coreCaseDataClient, authClient, idamClient, CASE_WORKER_ID);

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

        CcdCase ccdCase = new CcdCase();
        Appeal appeal = new Appeal();
        appeal.setAppealNumber("567");
        Appellant appellant = new Appellant();
        appellant.setName(new Name("Mr", "Harry", "Kane"));
        ccdCase.setAppeal(appeal);
        ccdCase.setAppellant(appellant);

        CcdCaseResponse ccdCaseResponse = new CcdCaseResponse();
        ccdCaseResponse.setCaseData(ccdCase);

        ccdPath = "caseworkers/123/jurisdictions/SSCS/case-types/Benefit/cases/567";
        given(coreCaseDataClient.get(eq("Bearer " + userToken),eq(serviceToken),
                eq(ccdPath)))
                .willReturn(new ResponseEntity<>(ccdCaseResponse, OK));

        CcdCase ccdCaseRes = ccdService.findCcdCaseByAppealNumber("567");

        verify(coreCaseDataClient).get(eq("Bearer " + userToken),eq(serviceToken),eq(ccdPath));

        assertEquals(ccdCaseRes, ccdCase);
    }

    @Test
    public void shouldUnsubscribeFromCcd() throws Exception {
        tokenPath = "caseworkers/123/jurisdictions/SSCS/case-types/"
                + "Benefit/event-triggers/appealReceived/token";

        ccdPath = "caseworkers/123/jurisdictions/SSCS/case-types/Benefit/cases";

        setup();
        CcdCase ccdCase = new CcdCase();
        ccdCase.setBenefitType("Benefit");

        CcdCaseResponse ccdCaseResponse = new CcdCaseResponse();
        ccdCaseResponse.setCaseData(ccdCase);

        String casePath = "caseworkers/123/jurisdictions/SSCS/case-types/Benefit/cases/567";

        given(coreCaseDataClient.get(eq("Bearer " + userToken), eq(serviceToken), eq(casePath)))
                .willReturn(new ResponseEntity<>(ccdCaseResponse, OK));

        String benefitType = ccdService.unsubscribe("567", "reason");

        verify(coreCaseDataClient).get(eq("Bearer " + userToken),eq(serviceToken),eq(casePath));

        assertEquals(ccdCase.getBenefitType().toLowerCase(), benefitType);
    }

    @Test
    public void shouldUpdateSubscriptionInCcd() throws Exception {
        tokenPath = "caseworkers/123/jurisdictions/SSCS/case-types/"
                + "Benefit/event-triggers/appealReceived/token";

        ccdPath = "caseworkers/123/jurisdictions/SSCS/case-types/Benefit/cases";

        setup();
        CcdCase ccdCase = new CcdCase();
        ccdCase.setBenefitType("Benefit");

        CcdCaseResponse ccdCaseResponse = new CcdCaseResponse();
        ccdCaseResponse.setCaseData(ccdCase);

        String casePath = "caseworkers/123/jurisdictions/SSCS/case-types/Benefit/cases/567";

        Subscription subscription = new Subscription();


        given(coreCaseDataClient.get(eq("Bearer " + userToken), eq(serviceToken), eq(casePath)))
                .willReturn(new ResponseEntity<>(ccdCaseResponse, OK));

        String benefitType = ccdService.updateSubscription("567", subscription);

        verify(coreCaseDataClient).get(eq("Bearer " + userToken),eq(serviceToken),eq(casePath));

        assertEquals(ccdCase.getBenefitType().toLowerCase(), benefitType);
    }

    @Test
    public void shouldReturnCaseGivenSurnameAndAppealNumber() {

    }
}
