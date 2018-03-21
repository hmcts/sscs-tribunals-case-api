package uk.gov.hmcts.sscs.service;

import static junit.framework.TestCase.assertTrue;
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

import uk.gov.hmcts.sscs.builder.TrackYourAppealJsonBuilder;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.ccd.Appeal;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.ccd.Subscription;
import uk.gov.hmcts.sscs.model.ccd.Subscriptions;
import uk.gov.hmcts.sscs.model.tya.SubscriptionRequest;
import uk.gov.hmcts.sscs.service.exceptions.InvalidSurnameException;
import uk.gov.hmcts.sscs.service.referencedata.RegionalProcessingCenterService;
import uk.gov.hmcts.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;


@RunWith(MockitoJUnitRunner.class)
public class TribunalsServiceTest {

    public static final String APPEAL_NUMBER = "asfefsdf3223";
    private static final String SURNAME = "surname";
    public static final String REF_NUM = "ref-num";
    private TribunalsService tribunalsService;

    @Mock
    private CcdService ccdService;

    @Mock
    private EmailService emailService;

    private SubmitYourAppealEmail email = new SubmitYourAppealEmail("from@hmcts.net",
            "to@hmcts.net", "Your appeal", "Your appeal has been created");

    @Mock
    private SubmitYourAppealToCcdCaseDataDeserializer transformer;

    @Mock
    private AppealNumberGenerator appealNumberGenerator;

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
        tribunalsService = new TribunalsService(ccdService,
                emailService, email, transformer, appealNumberGenerator, regionalProcessingCenterService,
                trackYourAppealJsonBuilder);
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
    public void shouldSendSubmitYourAppealEmail() throws CcdException {
        SyaCaseWrapper syaCaseWrapper = new SyaCaseWrapper();

        Appeal appeal = Appeal.builder().build();
        Subscription appellantSubscription = Subscription.builder()
                .tya(APPEAL_NUMBER)
                .build();
        Subscriptions subscriptions = Subscriptions.builder()
                .appellantSubscription(appellantSubscription)
                .build();
        CaseData caseData = CaseData.builder()
                .appeal(appeal)
                .subscriptions(subscriptions)
                .build();

        given(transformer.convertSyaToCcdCaseData(syaCaseWrapper)).willReturn(caseData);
        given(appealNumberGenerator.generate()).willReturn(APPEAL_NUMBER);

        tribunalsService.submitAppeal(syaCaseWrapper);

        verify(emailService).sendEmail(any(SubmitYourAppealEmail.class));
        verify(ccdService).createCase(captor.capture());

        CaseData savedCase = captor.getValue();

        assertEquals(APPEAL_NUMBER, savedCase.getSubscriptions().getAppellantSubscription().getTya());
    }

    @Test
    public void shouldUnsubscribe() throws CcdException {

        tribunalsService.unsubscribe(APPEAL_NUMBER, "reason");

        verify(ccdService).unsubscribe(eq(APPEAL_NUMBER), eq("reason"));
    }

    @Test
    public void shouldUpdateSubscriptionDetails() throws CcdException {
        tribunalsService.updateSubscription(APPEAL_NUMBER, subscriptionRequest);

        verify(ccdService).updateSubscription(eq(APPEAL_NUMBER), eq(subscriptionRequest));
    }

    private CaseData getCaseData() {
        return CaseData.builder().build();
    }

    private Subscription getSubscription() {
        return Subscription.builder().build();
    }

}
