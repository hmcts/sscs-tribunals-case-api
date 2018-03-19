package uk.gov.hmcts.sscs.service;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.domain.reminder.ReminderResponse;
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
    public void shouldSendCaseToCcd() throws Exception {

        CaseDetails caseDetails = ccdService.createCase(CaseDataUtils.buildCaseData());

        assertNotNull(caseDetails);
        verify(createCoreCaseDataService).createCcdCase(CaseDataUtils.buildCaseData());
    }

    @Test
    @Ignore
    public void shouldStartEventInCcd() throws Exception {

        ReminderResponse reminder = new ReminderResponse("123", "hearingReminderNotification");

        ccdService.createEvent(reminder);
    }

    @Test
    public void shouldGetCaseFromCcd() throws Exception {

        CaseData caseData = ccdService.findCcdCaseByAppealNumber(anyString());

        verify(readCoreCaseDataService).getCcdCaseDataByAppealNumber(anyString());

        assertNotNull(caseData);
    }

    @Test
    public void shouldUnsubscribeFromCcd() throws Exception {

        String benefitType = ccdService.unsubscribe(anyString(), "reason");

        verify(readCoreCaseDataService).getCcdCaseDataByAppealNumber(anyString());

        assertEquals(BENEFIT_TYPE, benefitType);
    }

    @Test
    public void shouldUpdateSubscriptionInCcd() throws Exception {
        when(readCoreCaseDataService.getCcdCaseDetailsByAppealNumber(anyString()))
                .thenReturn(CaseDataUtils.buildCaseDetails());


        String benefitType = ccdService.updateSubscription(anyString(), getSubscriptionRequest());

        verify(readCoreCaseDataService).getCcdCaseDetailsByAppealNumber(anyString());
        verify(updateCoreCaseDataService).updateCcdCase(any(CaseData.class), anyLong(), anyString());

        assertEquals(BENEFIT_TYPE, benefitType);
    }

    @Test
    public void shouldReturnCaseGivenSurnameAndAppealNumber() throws Exception {

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), SURNAME);

        verify(readCoreCaseDataService).getCcdCaseDataByAppealNumber(anyString());

        assertNotNull(caseData);
        assertEquals(SURNAME, caseData.getAppeal().getAppellant().getName().getLastName());
    }

    @Test
    public void shouldReturnNullIfSurnameInvalid() throws Exception {

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), "XXX");

        verify(readCoreCaseDataService).getCcdCaseDataByAppealNumber(anyString());

        assertNull(caseData);
    }

    @Test
    public void shouldUpdateCaseInCcd() throws Exception {

        CaseDetails caseDetails = ccdService.updateCase(any(CaseData.class),
                anyLong(),anyString());

        assertNotNull(caseDetails);
        verify(updateCoreCaseDataService).updateCcdCase(any(CaseData.class),
                anyLong(),anyString());
    }

    private SubscriptionRequest getSubscriptionRequest() {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setEmail("email@email.com");
        subscriptionRequest.setMobileNumber("0777777777");
        return subscriptionRequest;
    }

}
