package uk.gov.hmcts.reform.sscs.ccd.presubmit.deathofappellant;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;

@RunWith(JUnitParamsRunner.class)
public class DeathOfAppellantAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private DeathOfAppellantAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new DeathOfAppellantAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.DEATH_OF_APPELLANT);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build()).dwpUcb("yes").build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonDeathOfAppellantEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenValidCallback_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenADeathOfAppellantEvent_thenSetInterlocReviewStateAndRemoveUcb() {

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), response.getData().getInterlocReviewState());
        assertNull(response.getData().getDwpUcb());
    }

    @Test
    public void givenADeathOfAppellantEventThatIsSubscribedToEmailAndSms_thenUnsubscribeFromEmailAndSms() {
        callback.getCaseDetails().getCaseData().setSubscriptions(Subscriptions.builder().appellantSubscription(Subscription.builder().subscribeEmail("Yes").subscribeSms("Yes").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), response.getData().getInterlocReviewState());
        assertEquals("No", response.getData().getSubscriptions().getAppellantSubscription().getSubscribeEmail());
        assertEquals("No", response.getData().getSubscriptions().getAppellantSubscription().getSubscribeSms());
    }

    @Test
    public void givenADeathOfAppellantEventThatHasNoSubscription_thenHandleUnsubscription() {
        callback.getCaseDetails().getCaseData().setSubscriptions(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), response.getData().getInterlocReviewState());
        assertNull(response.getData().getSubscriptions().getAppellantSubscription());
    }

    @Test
    public void givenADeathOfAppellantEventThatHasNoAppellantSubscription_thenHandleUnsubscription() {
        callback.getCaseDetails().getCaseData().setSubscriptions(Subscriptions.builder().appellantSubscription(null).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), response.getData().getInterlocReviewState());
        assertNull(response.getData().getSubscriptions().getAppellantSubscription());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}
