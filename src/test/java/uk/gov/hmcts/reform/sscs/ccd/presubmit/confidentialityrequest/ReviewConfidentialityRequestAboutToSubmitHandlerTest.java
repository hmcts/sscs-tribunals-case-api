package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityrequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FE_ACTIONED_NR;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;

@RunWith(JUnitParamsRunner.class)
public class ReviewConfidentialityRequestAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private ReviewConfidentialityRequestAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        handler = new ReviewConfidentialityRequestAboutToSubmitHandler(false);

        when(callback.getEvent()).thenReturn(EventType.REVIEW_CONFIDENTIALITY_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);


        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .dwpState(FE_ACTIONED_NR)
            .interlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE)
            .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonReviewConfidentialityRequestEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    private DatedRequestOutcome createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome outcome) {
        return outcome == null ? null : DatedRequestOutcome.builder().date(LocalDate.now().minusDays(1)).requestOutcome(outcome).build();
    }

    private DatedRequestOutcome createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome outcome) {
        return outcome == null ? null : DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(outcome).build();
    }

    @Parameters({
        "null, null",
        "null, GRANTED",
        "null, REFUSED",
        "GRANTED, null",
        "GRANTED, GRANTED",
        "GRANTED, REFUSED",
        "REFUSED, null",
        "REFUSED, GRANTED",
        "REFUSED, REFUSED"
    })
    @Test
    public void givenNoConfidentialityRequestsAreInProgressShouldDisplayAnError(@Nullable RequestOutcome appellantOutcome, @Nullable RequestOutcome jointPartyOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review. Please check case data. If problem continues please contact support", error);

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());

        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }

    @Parameters({"null", "GRANTED", "REFUSED"})
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfGrantedOrRefusedSetIncorrectly(@Nullable RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());

        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Parameters({"null", "GRANTED", "REFUSED"})
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfGrantedSet(@Nullable RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION, sscsCaseData.getInterlocReviewState());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Parameters({
        "null, something",
        "null, grantConfidentialityRequest",
        "null, refuseConfidentialityRequest",
        "GRANTED, something",
        "GRANTED, grantConfidentialityRequest",
        "GRANTED, refuseConfidentialityRequest",
        "REFUSED, something",
        "REFUSED, grantConfidentialityRequest",
        "REFUSED, refuseConfidentialityRequest"
    })
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfAppellantGrantedSetAndJointPartyReviewValueSet(@Nullable RequestOutcome jointPartyRequestOutcome, @Nullable String jointPartyReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(jointPartyReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is not in progress but value set for granted or refused is:" + jointPartyReviewValue + ". Please check case data. If problem continues please contact support", error);

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());

        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters({"null", "GRANTED", "REFUSED"})
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfRefusedSet(@Nullable RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        Assert.assertNull(sscsCaseData.getInterlocReviewState());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Parameters({"something", "grantConfidentialityRequest", "refuseConfidentialityRequest"})
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfAppellantRefusedSetAndJointPartyReviewValueSet(String jointPartyReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(jointPartyReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is not in progress but value set for granted or refused is:" + jointPartyReviewValue + ". Please check case data. If problem continues please contact support", error);

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters({"null", "GRANTED", "REFUSED"})
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfGrantedOrRefusedSetIncorrectly(@Nullable RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());

        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());

    }

    @Parameters({"null", "GRANTED", "REFUSED"})
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfGrantedSet(@Nullable RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION, sscsCaseData.getInterlocReviewState());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters({
        "null, something",
        "null, grantConfidentialityRequest",
        "null, refuseConfidentialityRequest",
        "GRANTED, something",
        "GRANTED, grantConfidentialityRequest",
        "GRANTED, refuseConfidentialityRequest",
        "REFUSED, something",
        "REFUSED, grantConfidentialityRequest",
        "REFUSED, refuseConfidentialityRequest"
    })
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfJointPartyGrantedSetAndAppellantReviewValueSet(@Nullable RequestOutcome appellantRequestOutcome, @Nullable String appellantReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(appellantReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is not in progress but value set for granted or refused is:" + appellantReviewValue + ". Please check case data. If problem continues please contact support", error);

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }


    @Parameters({"null", "GRANTED", "REFUSED"})
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfRefusedSet(@Nullable RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        Assert.assertNull(sscsCaseData.getInterlocReviewState());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters({"something", "grantConfidentialityRequest", "refuseConfidentialityRequest"})
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfAppellantRefusedSetAndAppellantReviewValueSet(String appellantReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(appellantReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is not in progress but value set for granted or refused is:" + appellantReviewValue + ". Please check case data. If problem continues please contact support", error);

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());

        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getIsConfidentialCase());
        Assert.assertNull(sscsCaseData.getIsProgressingViaGaps());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantGrantedAndJointPartyGranted() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION, sscsCaseData.getInterlocReviewState());
        assertEquals(YES, sscsCaseData.getIsConfidentialCase());
        assertEquals(YES.getValue(), sscsCaseData.getIsProgressingViaGaps());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantGrantedAndJointPartyRefused() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION, sscsCaseData.getInterlocReviewState());
        assertEquals(YES, sscsCaseData.getIsConfidentialCase());
        assertEquals(YES.getValue(), sscsCaseData.getIsProgressingViaGaps());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantRefusedAndJointPartyGranted() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION, sscsCaseData.getInterlocReviewState());
        assertEquals(YES, sscsCaseData.getIsConfidentialCase());
        assertEquals(YES.getValue(), sscsCaseData.getIsProgressingViaGaps());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantRefusedAndJointPartyRefused() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNull(sscsCaseData.getInterlocReviewState());
        Assert.assertNull(sscsCaseData.getIsConfidentialCase());
        Assert.assertNull(sscsCaseData.getIsProgressingViaGaps());

    }

    @Parameters({
        "grantConfidentialityRequest",
        "refuseConfidentialityRequest"
    })
    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantReviewValuePopulatedAndJointPartyReviewValueSetIncorrectly(String appellantReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(appellantReviewValue);
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
        Assert.assertNull(sscsCaseData.getIsConfidentialCase());
        Assert.assertNull(sscsCaseData.getIsProgressingViaGaps());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyGrantedAndAppellantReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
    }

    @Test
    @Parameters({"something", "grantConfidentialityRequest", "refuseConfidentialityRequest"})
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyReviewValuePopulatedAndAppellantReviewValueNotSet(String jointPartyReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(jointPartyReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyRefusedAndAppellantReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenConfidentialityRequestAppellantGrantedAndStateIsResponseReviewed_thenSetClearDwpStateAndInterlocReviewState() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        when(callback.getCaseDetails().getState()).thenReturn(State.RESPONSE_RECEIVED);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertNull(sscsCaseData.getInterlocReviewState());
        assertEquals(State.RESPONSE_RECEIVED, callback.getCaseDetails().getState());
    }

    @Test
    public void givenConfidentialityRequestJointPartyGrantedAndStateIsResponseReviewed_thenSetClearDwpStateAndInterlocReviewState() {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");
        when(callback.getCaseDetails().getState()).thenReturn(State.RESPONSE_RECEIVED);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertNull(sscsCaseData.getInterlocReviewState());
        assertEquals(State.RESPONSE_RECEIVED, callback.getCaseDetails().getState());
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheRequest() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Parameters({
        "null, null, null, null",
        "null, GRANTED, null, null",
        "null, REFUSED, null, null",
        "null, IN_PROGRESS, null, grantConfidentialityRequest",
        "null, IN_PROGRESS, null, refuseConfidentialityRequest",
        "GRANTED, null, null, null",
        "GRANTED, GRANTED, null, null",
        "GRANTED, REFUSED, null, null",
        "GRANTED, IN_PROGRESS, null, grantConfidentialityRequest",
        "GRANTED, IN_PROGRESS, null, refuseConfidentialityRequest",
        "REFUSED, null, null, null",
        "REFUSED, GRANTED, null, null",
        "REFUSED, REFUSED, null, null",
        "REFUSED, IN_PROGRESS, null, grantConfidentialityRequest",
        "REFUSED, IN_PROGRESS, null, refuseConfidentialityRequest",
        "IN_PROGRESS, null, grantConfidentialityRequest, null",
        "IN_PROGRESS, GRANTED, grantConfidentialityRequest, null",
        "IN_PROGRESS, REFUSED, grantConfidentialityRequest, null",
        "IN_PROGRESS, IN_PROGRESS, grantConfidentialityRequest, grantConfidentialityRequest",
        "IN_PROGRESS, IN_PROGRESS, grantConfidentialityRequest, refuseConfidentialityRequest",
        "IN_PROGRESS, GRANTED, refuseConfidentialityRequest, null",
        "IN_PROGRESS, REFUSED, refuseConfidentialityRequest, null",
        "IN_PROGRESS, IN_PROGRESS, refuseConfidentialityRequest, grantConfidentialityRequest",
        "IN_PROGRESS, IN_PROGRESS, refuseConfidentialityRequest, refuseConfidentialityRequest"
    })
    @Test
    public void givenConfidentialityRequestsCombinationShouldResetNotSetValuesToNull(@Nullable RequestOutcome appellantOutcome, @Nullable RequestOutcome jointPartyOutcome, @Nullable String appellantReviewText, @Nullable String jointPartyReviewText) {

        sscsCaseData.getJointParty().setHasJointParty(YES);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome));
        if (RequestOutcome.IN_PROGRESS.equals(appellantOutcome)) {
            sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(appellantReviewText);
        }
        if (RequestOutcome.IN_PROGRESS.equals(jointPartyOutcome)) {
            sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(jointPartyReviewText);
        }

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        if (RequestOutcome.IN_PROGRESS.equals(appellantOutcome) || RequestOutcome.IN_PROGRESS.equals(jointPartyOutcome)) {
            assertEquals(0, response.getErrors().size());
        } else {
            assertEquals(1, response.getErrors().size());
            String error = response.getErrors().stream().findFirst().orElse("");
            assertEquals("There is no confidentiality request to review. Please check case data. If problem continues please contact support", error);
        }

        if (!RequestOutcome.IN_PROGRESS.equals(appellantOutcome)) {
            assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        }
        if (!RequestOutcome.IN_PROGRESS.equals(jointPartyOutcome)) {
            assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        }
    }

    @Test
    public void givenTheEnhancedConfidentialityFlagIsSetDoNotSetTheInterlocReviewStateOrProgressingViaGapsField() {
        handler = new ReviewConfidentialityRequestAboutToSubmitHandler(true);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        assertEquals(FE_ACTIONED_NR, sscsCaseData.getDwpState());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertThat(sscsCaseData.getInterlocReviewState(), is(nullValue()));
        assertEquals(YES, sscsCaseData.getIsConfidentialCase());
        assertNull(sscsCaseData.getIsProgressingViaGaps());
    }
}
