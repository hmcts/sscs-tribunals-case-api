package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityrequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.io.IOException;
import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;

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
        handler = new ReviewConfidentialityRequestAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.REVIEW_CONFIDENTIALITY_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);


        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .dwpState("previousDwpState")
            .interlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE.getId())
            .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonReviewConfidentialityRequestEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }


    @NamedParameters("noConfidentialityRequestsAreInProgress")
    @SuppressWarnings("unused")
    private Object[] noConfidentialityRequestsAreInProgress() {
        return new Object[] {
            new RequestOutcome[] {null, null},
            new RequestOutcome[] {null, RequestOutcome.NOT_SET},
            new RequestOutcome[] {null, RequestOutcome.GRANTED},
            new RequestOutcome[] {null, RequestOutcome.REFUSED},
            new RequestOutcome[] {RequestOutcome.GRANTED, null},
            new RequestOutcome[] {RequestOutcome.GRANTED, RequestOutcome.NOT_SET},
            new RequestOutcome[] {RequestOutcome.GRANTED, RequestOutcome.GRANTED},
            new RequestOutcome[] {RequestOutcome.GRANTED, RequestOutcome.REFUSED},
            new RequestOutcome[] {RequestOutcome.REFUSED, null},
            new RequestOutcome[] {RequestOutcome.REFUSED, RequestOutcome.NOT_SET},
            new RequestOutcome[] {RequestOutcome.REFUSED, RequestOutcome.GRANTED},
            new RequestOutcome[] {RequestOutcome.REFUSED, RequestOutcome.REFUSED},
            new RequestOutcome[] {RequestOutcome.NOT_SET, null},
            new RequestOutcome[] {RequestOutcome.NOT_SET, RequestOutcome.NOT_SET},
            new RequestOutcome[] {RequestOutcome.NOT_SET, RequestOutcome.GRANTED},
            new RequestOutcome[] {RequestOutcome.NOT_SET, RequestOutcome.REFUSED},
        };
    }

    @NamedParameters("noRequestIsInProgress")
    @SuppressWarnings("unused")
    private Object[] noRequestIsInProgress() {
        return new Object[] {
            new RequestOutcome[] {null},
            new RequestOutcome[] {RequestOutcome.GRANTED},
            new RequestOutcome[] {RequestOutcome.REFUSED},
            new RequestOutcome[] {RequestOutcome.NOT_SET},

        };
    }

    @NamedParameters("populatedRequestReviewValues")
    @SuppressWarnings("unused")
    private Object[] populatedRequestReviewValues() {
        return new Object[] {
            new String[] {"something"},
            new String[] {"grantConfidentialityRequest"},
            new String[] {"refuseConfidentialityRequest"}
        };
    }

    @NamedParameters("noRequestIsInProgressAndPopulatedRequestReviewValues")
    @SuppressWarnings("unused")
    private Object[] noRequestIsInProgressAndPopulatedRequestReviewValues() {
        return new Object[] {
            new Object[] {null, "something"},
            new Object[] {null, "grantConfidentialityRequest"},
            new Object[] {null, "refuseConfidentialityRequest"},
            new Object[] {RequestOutcome.NOT_SET, "something"},
            new Object[] {RequestOutcome.NOT_SET, "grantConfidentialityRequest"},
            new Object[] {RequestOutcome.NOT_SET, "refuseConfidentialityRequest"},
            new Object[] {RequestOutcome.GRANTED, "something"},
            new Object[] {RequestOutcome.GRANTED, "grantConfidentialityRequest"},
            new Object[] {RequestOutcome.GRANTED, "refuseConfidentialityRequest"},
            new Object[] {RequestOutcome.REFUSED, "something"},
            new Object[] {RequestOutcome.REFUSED, "grantConfidentialityRequest"},
            new Object[] {RequestOutcome.REFUSED, "refuseConfidentialityRequest"}
        };
    }

    private DatedRequestOutcome createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome outcome, boolean treatNotSetAsNull) {
        return outcome == null || (treatNotSetAsNull && RequestOutcome.NOT_SET.equals(outcome)) ? null : DatedRequestOutcome.builder().date(LocalDate.now().minusDays(1)).requestOutcome(outcome).build();
    }

    private DatedRequestOutcome createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome outcome, boolean treatNotSetAsNull) {
        return outcome == null || (treatNotSetAsNull && RequestOutcome.NOT_SET.equals(outcome)) ? null : DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(outcome).build();
    }

    @Parameters(named = "noConfidentialityRequestsAreInProgress")
    @Test
    public void givenNoConfidentialityRequestsAreInProgressShouldDisplayAnError(RequestOutcome appellantOutcome, RequestOutcome jointPartyOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome, false));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome, false), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome, false), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfGrantedOrRefusedSetIncorrectly(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome, false));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome, false), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfGrantedSet(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome, false));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());
        
        Assert.assertEquals(DwpState.CONFIDENTIALITY_ACTION_REQUIRED.getId(), sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Parameters(named = "noRequestIsInProgressAndPopulatedRequestReviewValues")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfAppellantGrantedSetAndJointPartyReviewValueSet(RequestOutcome jointPartyRequestOutcome, String jointPartyReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome, false));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(jointPartyReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is not in progress but value set for granted or refused is:" + jointPartyReviewValue + ". Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome, false), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfRefusedSet(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome, false));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.NONE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Parameters(named = "populatedRequestReviewValues")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfAppellantRefusedSetAndJointPartyReviewValueSet(String jointPartyReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(jointPartyReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is not in progress but value set for granted or refused is:" + jointPartyReviewValue + ". Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfGrantedOrRefusedSetIncorrectly(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome, false), sscsCaseData.getConfidentialityRequestOutcomeAppellant());

    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfGrantedSet(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals(DwpState.CONFIDENTIALITY_ACTION_REQUIRED.getId(), sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters(named = "noRequestIsInProgressAndPopulatedRequestReviewValues")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfJointPartyGrantedSetAndAppellantReviewValueSet(RequestOutcome appellantRequestOutcome, String appellantReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(appellantReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is not in progress but value set for granted or refused is:" + appellantReviewValue + ". Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome, false), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }


    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfRefusedSet(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.NONE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters(named = "populatedRequestReviewValues")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfAppellantRefusedSetAndAppellantReviewValueSet(String appellantReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(appellantReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is not in progress but value set for granted or refused is:" + appellantReviewValue + ". Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantGrantedAndJointPartyGranted() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("confidentialityActionRequired", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantGrantedAndJointPartyRefused() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("confidentialityActionRequired", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantRefusedAndJointPartyGranted() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("confidentialityActionRequired", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantRefusedAndJointPartyRefused() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.NONE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Parameters(named = "populatedRequestReviewValues")
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantReviewValuePopulatedAndJointPartyReviewValueSetIncorrectly(String appellantReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(appellantReviewValue);
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantGrantedAndJointPartyReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantRefusedAndJointPartyReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantRefusedAndJointPartyReviewValueNotSet() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());



        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantNotSetAndJointPartyReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantSetIncorrectlyAndJointPartyReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantSetIncorrectlyAndJointPartyReviewValueNotSet() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyGrantedAndAppellantReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    @Parameters(named = "populatedRequestReviewValues")
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyReviewValuePopulatedAndAppellantReviewValueNotSet(String jointPartyReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(jointPartyReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyRefusedAndAppellantReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyNotSetAndAppellantReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, false));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS, true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
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
}
