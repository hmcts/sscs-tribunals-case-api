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

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.domain.corecase.Appellant;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.domain.corecase.Name;
import uk.gov.hmcts.sscs.domain.corecase.Subscription;
import uk.gov.hmcts.sscs.domain.reminder.ReminderResponse;
import uk.gov.hmcts.sscs.service.ccd.CaseDataUtils;
import uk.gov.hmcts.sscs.service.ccd.ReadCoreCaseDataService;
import uk.gov.hmcts.sscs.service.ccd.mapper.CaseDetailsToCcdCaseMapper;
import uk.gov.hmcts.sscs.service.referencedata.RegionalProcessingCenterService;

@RunWith(MockitoJUnitRunner.class)
public class CcdServiceTest {
    private static final String CASE_WORKER_ID = "123";
    private static final String TOKEN = "token123";
    private static final String CASE_REFERENCE = "SC068/17/00013";
    private static final String BENEFIT = "benefit";
    private static  final String SURNAME = "Kane";

    private CcdService ccdService;

    @Mock
    private AuthClient authClient;

    @Mock
    private IdamClient idamClient;

    @Mock
    private CoreCaseDataClient coreCaseDataClient;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Captor
    private ArgumentCaptor<Map> captor;

    private String userToken = "user-caseToken";

    private String serviceToken = "service-caseToken";

    private String tokenPath;

    private String ccdPath;

    @Mock
    private ReadCoreCaseDataService readCoreCaseDataService;

    @Mock
    private CaseDetailsToCcdCaseMapper caseDetailsToCcdCaseMapper;

    public void setup() throws Exception {
        ccdService = new CcdService(coreCaseDataClient, authClient, idamClient, CASE_WORKER_ID,
                readCoreCaseDataService, caseDetailsToCcdCaseMapper, regionalProcessingCenterService);

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
        mockCaseDetails();

        CcdCase ccdCaseRes = ccdService.findCcdCaseByAppealNumber(anyString());

        verify(readCoreCaseDataService).getCcdCase(anyString());
        verify(regionalProcessingCenterService).getByScReferenceCode(anyString());

        assertEquals(getCcdCase(), ccdCaseRes);
    }

    @Test
    public void shouldUnsubscribeFromCcd() throws Exception {
        tokenPath = "caseworkers/123/jurisdictions/SSCS/case-types/"
                + "Benefit/event-triggers/appealReceived/token";
        ccdPath = "caseworkers/123/jurisdictions/SSCS/case-types/Benefit/cases";

        setup();
        mockCaseDetails();

        String benefitType = ccdService.unsubscribe(anyString(), "reason");

        verify(readCoreCaseDataService).getCcdCase(anyString());

        assertEquals(BENEFIT, benefitType);
    }

    @Test
    public void shouldUpdateSubscriptionInCcd() throws Exception {
        tokenPath = "caseworkers/123/jurisdictions/SSCS/case-types/"
                + "Benefit/event-triggers/appealReceived/token";
        ccdPath = "caseworkers/123/jurisdictions/SSCS/case-types/Benefit/cases";

        setup();
        mockCaseDetails();

        String benefitType = ccdService.updateSubscription(anyString(), new Subscription());

        verify(readCoreCaseDataService).getCcdCase(anyString());

        assertEquals(BENEFIT, benefitType);
    }

    @Test
    public void shouldReturnCaseGivenSurnameAndAppealNumber() throws Exception {
        setup();
        mockCaseDetails();

        CcdCase ccdCase = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), SURNAME);

        verify(readCoreCaseDataService).getCcdCase(anyString());

        assertNotNull(ccdCase);
        assertEquals(SURNAME, ccdCase.getAppellant().getName().getSurname());
    }

    @Test
    public void shouldReturnNullIfSurnameInvalid() throws Exception {
        setup();
        mockCaseDetails();

        CcdCase ccdCaseRes = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), "XXX");

        verify(readCoreCaseDataService).getCcdCase(anyString());

        assertNull(ccdCaseRes);
    }

    private void mockCaseDetails() {
        CaseDetails caseDetails = CaseDataUtils.buildCaseDetails();
        when(readCoreCaseDataService.getCcdCase(anyString())).thenReturn(caseDetails);

        when(caseDetailsToCcdCaseMapper.map(caseDetails)).thenReturn(getCcdCase());
    }

    private CcdCase getCcdCase() {
        CcdCase ccdCase = new CcdCase();
        ccdCase.setCaseReference(CASE_REFERENCE);
        ccdCase.setBenefitType(BENEFIT);

        Appellant appellant = new Appellant();
        appellant.setName(new Name("Mr", "Harry", "Kane"));
        ccdCase.setAppellant(appellant);
        return ccdCase;
    }
}
