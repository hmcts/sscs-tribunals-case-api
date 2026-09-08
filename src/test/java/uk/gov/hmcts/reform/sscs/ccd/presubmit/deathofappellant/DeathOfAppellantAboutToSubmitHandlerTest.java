package uk.gov.hmcts.reform.sscs.ccd.presubmit.deathofappellant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Optional;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;

class DeathOfAppellantAboutToSubmitHandlerTest {
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

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.byDefaultProvider()
                                                     .configure()
                                                     .messageInterpolator(new ParameterMessageInterpolator())
                                                     .buildValidatorFactory();

    protected static Validator validator = VALIDATOR_FACTORY.getValidator();

    private SscsCaseData sscsCaseData;
    private AutoCloseable autoCloseable;

    @BeforeEach
    void setUp() {
        autoCloseable = openMocks(this);
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
        SscsCaseData sscsCaseDataBefore = SscsCaseData.builder()
                                                      .ccdCaseId("ccdId")
                                                      .appeal(Appeal.builder().appellant(Appellant.builder().build()).build())
                                                      .dwpUcb("yes")
                                                      .build();
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);
        when(caseDetailsBefore.getState()).thenReturn(State.HEARING);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void givenANonDeathOfAppellantEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenValidCallback_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void givenADeathOfAppellantEvent_thenSetInterlocReviewStateAndRemoveUcb() {
        handler = new DeathOfAppellantAboutToSubmitHandler(validator, hearingMessageHelper, true);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.AWAITING_ADMIN_ACTION);
        assertThat(response.getData().getDwpUcb()).isNull();
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(),
            CancellationReason.PARTY_UNABLE_TO_ATTEND);
        verifyNoMoreInteractions(hearingMessageHelper);
    }

    @Test
    void givenADeathOfAppellantEventThatIsSubscribedToEmailAndSms_thenUnsubscribeFromEmailAndSms() {
        callback.getCaseDetails().getCaseData().setSubscriptions(Subscriptions.builder().appellantSubscription(Subscription.builder().subscribeEmail("Yes").subscribeSms("Yes").build()).build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.AWAITING_ADMIN_ACTION);
        assertThat(response.getData().getSubscriptions().getAppellantSubscription().getSubscribeEmail()).isEqualTo("No");
        assertThat(response.getData().getSubscriptions().getAppellantSubscription().getSubscribeSms()).isEqualTo("No");
        assertThat(response.getData().getSubscriptions().getAppellantSubscription().getWantSmsNotifications()).isEqualTo("No");
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenADeathOfAppellantEventThatHasNoSubscription_thenHandleUnsubscription() {
        callback.getCaseDetails().getCaseData().setSubscriptions(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.AWAITING_ADMIN_ACTION);
        assertThat(response.getData().getSubscriptions().getAppellantSubscription()).isNull();
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenADeathOfAppellantEventThatHasNoAppellantSubscription_thenHandleUnsubscription() {
        callback.getCaseDetails().getCaseData().setSubscriptions(Subscriptions.builder().appellantSubscription(null).build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.AWAITING_ADMIN_ACTION);
        assertThat(response.getData().getSubscriptions().getAppellantSubscription()).isNull();
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenADeathOfAppellantEventThatHasNoAppointeeBeforeAndHasAppointeeAfter_thenSetInterlocReviewState() {

        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setAppointee(null);
        caseDetails.getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        caseDetails.getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder().name(Name.builder().firstName("Tester").build()).build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.AWAITING_ADMIN_ACTION);
        assertThat(response.getData().getDwpState()).isNull();
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenADeathOfAppellantEventThatHasAppointeeBeforeAndItHasNowChanged_thenSetInterlocReviewState() {

        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder().name(Name.builder().firstName("Fred").build()).build());
        caseDetails.getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        caseDetails.getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder().name(Name.builder().firstName("Tester").build()).build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.AWAITING_ADMIN_ACTION);
        assertThat(response.getData().getDwpState()).isNull();
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenADeathOfAppellantEventThatHasNoAppointeeBeforeAndNoAppointeeAfter_thenSetInterlocReviewStateAndDwpState() {

        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setAppointee(null);
        caseDetails.getCaseData().getAppeal().getAppellant().setAppointee(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.AWAITING_ADMIN_ACTION);
        assertThat(response.getData().getDwpState()).isEqualTo(DwpState.APPOINTEE_DETAILS_NEEDED);
        verifyNoInteractions(hearingMessageHelper);
    }

    @ParameterizedTest
    @CsvSource(value = {"null, null", "no, null", "null, no", "no, no"}, nullValues = "null")
    void givenADeathOfAppellantEventThatHasWithAppointeeNoBeforeAndWithAppointeeNoAfter_thenSetInterlocReviewStateAndDwpState(String isAppointeeBefore, String isAppointeeAfter) {

        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setIsAppointee(isAppointeeBefore);
        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder().build());
        caseDetails.getCaseData().getAppeal().getAppellant().setIsAppointee(isAppointeeAfter);
        caseDetails.getCaseData().getAppeal().getAppellant().setAppointee(Appointee.builder().build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState()).isEqualTo(DwpState.APPOINTEE_DETAILS_NEEDED);
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenADeathOfAppellantEventThatHasAppointeeBeforeAndAppointeeAfterWithNoChange_thenSetInterlocReviewStateButNotDwpState() {

        Appointee appointee = Appointee.builder().name(Name.builder().firstName("Fred").build()).build();
        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setAppointee(appointee);
        caseDetailsBefore.getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");
        caseDetails.getCaseData().getAppeal().getAppellant().setAppointee(appointee);
        caseDetails.getCaseData().getAppeal().getAppellant().setIsAppointee("Yes");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.AWAITING_ADMIN_ACTION);
        assertThat(response.getData().getDwpState()).isNull();
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenADeathOfAppellantInFuture_thenDisplayAnError() {

        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        sscsCaseData.setDateOfAppellantDeath(tomorrow.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        final String error = response.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("Date of appellant death must not be in the future");
    }

    @Test
    void givenADeathOfAppellantInPast_thenDoNotDisplayAnError() {

        final LocalDate yesterday = LocalDate.now().minusDays(1);
        sscsCaseData.setDateOfAppellantDeath(yesterday.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenADeathOfAppellantWithNoJointPartyOnCase_thenClearConfidentialFlags() {
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YesNo.YES);
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeAppellant(
            DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(RequestOutcome.GRANTED).build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getIsConfidentialCase()).isNull();
        assertThat(response.getData().getIsAppellantDeceased()).isEqualTo(YesNo.YES);
        assertThat(response.getData().getConfidentialityRequestOutcomeAppellant()).isNull();
    }

    @Test
    void givenADeathOfAppellantWithJointPartyConfidentialRequestNotGranted_thenClearConfidentialFlagsForAppellant() {
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YesNo.YES);
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeAppellant(
            DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(RequestOutcome.GRANTED).build());
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeJointParty(
            DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(RequestOutcome.IN_PROGRESS).build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getIsConfidentialCase()).isNull();
        assertThat(response.getData().getIsAppellantDeceased()).isEqualTo(YesNo.YES);
        assertThat(response.getData().getConfidentialityRequestOutcomeAppellant()).isNull();
        assertThat(response.getData().getConfidentialityRequestOutcomeJointParty().getRequestOutcome()).isEqualTo(RequestOutcome.IN_PROGRESS);
    }

    @Test
    void givenADeathOfAppellantWithJointPartyOnCaseAndConfidentialRequestGranted_thenClearConfidentialFlagForAppellantAndDoNotClearConfidentialFlagOnCase() {
        callback.getCaseDetails().getCaseData().setIsConfidentialCase(YesNo.YES);
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeAppellant(
            DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(RequestOutcome.GRANTED).build());
        callback.getCaseDetails().getCaseData().setConfidentialityRequestOutcomeJointParty(
            DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(RequestOutcome.GRANTED).build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getIsConfidentialCase()).isEqualTo(YesNo.YES);
        assertThat(response.getData().getIsAppellantDeceased()).isEqualTo(YesNo.YES);
        assertThat(response.getData().getConfidentialityRequestOutcomeAppellant()).isNull();
        assertThat(response.getData().getConfidentialityRequestOutcomeJointParty().getRequestOutcome()).isEqualTo(RequestOutcome.GRANTED);
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);

        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class);
    }

}
