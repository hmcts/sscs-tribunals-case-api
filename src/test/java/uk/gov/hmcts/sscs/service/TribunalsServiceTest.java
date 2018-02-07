package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.hmcts.sscs.domain.corecase.Appeal;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.domain.corecase.Subscription;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDeserializer;

@RunWith(MockitoJUnitRunner.class)
public class TribunalsServiceTest {

    public static final String APPEAL_NUMBER = "asfefsdf3223";
    private TribunalsService tribunalsService;

    @Mock
    private CcdService ccdService;

    @Mock
    private EmailService emailService;

    private SubmitYourAppealEmail email = new SubmitYourAppealEmail("from@hmcts.net",
            "to@hmcts.net", "Your appeal", "Your appeal has been created");

    @Mock
    private SubmitYourAppealToCcdCaseDeserializer transformer;

    @Mock
    private AppealNumberGenerator appealNumberGenerator;

    @Captor
    private ArgumentCaptor<CcdCase> captor;

    @Before
    public void setUp() throws Exception {
        tribunalsService = new TribunalsService(ccdService, emailService, email, transformer, appealNumberGenerator);
    }

    @Test
    public void shouldSendSubmitYourAppealEmail() throws CcdException {
        SyaCaseWrapper syaCaseWrapper = new SyaCaseWrapper();

        CcdCase ccdCase = new CcdCase();
        Appeal appeal = new Appeal();
        ccdCase.setAppeal(appeal);

        given(transformer.convertSyaToCcdCase(syaCaseWrapper)).willReturn(ccdCase);
        given(appealNumberGenerator.generate()).willReturn(APPEAL_NUMBER);

        tribunalsService.submitAppeal(syaCaseWrapper);

        verify(emailService).sendEmail(any(SubmitYourAppealEmail.class));
        verify(ccdService).createCase(captor.capture());

        CcdCase savedCase = captor.getValue();

        assertEquals(APPEAL_NUMBER, savedCase.getAppeal().getAppealNumber());
    }

    @Test
    public void shouldUnsubscribe() throws CcdException {

        tribunalsService.unsubscribe(APPEAL_NUMBER, "reason");

        verify(ccdService).unsubscribe(eq(APPEAL_NUMBER), eq("reason"));
    }

    @Test
    public void shouldUpdateSubscriptionDetails() throws CcdException {
        Subscription subscription = new Subscription();
        tribunalsService.updateSubscription(APPEAL_NUMBER, subscription);

        verify(ccdService).updateSubscription(eq(APPEAL_NUMBER), eq(subscription));
    }
}
