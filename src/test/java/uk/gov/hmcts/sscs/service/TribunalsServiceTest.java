package uk.gov.hmcts.sscs.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.transform.SubmitYourAppealToCcdCaseTransformer;

@RunWith(MockitoJUnitRunner.class)
public class TribunalsServiceTest {

    private TribunalsService tribunalsService;

    @Mock
    private CcdService ccdService;

    @Mock
    private EmailService emailService;

    private SubmitYourAppealEmail email = new SubmitYourAppealEmail("from@hmcts.net",
            "to@hmcts.net", "Your appeal", "Your appeal has been created");

    @Mock
    private SubmitYourAppealToCcdCaseTransformer transformer;

    @Before
    public void setUp() throws Exception {
        tribunalsService = new TribunalsService(ccdService, emailService, email, transformer);
    }

    @Test
    public void shouldSendSubmitYourAppealEmail() throws CcdException {
        SyaCaseWrapper syaCaseWrapper = new SyaCaseWrapper();

        CcdCase ccdCase = new CcdCase();

        given(transformer.convertSyaToCcdCase(syaCaseWrapper)).willReturn(ccdCase);

        tribunalsService.submitAppeal(syaCaseWrapper);

        verify(emailService).sendEmail(any(SubmitYourAppealEmail.class));
    }
}
