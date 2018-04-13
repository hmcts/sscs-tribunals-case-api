package uk.gov.hmcts.sscs.service;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.*;
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
    private static final String BENEFIT_TYPE = "1325";
    private static  final String SURNAME = "Test";

    private CcdService ccdService;

    @Mock
    private ReadCoreCaseDataService readCoreCaseDataService;

    @Mock
    private CreateCoreCaseDataService createCoreCaseDataService;

    @Mock
    private UpdateCoreCaseDataService updateCoreCaseDataService;


    @Before
    public void setup() {
        ccdService = new CcdService(readCoreCaseDataService, createCoreCaseDataService, updateCoreCaseDataService);

        when(readCoreCaseDataService.getCcdCaseDataByAppealNumber(anyString()))
                .thenReturn(CaseDataUtils.buildCaseData());

        when(createCoreCaseDataService.createCcdCase(CaseDataUtils.buildCaseData()))
                .thenReturn(CaseDataUtils.buildCaseDetails());

        when(updateCoreCaseDataService.updateCcdCase(any(CaseData.class), anyLong(), anyString()))
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
        verify(updateCoreCaseDataService).updateCcdCase(any(CaseData.class), anyLong(), anyString());

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
    public void shouldReturnNullIfSurnameInvalid() {

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), "XXX");

        verify(readCoreCaseDataService).getCcdCaseDataByAppealNumber(anyString());

        assertNull(caseData);
    }

    @Test
    public void shouldUpdateCaseInCcd() {

        CaseDetails caseDetails = ccdService.updateCase(any(CaseData.class),
                anyLong(),anyString());

        assertNotNull(caseDetails);
        verify(updateCoreCaseDataService).updateCcdCase(any(CaseData.class),
                anyLong(),anyString());
    }

    @Test
    public void shouldUnSubscribeEmailNotification() {
        when(readCoreCaseDataService.getCcdCaseDetailsByAppealNumber(anyString()))
                .thenReturn(CaseDataUtils.buildCaseDetails());


        String benefitType = ccdService.unsubscribe(anyString());

        verify(readCoreCaseDataService).getCcdCaseDetailsByAppealNumber(anyString());
        verify(updateCoreCaseDataService).updateCcdCase(any(CaseData.class), anyLong(), anyString());

        assertEquals(BENEFIT_TYPE, benefitType);
    }

    private SubscriptionRequest getSubscriptionRequest() {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setEmail("email@email.com");
        return subscriptionRequest;
    }

}
