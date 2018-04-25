package uk.gov.hmcts.sscs.service;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.hmcts.sscs.builder.TrackYourAppealJsonBuilder;
import uk.gov.hmcts.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.tya.SubscriptionRequest;
import uk.gov.hmcts.sscs.service.exceptions.InvalidSurnameException;
import uk.gov.hmcts.sscs.service.referencedata.RegionalProcessingCenterService;


@RunWith(MockitoJUnitRunner.class)
public class TribunalsServiceTest {

    private static final String APPEAL_NUMBER = "asfefsdf3223";
    private static final String SURNAME = "surname";
    private TribunalsService tribunalsService;

    @Mock
    private CcdService ccdService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private TrackYourAppealJsonBuilder trackYourAppealJsonBuilder;

    @Mock
    private SubscriptionRequest subscriptionRequest;

    @Captor
    private ArgumentCaptor<CaseData> captor;

    @Before
    public void setUp() {
        tribunalsService = new TribunalsService(ccdService, regionalProcessingCenterService,
                trackYourAppealJsonBuilder);
    }

    @Test(expected = AppealNotFoundException.class)
    public void shouldThrowExceptionIfAppealNumberNotFound() throws CcdException {
        given(ccdService.findCcdCaseByAppealNumber(APPEAL_NUMBER)).willReturn(null);

        tribunalsService.findAppeal(APPEAL_NUMBER);
    }

    @Test
    public void shouldReturnTrueGivenValidAppealNumberAndSurname() throws CcdException {
        given(ccdService.findCcdCaseByAppealNumberAndSurname(APPEAL_NUMBER, SURNAME)).willReturn(getCaseData());

        assertTrue(tribunalsService.validateSurname(APPEAL_NUMBER, SURNAME));
    }

    @Test(expected = InvalidSurnameException.class)
    public void shouldThrowExceptionGivenValidationFails() throws CcdException {
        given(ccdService.findCcdCaseByAppealNumberAndSurname(APPEAL_NUMBER, SURNAME)).willReturn(null);

        tribunalsService.validateSurname(APPEAL_NUMBER, SURNAME);
    }

    @Test
    public void shouldUnsubscribe() throws CcdException {

        tribunalsService.unsubscribe(APPEAL_NUMBER);

        verify(ccdService).unsubscribe(eq(APPEAL_NUMBER));
    }

    @Test
    public void shouldUpdateSubscriptionDetails() throws CcdException {
        tribunalsService.updateSubscription(APPEAL_NUMBER, subscriptionRequest);

        verify(ccdService).updateSubscription(eq(APPEAL_NUMBER), eq(subscriptionRequest));
    }

    private CaseData getCaseData() {
        return CaseData.builder().build();
    }

}
