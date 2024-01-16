package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityrequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class ReviewConfidentialityRequestAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private ReviewConfidentialityRequestAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp()  {
        openMocks(this);
        handler = new ReviewConfidentialityRequestAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.REVIEW_CONFIDENTIALITY_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .dwpState(DwpState.FE_ACTIONED_NR)
            .interlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE)
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

    private DatedRequestOutcome createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome outcome) {
        return outcome == null ? null :
            DatedRequestOutcome.builder().date(LocalDate.now().minusDays(1)).requestOutcome(outcome).build();
    }

    @Parameters(named = "noConfidentialityRequestsAreInProgress")
    @Test
    public void givenNoConfidentialityRequestsAreInProgressShouldDisplayAnErrorWhenJointPartyWhenJointPartyExists(RequestOutcome appellantOutcome, RequestOutcome jointPartyOutcome) {

        sscsCaseData.getJointParty().setHasJointParty(YES);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review", error);

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorWhenJointPartyExists(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.getJointParty().setHasJointParty(YES);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }


    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorWhenJointPartyExists(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.getJointParty().setHasJointParty(YES);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorWhenJointPartyExists() {

        sscsCaseData.getJointParty().setHasJointParty(YES);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }


    @Parameters(named = "noConfidentialityRequestsAreInProgress")
    @Test
    public void givenNoConfidentialityRequestsAreInProgressShouldDisplayAnErrorWhenJointPartyWhenJointPartyDoesNotExist(RequestOutcome appellantOutcome, RequestOutcome jointPartyOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review", error);

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }

    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorWhenJointPartyDoesNotExist(RequestOutcome jointPartyRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review", error);

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }


    @Parameters(named = "noRequestIsInProgress")
    @Test
    public void givenJointPartyConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorWhenJointPartyDoesNotExists(RequestOutcome appellantRequestOutcome) {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review", error);

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantRequestOutcome), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());

    }

    @Test
    public void givenBothRequestsInProgressShouldNotDisplayAnErrorWhenJointPartyDoesNotExist() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no confidentiality request to review", error);

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome.IN_PROGRESS), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }


    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheRequest() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
