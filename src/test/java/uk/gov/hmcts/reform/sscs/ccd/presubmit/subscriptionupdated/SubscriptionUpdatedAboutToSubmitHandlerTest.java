package uk.gov.hmcts.reform.sscs.ccd.presubmit.subscriptionupdated;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

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

@RunWith(JUnitParamsRunner.class)
public class SubscriptionUpdatedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private SubscriptionUpdatedAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;


    @Before
    public void setUp() {
        initMocks(this);
        handler = new SubscriptionUpdatedAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.SUBSCRIPTION_UPDATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonSubscriptionUpdatedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAppellantSubscriptionWithNoExistingTya_thenAutomaticallyGenerateARandomNumberForTya() {
        Subscription appellantSubscription = Subscription.builder().tya(null).email("email").build();
        Subscriptions subscriptions = Subscriptions.builder().appellantSubscription(appellantSubscription).build();
        sscsCaseData.setSubscriptions(subscriptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getSubscriptions().getAppellantSubscription().getTya());
        assertEquals("email", response.getData().getSubscriptions().getAppellantSubscription().getEmail());
    }

    @Test
    public void givenAppointeeSubscriptionWithNoExistingTya_thenAutomaticallyGenerateARandomNumberForTya() {
        Subscription appointeeSubscription = Subscription.builder().tya(null).email("email").build();
        Subscriptions subscriptions = Subscriptions.builder().appointeeSubscription(appointeeSubscription).build();
        sscsCaseData.setSubscriptions(subscriptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getSubscriptions().getAppointeeSubscription().getTya());
        assertEquals("email", response.getData().getSubscriptions().getAppointeeSubscription().getEmail());
    }

    @Test
    public void givenRepSubscriptionWithNoExistingTya_thenAutomaticallyGenerateARandomNumberForTya() {
        Subscription repSubscription = Subscription.builder().tya(null).email("email").build();
        Subscriptions subscriptions = Subscriptions.builder().representativeSubscription(repSubscription).build();
        sscsCaseData.setSubscriptions(subscriptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getSubscriptions().getRepresentativeSubscription().getTya());
        assertEquals("email", response.getData().getSubscriptions().getRepresentativeSubscription().getEmail());
    }

    @Test
    public void givenAppellantSubscriptionWithExistingTya_thenUseExistingTyaNumber() {
        Subscription appellantSubscription = Subscription.builder().tya("123").email("email").build();
        Subscriptions subscriptions = Subscriptions.builder().appellantSubscription(appellantSubscription).build();
        sscsCaseData.setSubscriptions(subscriptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("123", response.getData().getSubscriptions().getAppellantSubscription().getTya());
        assertEquals("email", response.getData().getSubscriptions().getAppellantSubscription().getEmail());
    }

    @Test
    public void givenAppointeeSubscriptionWithExistingTya_thenUseExistingTyaNumber() {
        Subscription appointeeSubscription = Subscription.builder().tya("123").email("email").build();
        Subscriptions subscriptions = Subscriptions.builder().appointeeSubscription(appointeeSubscription).build();
        sscsCaseData.setSubscriptions(subscriptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("123", response.getData().getSubscriptions().getAppointeeSubscription().getTya());
        assertEquals("email", response.getData().getSubscriptions().getAppointeeSubscription().getEmail());
    }

    @Test
    public void givenRepSubscriptionWithExistingTya_thenUseExistingTyaNumber() {
        Subscription repSubscription = Subscription.builder().tya("123").email("email").build();
        Subscriptions subscriptions = Subscriptions.builder().representativeSubscription(repSubscription).build();
        sscsCaseData.setSubscriptions(subscriptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("123", response.getData().getSubscriptions().getRepresentativeSubscription().getTya());
        assertEquals("email", response.getData().getSubscriptions().getRepresentativeSubscription().getEmail());
    }

    @Test
    public void givenEmptySubscriptions_thenDoNotUpdateTyaNumber() {
        Subscription repSubscription = Subscription.builder().build();
        Subscription appellantSubscription = Subscription.builder().build();
        Subscription appointeeSubscription = Subscription.builder().build();
        Subscriptions subscriptions = Subscriptions.builder().appellantSubscription(appellantSubscription).appointeeSubscription(appointeeSubscription).representativeSubscription(repSubscription).build();

        sscsCaseData.setSubscriptions(subscriptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getSubscriptions().getAppellantSubscription().getTya());
        assertNull(response.getData().getSubscriptions().getAppointeeSubscription().getTya());
        assertNull(response.getData().getSubscriptions().getRepresentativeSubscription().getTya());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}
