package uk.gov.hmcts.reform.sscs.ccd.presubmit.deathofappellant;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.time.LocalDate;
import java.util.Optional;
import javax.validation.Validation;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;

@RunWith(JUnitParamsRunner.class)
public class DeathOfAppellantAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private DeathOfAppellantAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    @Mock
    private ListAssistHearingMessageHelper hearingMessageHelper;

    protected static Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    private SscsCaseData sscsCaseData;
    private SscsCaseData sscsCaseDataBefore;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new DeathOfAppellantAboutToSubmitHandler(validator, hearingMessageHelper, false);


        when(callback.getEvent()).thenReturn(EventType.DEATH_OF_APPELLANT);
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .appeal(Appeal.builder().appellant(Appellant.builder().build()).build())
                .dwpUcb("yes")
                .schedulingAndListingFields(SchedulingAndListingFields.builder()
                        .hearingRoute(HearingRoute.LIST_ASSIST)
                        .build())
                .build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        sscsCaseDataBefore = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .appeal(Appeal.builder().appellant(Appellant.builder().build()).build())
                .dwpUcb("yes")
                .build();
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);
        when(caseDetailsBefore.getState()).thenReturn(State.HEARING);
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
        handler = new DeathOfAppellantAboutToSubmitHandler(validator, hearingMessageHelper, true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertNull(response.getData().getDwpUcb());
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()),
                eq(CancellationReason.PARTY_UNABLE_TO_ATTEND));
        verifyNoMoreInteractions(hearingMessageHelper);
    }

    @Test
    public void givenADeathOfAppellantEventThatIsSubscribedToEmailAndSms_thenUnsubscribeFromEmailAndSms() {
        callback.getCaseDetails().getCaseData().setSubscriptions(Subscriptions.builder().appellantSubscription(Subscription.builder().subscribeEmail("Yes").subscribeSms("Yes").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertEquals("No", response.getData().getSubscriptions().getAppellantSubscription().getSubscribeEmail());
        assertEquals("No", response.getData().getSubscriptions().getAppellantSubscription().getSubscribeSms());
        assertEquals("No", response.getData().getSubscriptions().getAppellantSubscription().getWantSmsNotifications());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    public void givenADeathOfAppellantEventThatHasNoSubscription_thenHandleUnsubscription() {
        callback.getCaseDetails().getCaseData().setSubscriptions(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertNull(response.getData().getSubscriptions().getAppellantSubscription());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    public void givenADeathOfAppellantEventThatHasNoAppellantSubscription_thenHandleUnsubscription() {
        callback.getCaseDetails().getCaseData().setSubscriptions(Subscriptions.builder().appellantSubscription(null).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertNull(response.getData().getSubscriptions().getAppellantSubscription());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    public void givenADeathOfAppellantEventThatHasNoAppointeeBeforeAndHasAppointeeAfter_thenSetInterlocReviewState() {

        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setAppointee(null);
        caseDetails.getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        caseDetails.getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder().name(Name.builder().firstName("Tester").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertNull(response.getData().getDwpState());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    public void givenADeathOfAppellantEventThatHasAppointeeBeforeAndItHasNowChanged_thenSetInterlocReviewState() {

        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder().name(Name.builder().firstName("Fred").build()).build());
        caseDetails.getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        caseDetails.getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder().name(Name.builder().firstName("Tester").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertNull(response.getData().getDwpState());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
        public void givenADeathOfAppellantEventThatHasNoAppointeeBeforeAndNoAppointeeAfter_thenSetInterlocReviewStateAndDwpState() {

        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setAppointee(null);
        caseDetails.getCaseData().getAppeal().getAppellant().setAppointee(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertEquals(DwpState.APPOINTEE_DETAILS_NEEDED, response.getData().getDwpState());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    @Parameters({"null, null", "no, null", "null, no", "no, no"})
    public void givenADeathOfAppellantEventThatHasWithAppointeeNoBeforeAndWithAppointeeNoAfter_thenSetInterlocReviewStateAndDwpState(@Nullable String isAppointeeBefore, @Nullable String isAppointeeAfter) {

        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setIsAppointee(isAppointeeBefore);
        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder().build());
        caseDetails.getCaseData().getAppeal().getAppellant().setIsAppointee(isAppointeeAfter);
        caseDetails.getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DwpState.APPOINTEE_DETAILS_NEEDED, response.getData().getDwpState());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    public void givenADeathOfAppellantEventThatHasAppointeeBeforeAndAppointeeAfterWithNoChange_thenDoNotSetInterlocReviewStateOrDwpState() {

        Appointee appointee = Appointee.builder().name(Name.builder().firstName("Fred").build()).build();
        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setAppointee(appointee);
        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        caseDetails.getCaseData().getAppeal().getAppellant().setAppointee(appointee);
        caseDetails.getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertNull(response.getData().getDwpState());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    public void givenADeathOfAppellantInFuture_thenDisplayAnError() {

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        sscsCaseData.setDateOfAppellantDeath(tomorrow.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Date of appellant death must not be in the future", error);
    }

    @Test
    public void givenADeathOfAppellantInPast_thenDoNotDisplayAnError() {

        LocalDate yesterday = LocalDate.now().minusDays(1);
        sscsCaseData.setDateOfAppellantDeath(yesterday.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenADeathOfAppellantWithNoJointPartyOnCase_thenClearConfidentialFlags() {
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YesNo.YES);
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeAppellant(
                DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(RequestOutcome.GRANTED).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getIsConfidentialCase());
        assertEquals(YesNo.YES, response.getData().getIsAppellantDeceased());
        assertNull(response.getData().getConfidentialityRequestOutcomeAppellant());
    }

    @Test
    public void givenADeathOfAppellantWithJointPartyConfidentialRequestNotGranted_thenClearConfidentialFlagsForAppellant() {
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YesNo.YES);
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeAppellant(
                DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(RequestOutcome.GRANTED).build());
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeJointParty(
                DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(RequestOutcome.IN_PROGRESS).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getIsConfidentialCase());
        assertEquals(YesNo.YES, response.getData().getIsAppellantDeceased());
        assertNull(response.getData().getConfidentialityRequestOutcomeAppellant());
        assertEquals(RequestOutcome.IN_PROGRESS, response.getData().getConfidentialityRequestOutcomeJointParty().getRequestOutcome());
    }

    @Test
    public void givenADeathOfAppellantWithJointPartyOnCaseAndConfidentialRequestGranted_thenClearConfidentialFlagForAppellantAndDoNotClearConfidentialFlagOnCase() {
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YesNo.YES);
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeAppellant(
                DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(RequestOutcome.GRANTED).build());
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeJointParty(
                DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(RequestOutcome.GRANTED).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.YES, response.getData().getIsConfidentialCase());
        assertEquals(YesNo.YES, response.getData().getIsAppellantDeceased());
        assertNull(response.getData().getConfidentialityRequestOutcomeAppellant());
        assertEquals(RequestOutcome.GRANTED, response.getData().getConfidentialityRequestOutcomeJointParty().getRequestOutcome());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}
