package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityrequest;

import static org.junit.Assert.*;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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

    @NamedParameters("allValidConfidentialityRequestCombinations")
    @SuppressWarnings("unused")
    private Object[] allValidConfidentialityRequestCombinations() {
        return new Object[] {
            new Object[] {null, null, null, null},
            new Object[] {null, RequestOutcome.GRANTED, null, null},
            new Object[] {null, RequestOutcome.REFUSED, null, null},
            new Object[] {null, RequestOutcome.IN_PROGRESS, null, "grantConfidentialityRequest"},
            new Object[] {null, RequestOutcome.IN_PROGRESS, null, "refuseConfidentialityRequest"},
            new Object[] {RequestOutcome.GRANTED, null, null, null},
            new Object[] {RequestOutcome.GRANTED, RequestOutcome.GRANTED, null, null},
            new Object[] {RequestOutcome.GRANTED, RequestOutcome.REFUSED, null, null},
            new Object[] {RequestOutcome.GRANTED, RequestOutcome.IN_PROGRESS, null, "grantConfidentialityRequest"},
            new Object[] {RequestOutcome.GRANTED, RequestOutcome.IN_PROGRESS, null, "refuseConfidentialityRequest"},
            new Object[] {RequestOutcome.REFUSED, null, null, null},
            new Object[] {RequestOutcome.REFUSED, RequestOutcome.GRANTED, null, null},
            new Object[] {RequestOutcome.REFUSED, RequestOutcome.REFUSED, null, null},
            new Object[] {RequestOutcome.REFUSED, RequestOutcome.IN_PROGRESS, null, "grantConfidentialityRequest"},
            new Object[] {RequestOutcome.REFUSED, RequestOutcome.IN_PROGRESS, null, "refuseConfidentialityRequest"},
            new Object[] {RequestOutcome.IN_PROGRESS, null, "grantConfidentialityRequest", null},
            new Object[] {RequestOutcome.IN_PROGRESS, RequestOutcome.GRANTED, "grantConfidentialityRequest", null},
            new Object[] {RequestOutcome.IN_PROGRESS, RequestOutcome.REFUSED, "grantConfidentialityRequest", null},
            new Object[] {RequestOutcome.IN_PROGRESS, RequestOutcome.IN_PROGRESS, "grantConfidentialityRequest", "grantConfidentialityRequest"},
            new Object[] {RequestOutcome.IN_PROGRESS, RequestOutcome.IN_PROGRESS, "grantConfidentialityRequest", "refuseConfidentialityRequest"},
            new Object[] {RequestOutcome.IN_PROGRESS, RequestOutcome.GRANTED, "refuseConfidentialityRequest", null},
            new Object[] {RequestOutcome.IN_PROGRESS, RequestOutcome.REFUSED, "refuseConfidentialityRequest", null},
            new Object[] {RequestOutcome.IN_PROGRESS, RequestOutcome.IN_PROGRESS, "refuseConfidentialityRequest", "grantConfidentialityRequest"},
            new Object[] {RequestOutcome.IN_PROGRESS, RequestOutcome.IN_PROGRESS, "refuseConfidentialityRequest", "refuseConfidentialityRequest"}
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

    @NamedParameters("validRequestReviewValueCombinations")
    @SuppressWarnings("unused")
    private Object[] validRequestReviewValueCombinations() {
        return new Object[] {
            new String[] {"grantConfidentialityRequest", "grantConfidentialityRequest"},
            new String[] {"refuseConfidentialityRequest", "refuseConfidentialityRequest"},
            new String[] {"refuseConfidentialityRequest", "grantConfidentialityRequest"},
            new String[] {"refuseConfidentialityRequest", "refuseConfidentialityRequest"}
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

    private DatedRequestOutcome createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome outcome) {
        return outcome == null ? null : DatedRequestOutcome.builder().date(LocalDate.now().minusDays(1)).requestOutcome(outcome).build();
    }

    private DatedRequestOutcome createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome outcome) {
        return outcome == null ? null : DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(outcome).build();
    }

    @Parameters(named = "noConfidentialityRequestsAreInProgress")
    @Test
    public void givenNoConfidentialityRequestsAreInProgressShouldDisplayAnError(RequestOutcome appellantOutcome, RequestOutcome jointPartyOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfGrantedOrRefusedSetIncorrectly(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfGrantedSet(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());
        
        Assert.assertEquals(DwpState.CONFIDENTIALITY_ACTION_REQUIRED.getId(), sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals("Yes", sscsCaseData.getIsProgressingViaGaps());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Parameters(named = "noRequestIsInProgressAndPopulatedRequestReviewValues")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfAppellantGrantedSetAndJointPartyReviewValueSet(RequestOutcome jointPartyRequestOutcome, String jointPartyReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(jointPartyReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is not in progress but value set for granted or refused is:" + jointPartyReviewValue + ". Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfRefusedSet(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.NONE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Parameters(named = "populatedRequestReviewValues")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfAppellantRefusedSetAndJointPartyReviewValueSet(String jointPartyReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(jointPartyReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is not in progress but value set for granted or refused is:" + jointPartyReviewValue + ". Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());

        Assert.assertNull(sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfGrantedOrRefusedSetIncorrectly(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());

    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfGrantedSet(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals(DwpState.CONFIDENTIALITY_ACTION_REQUIRED.getId(), sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters(named = "noRequestIsInProgressAndPopulatedRequestReviewValues")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfJointPartyGrantedSetAndAppellantReviewValueSet(RequestOutcome appellantRequestOutcome, String appellantReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(appellantReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is not in progress but value set for granted or refused is:" + appellantReviewValue + ". Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }


    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfRefusedSet(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.NONE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Parameters(named = "populatedRequestReviewValues")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfAppellantRefusedSetAndAppellantReviewValueSet(String appellantReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(appellantReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is not in progress but value set for granted or refused is:" + appellantReviewValue + ". Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestAppellantGrantedOrRefused());
        Assert.assertNotNull(sscsCaseData.getConfidentialityRequestJointPartyGrantedOrRefused());

    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantGrantedAndJointPartyGranted() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("confidentialityActionRequired", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantGrantedAndJointPartyRefused() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("confidentialityActionRequired", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantRefusedAndJointPartyGranted() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("confidentialityActionRequired", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorIfAppellantRefusedAndJointPartyRefused() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome.REFUSED), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.NONE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Parameters(named = "populatedRequestReviewValues")
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfAppellantReviewValuePopulatedAndJointPartyReviewValueSetIncorrectly(String appellantReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused(appellantReviewValue);
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Joint Party confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyGrantedAndAppellantReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    @Parameters(named = "populatedRequestReviewValues")
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyReviewValuePopulatedAndAppellantReviewValueNotSet(String jointPartyReviewValue) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused(jointPartyReviewValue);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenBothRequestsInProgressShouldDisplayAnErrorIfJointPartyRefusedAndAppellantReviewValueSetIncorrectly() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("refuseConfidentialityRequest");
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("something");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:something. Please check case data. If problem continues please contact support", error);


        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void givenConfidentialityRequestAppellantGrantedAndStateIsResponseReviewed_thenSetClearDwpStateAndInterlocReviewState() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setState(State.RESPONSE_RECEIVED);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        assertNull(sscsCaseData.getDwpState());
        assertNull(sscsCaseData.getInterlocReviewState());
        assertEquals("Yes", sscsCaseData.getIsProgressingViaGaps());
    }

    @Test
    public void givenConfidentialityRequestJointPartyGrantedAndStateIsResponseReviewed_thenSetClearDwpStateAndInterlocReviewState() {

        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestJointPartyGrantedOrRefused("grantConfidentialityRequest");
        sscsCaseData.setState(State.RESPONSE_RECEIVED);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        assertNull(sscsCaseData.getDwpState());
        assertNull(sscsCaseData.getInterlocReviewState());
        assertEquals("Yes", sscsCaseData.getIsProgressingViaGaps());
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

    @Parameters(named = "allValidConfidentialityRequestCombinations")
    @Test
    public void givenConfidentialityRequestsCombinationShouldResetNotSetValuesToNull(RequestOutcome appellantOutcome, RequestOutcome jointPartyOutcome, String appellantReviewText, String jointPartyReviewText) {

        sscsCaseData.setJointParty("Yes");
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
            Assert.assertEquals(0, response.getErrors().size());
        } else {
            Assert.assertEquals(1, response.getErrors().size());
            String error = response.getErrors().stream().findFirst().orElse("");
            assertEquals("There is no confidentiality request to review. Please check case data. If problem continues please contact support", error);
        }

        if (!RequestOutcome.IN_PROGRESS.equals(appellantOutcome)) {
            Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        }
        if (!RequestOutcome.IN_PROGRESS.equals(jointPartyOutcome)) {
            Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        }
    }
}
