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
            new RequestOutcome[] {null, RequestOutcome.GRANTED},
            new RequestOutcome[] {null, RequestOutcome.REFUSED},
            new RequestOutcome[] {RequestOutcome.GRANTED, null},
            new RequestOutcome[] {RequestOutcome.GRANTED, RequestOutcome.GRANTED},
            new RequestOutcome[] {RequestOutcome.GRANTED, RequestOutcome.REFUSED},
            new RequestOutcome[] {RequestOutcome.REFUSED, null},
            new RequestOutcome[] {RequestOutcome.REFUSED, RequestOutcome.GRANTED},
            new RequestOutcome[] {RequestOutcome.REFUSED, RequestOutcome.REFUSED},
        };
    }

    @NamedParameters("noRequestIsInProgress")
    @SuppressWarnings("unused")
    private Object[] noRequestIsInProgress() {
        return new Object[] {
            new RequestOutcome[] {null},
            new RequestOutcome[] {RequestOutcome.GRANTED},
            new RequestOutcome[] {RequestOutcome.REFUSED},
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
            new Object[] {RequestOutcome.GRANTED, "something"},
            new Object[] {RequestOutcome.GRANTED, "grantConfidentialityRequest"},
            new Object[] {RequestOutcome.GRANTED, "refuseConfidentialityRequest"},
            new Object[] {RequestOutcome.REFUSED, "something"},
            new Object[] {RequestOutcome.REFUSED, "grantConfidentialityRequest"},
            new Object[] {RequestOutcome.REFUSED, "refuseConfidentialityRequest"}
        };
    }

    @Parameters(named = "noConfidentialityRequestsAreInProgress")
    @Test
    public void givenNoConfidentialityRequestsAreInProgressShouldDisplayAnError(RequestOutcome appellantOutcome, RequestOutcome jointPartyOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyOutcome);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("At least one confidentiality request should be in progress. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestDate());

        Assert.assertEquals(appellantOutcome, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(jointPartyOutcome, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfGrantedOrRefusedSetIncorrectly(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());

        Assert.assertEquals(jointPartyRequestOutcome, sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfGrantedSet(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());
        
        Assert.assertEquals(DwpState.CONFIDENTIALITY_ACTION_REQUIRED.getId(), sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(RequestOutcome.GRANTED, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestDate());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());
        Assert.assertEquals(jointPartyRequestOutcome, sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Parameters(named = "noRequestIsInProgressAndPopulatedRequestReviewValues")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfAppellantGrantedSetAndJointPartyReviewValueSet(RequestOutcome jointPartyRequestOutcome, String jointPartyReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(jointPartyReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is not in progress but value set for granted or refused is:" + jointPartyReviewValue + ". Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());

        Assert.assertEquals(jointPartyRequestOutcome, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfRefusedSet(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(jointPartyRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.NONE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(RequestOutcome.REFUSED, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestDate());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());
        Assert.assertEquals(jointPartyRequestOutcome, sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Parameters(named = "populatedRequestReviewValues")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfAppellantRefusedSetAndJointPartyReviewValueSet(String jointPartyReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(jointPartyReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is not in progress but value set for granted or refused is:" + jointPartyReviewValue + ". Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfGrantedOrRefusedSetIncorrectly(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());

        Assert.assertEquals(appellantRequestOutcome, sscsCaseData.getConfidentialityRequestOutcomeAppellant());

    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfGrantedSet(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals(DwpState.CONFIDENTIALITY_ACTION_REQUIRED.getId(), sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(RequestOutcome.GRANTED, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestDate());

        Assert.assertEquals(appellantRequestOutcome, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters(named = "noRequestIsInProgressAndPopulatedRequestReviewValues")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfJointPartyGrantedSetAndAppellantReviewValueSet(RequestOutcome appellantRequestOutcome, String appellantReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(appellantReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is not in progress but value set for granted or refused is:" + appellantReviewValue + ". Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());

        Assert.assertEquals(appellantRequestOutcome, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }


    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfRefusedSet(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(appellantRequestOutcome);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.NONE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(RequestOutcome.REFUSED, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestDate());

        Assert.assertEquals(appellantRequestOutcome, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters(named = "populatedRequestReviewValues")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfAppellantRefusedSetAndAppellantReviewValueSet(String appellantReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(appellantReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is not in progress but value set for granted or refused is:" + appellantReviewValue + ". Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantGrantedAndJointPartyGranted() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("confidentialityActionRequired", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.GRANTED, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.GRANTED, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantGrantedAndJointPartyRefused() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("confidentialityActionRequired", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.REFUSED, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.GRANTED, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantRefusedAndJointPartyGranted() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("confidentialityActionRequired", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.GRANTED, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.REFUSED, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantRefusedAndJointPartyRefused() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.REFUSED, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.REFUSED, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.NONE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantGrantedAndJointPartyReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantGrantedAndJointPartyReviewValueNotSet() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantRefusedAndJointPartyReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantRefusedAndJointPartyReviewValueNotSet() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());



        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantNotSetAndJointPartyReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantNotSetAndJointPartyReviewValueNotSet() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantSetIncorrectlyAndJointPartyReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantSetIncorrectlyAndJointPartyReviewValueNotSet() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyGrantedAndAppellantReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyGrantedAndAppellantReviewValueNotSet() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyRefusedAndAppellantReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyRefusedAndAppellantReviewValueNotSet() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyNotSetAndAppellantReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartySetIncorrectlyAndAppellantReviewValueNotSet() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(RequestOutcome.IN_PROGRESS);

        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());
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
