package uk.gov.hmcts.sscs.service;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.tya.SubscriptionRequest;
import uk.gov.hmcts.sscs.service.ccd.CaseDataUtils;
import uk.gov.hmcts.sscs.service.ccd.CreateCoreCaseDataService;
import uk.gov.hmcts.sscs.service.ccd.ReadCoreCaseDataService;
import uk.gov.hmcts.sscs.service.ccd.UpdateCoreCaseDataService;

@RunWith(MockitoJUnitRunner.class)
public class CcdServiceTest {
    private static final String BENEFIT_TYPE = "jsa";
    private static  final String SURNAME = "Test";

    private CcdService ccdService;

    @Mock
    private ReadCoreCaseDataService readCoreCaseDataService;

    @Mock
    private CreateCoreCaseDataService createCoreCaseDataService;

    @Mock
    private UpdateCoreCaseDataService updateCoreCaseDataService;

    private CaseData testCaseData;

    private Long caseId = 1L;

    private String eventId = "event-id";


    @Before
    public void setup() {
        ccdService = new CcdService(readCoreCaseDataService, createCoreCaseDataService, updateCoreCaseDataService);
        testCaseData = CaseDataUtils.buildCaseData();
        when(readCoreCaseDataService.getCcdCaseDataByAppealNumber(anyString()))
                .thenReturn(CaseDataUtils.buildCaseData());

        when(createCoreCaseDataService.createCcdCase(CaseDataUtils.buildCaseData()))
                .thenReturn(CaseDataUtils.buildCaseDetails());

        when(updateCoreCaseDataService.updateCase(testCaseData, 1L, eventId))
                .thenReturn(CaseDataUtils.buildCaseDetails("SC666/66/66666"));
    }

    @Test
    public void shouldSendCaseToCcd() {

        CaseDetails caseDetails = ccdService.createCase(CaseDataUtils.buildCaseData());

        assertNotNull(caseDetails);
        verify(createCoreCaseDataService).createCcdCase(CaseDataUtils.buildCaseData());
    }

    @Test
    public void shouldGetCaseFromCcd() {

        CaseData caseData = ccdService.findCcdCaseByAppealNumber(anyString());

        verify(readCoreCaseDataService).getCcdCaseDataByAppealNumber(anyString());

        assertNotNull(caseData);
    }

    @Test
    public void shouldUpdateSubscriptionInCcd() {
        when(readCoreCaseDataService.getCcdCaseDetailsByAppealNumber(anyString()))
                .thenReturn(CaseDataUtils.buildCaseDetails());


        String benefitType = ccdService.updateSubscription(anyString(), getSubscriptionRequest());

        verify(readCoreCaseDataService).getCcdCaseDetailsByAppealNumber(anyString());
        verify(updateCoreCaseDataService).updateCase(any(), eq(null), anyString());

        assertEquals(BENEFIT_TYPE, benefitType);
    }

    @Test
    public void shouldReturnCaseGivenSurnameAndAppealNumber() {

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), SURNAME);

        verify(readCoreCaseDataService).getCcdCaseDataByAppealNumber(anyString());

        assertNotNull(caseData);
        assertEquals(SURNAME, caseData.getAppeal().getAppellant().getName().getLastName());
    }

    @Test
    public void shouldReturnCaseGivenWhenSurnameHasAccent() {
        when(readCoreCaseDataService.getCcdCaseDataByAppealNumber(anyString()))
                .thenReturn(CaseDataUtils.buildCaseData());

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), "Tést");

        verify(readCoreCaseDataService).getCcdCaseDataByAppealNumber(anyString());

        assertNotNull(caseData);
    }

    @Test
    public void shouldReturnCaseGivenWhenSurnameInCcdHasAccent() {
        when(readCoreCaseDataService.getCcdCaseDataByAppealNumber(anyString()))
                .thenReturn(CaseDataUtils.buildCaseData("Tést"));

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), "Test");

        verify(readCoreCaseDataService).getCcdCaseDataByAppealNumber(anyString());

        assertNotNull(caseData);
    }

    @Test
    public void shouldReturnCaseGivenSurnameContainsNotAlphaCharacter() {
        when(readCoreCaseDataService.getCcdCaseDataByAppealNumber(anyString()))
                .thenReturn(CaseDataUtils.buildCaseData());

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), "Te-st");

        verify(readCoreCaseDataService).getCcdCaseDataByAppealNumber(anyString());

        assertNotNull(caseData);
    }

    @Test
    public void shouldReturnCaseGivenSurnameInCcdContainsNotAlphaCharacter() {
        when(readCoreCaseDataService.getCcdCaseDataByAppealNumber(anyString()))
                .thenReturn(CaseDataUtils.buildCaseData("Te-st"));

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), "Test");

        verify(readCoreCaseDataService).getCcdCaseDataByAppealNumber(anyString());

        assertNotNull(caseData);
    }

    @Test
    public void shouldReturnNullIfSurnameInvalid() {

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), "XXX");

        verify(readCoreCaseDataService).getCcdCaseDataByAppealNumber(anyString());

        assertNull(caseData);
    }

    @Test
    public void shouldUpdateCaseInCcd() {

        CaseDetails caseDetails = ccdService.updateCase(testCaseData, caseId, eventId);

        assertNotNull(caseDetails);
        verify(updateCoreCaseDataService).updateCase(testCaseData, caseId, eventId);
    }

    @Test
    public void shouldUnSubscribeEmailNotification() {
        when(readCoreCaseDataService.getCcdCaseDetailsByAppealNumber(anyString()))
                .thenReturn(CaseDataUtils.buildCaseDetails());


        String benefitType = ccdService.unsubscribe(anyString());

        verify(readCoreCaseDataService).getCcdCaseDetailsByAppealNumber(anyString());
        verify(updateCoreCaseDataService).updateCase(any(CaseData.class), eq(null), anyString());

        assertEquals(BENEFIT_TYPE, benefitType);
    }

    private SubscriptionRequest getSubscriptionRequest() {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setEmail("email@email.com");
        return subscriptionRequest;
    }

}
