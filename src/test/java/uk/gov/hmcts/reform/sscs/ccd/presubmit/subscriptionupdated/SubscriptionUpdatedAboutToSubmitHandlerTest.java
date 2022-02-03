package uk.gov.hmcts.reform.sscs.ccd.presubmit.subscriptionupdated;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
        openMocks(this);
        handler = new SubscriptionUpdatedAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.SUBSCRIPTION_UPDATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(Benefit.PIP.getShortName()).build()).build())
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
    public void givenJointPartySubscriptionWithNoExistingTya_thenAutomaticallyGenerateARandomNumberForTya() {
        Subscription jointPartySubscription = Subscription.builder().tya(null).email("email").build();
        Subscriptions subscriptions = Subscriptions.builder().jointPartySubscription(jointPartySubscription).build();
        sscsCaseData.setSubscriptions(subscriptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getSubscriptions().getJointPartySubscription().getTya());
        assertEquals("email", response.getData().getSubscriptions().getJointPartySubscription().getEmail());
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
    public void givenJointPartySubscriptionWithExistingTya_thenUseExistingTyaNumber() {
        Subscription jointPartySubscription = Subscription.builder().tya("123").email("email").build();
        Subscriptions subscriptions = Subscriptions.builder().jointPartySubscription(jointPartySubscription).build();
        sscsCaseData.setSubscriptions(subscriptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("123", response.getData().getSubscriptions().getJointPartySubscription().getTya());
        assertEquals("email", response.getData().getSubscriptions().getJointPartySubscription().getEmail());
    }

    @Test
    public void givenEmptySubscriptions_thenDoNotUpdateTyaNumber() {
        Subscription repSubscription = Subscription.builder().build();
        Subscription appellantSubscription = Subscription.builder().build();
        Subscription appointeeSubscription = Subscription.builder().build();
        Subscription jointPartySubscription = Subscription.builder().build();
        Subscriptions subscriptions = Subscriptions.builder().appellantSubscription(appellantSubscription).appointeeSubscription(appointeeSubscription).representativeSubscription(repSubscription).jointPartySubscription(jointPartySubscription).build();

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

    @Test
    public void givenOtherPartiesChange_thenDisplayError() {
        when(callback.getEvent()).thenReturn(EventType.SUBSCRIPTION_UPDATED);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());
        sscsCaseData.setOtherParties(Collections.emptyList());
        SscsCaseData previousCaseData = SscsCaseData.builder()
                .otherParties(Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build())).build();
        Optional<CaseDetails<SscsCaseData>> beforeData =
                Optional.of(new CaseDetails<SscsCaseData>(33333333L, "", State.APPEAL_CREATED, previousCaseData, LocalDateTime.now(), "Benefit"));
        when(callback.getCaseDetailsBefore()).thenReturn(beforeData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenBeforeOtherPartiesNullNowEmptyList_doNotDisplayError() {
        when(callback.getEvent()).thenReturn(EventType.SUBSCRIPTION_UPDATED);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());
        sscsCaseData.setOtherParties(Collections.emptyList());

        SscsCaseData previousCaseData = SscsCaseData.builder()
                .otherParties(null).build();
        Optional<CaseDetails<SscsCaseData>> beforeData =
                Optional.of(new CaseDetails<SscsCaseData>(33333333L, "", State.APPEAL_CREATED, previousCaseData, LocalDateTime.now(), "Benefit"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenOtherPartiesSubscriptionsAdded_thenTyaAdded() {
        when(callback.getEvent()).thenReturn(EventType.SUBSCRIPTION_UPDATED);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());
        List<CcdValue<OtherParty>> otherParties =
                Arrays.asList(CcdValue.<OtherParty>builder().value(
                        OtherParty.builder().id("other_party_1")
                                .otherPartySubscription(Subscription.builder().email("user1@email.com").build())
                                .otherPartyAppointeeSubscription(Subscription.builder().email("user2@email.com").build())
                                .otherPartyRepresentativeSubscription(Subscription.builder().email("user3@email.com").build())
                                .build()).build());

        sscsCaseData.setOtherParties(otherParties);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getData().getOtherParties().get(0).getValue().getOtherPartySubscription().getTya() != null);
        assertTrue(response.getData().getOtherParties().get(0).getValue().getOtherPartyAppointeeSubscription().getTya() != null);
        assertTrue(response.getData().getOtherParties().get(0).getValue().getOtherPartyRepresentativeSubscription().getTya() != null);
    }

    @Test
    public void givenEmptyOtherParties_thenSetToNullRatherThanEmpty() {
        sscsCaseData.setOtherParties(emptyList());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getOtherParties(), is(nullValue()));
    }

}
