package uk.gov.hmcts.sscs.service;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
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
import uk.gov.hmcts.sscs.model.ccd.Subscription;
import uk.gov.hmcts.sscs.service.ccd.CaseDataUtils;
import uk.gov.hmcts.sscs.service.ccd.CreateCoreCaseDataService;
import uk.gov.hmcts.sscs.service.ccd.ReadCoreCaseDataService;

@RunWith(MockitoJUnitRunner.class)
public class CcdServiceTest {
    private static final String BENEFIT_TYPE = "1325";
    private static  final String SURNAME = "Test";

    private CcdService ccdService;

    @Mock
    private ReadCoreCaseDataService readCoreCaseDataService;

    @Mock
    private CreateCoreCaseDataService createCoreCaseDataService;


    @Before
    public void setup() {
        ccdService = new CcdService(readCoreCaseDataService, createCoreCaseDataService);

        when(readCoreCaseDataService.getCcdCaseData(anyString()))
                .thenReturn(CaseDataUtils.buildCaseData());

        when(createCoreCaseDataService.createCcdCase(CaseDataUtils.buildCaseData()))
                .thenReturn(CaseDataUtils.buildCaseDetails());
    }

    @Test
    public void shouldSendCaseToCcd() throws Exception {

        CaseData caseData = CaseDataUtils.buildCaseData();
        CaseDetails caseDetails = ccdService.createCase(caseData);

        assertNotNull(caseDetails);
        verify(createCoreCaseDataService).createCcdCase(caseData);
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

        verify(readCoreCaseDataService).getCcdCaseData(anyString());

        assertNotNull(caseData);
    }

    @Test
    public void shouldUnsubscribeFromCcd() throws Exception {

        String benefitType = ccdService.unsubscribe(anyString(), "reason");

        verify(readCoreCaseDataService).getCcdCaseData(anyString());

        assertEquals(BENEFIT_TYPE, benefitType);
    }

    @Test
    public void shouldUpdateSubscriptionInCcd() throws Exception {

        String benefitType = ccdService.updateSubscription(anyString(), Subscription.builder().build());

        verify(readCoreCaseDataService).getCcdCaseData(anyString());

        assertEquals(BENEFIT_TYPE, benefitType);
    }

    @Test
    public void shouldReturnCaseGivenSurnameAndAppealNumber() throws Exception {

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), SURNAME);

        verify(readCoreCaseDataService).getCcdCaseData(anyString());

        assertNotNull(caseData);
        assertEquals(SURNAME, caseData.getAppeal().getAppellant().getName().getLastName());
    }

    @Test
    public void shouldReturnNullIfSurnameInvalid() throws Exception {

        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(anyString(), "XXX");

        verify(readCoreCaseDataService).getCcdCaseData(anyString());

        assertNull(caseData);
    }
}
