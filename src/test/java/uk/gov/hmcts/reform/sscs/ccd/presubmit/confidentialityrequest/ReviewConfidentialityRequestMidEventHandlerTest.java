package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityrequest;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;

@RunWith(JUnitParamsRunner.class)
public class ReviewConfidentialityRequestMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private ReviewConfidentialityRequestMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        handler = new ReviewConfidentialityRequestMidEventHandler();

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

    @NamedParameters("allConfidentialityRequestCombinations")
    @SuppressWarnings("unused")
    private Object[] allConfidentialityRequestCombinations() {
        return new Object[] {
            new RequestOutcome[] {null, null},
            new RequestOutcome[] {null, RequestOutcome.NOT_SET},
            new RequestOutcome[] {null, RequestOutcome.GRANTED},
            new RequestOutcome[] {null, RequestOutcome.REFUSED},
            new RequestOutcome[] {null, RequestOutcome.IN_PROGRESS},
            new RequestOutcome[] {RequestOutcome.GRANTED, null},
            new RequestOutcome[] {RequestOutcome.GRANTED, RequestOutcome.NOT_SET},
            new RequestOutcome[] {RequestOutcome.GRANTED, RequestOutcome.GRANTED},
            new RequestOutcome[] {RequestOutcome.GRANTED, RequestOutcome.REFUSED},
            new RequestOutcome[] {RequestOutcome.GRANTED, RequestOutcome.IN_PROGRESS},
            new RequestOutcome[] {RequestOutcome.REFUSED, null},
            new RequestOutcome[] {RequestOutcome.REFUSED, RequestOutcome.NOT_SET},
            new RequestOutcome[] {RequestOutcome.REFUSED, RequestOutcome.GRANTED},
            new RequestOutcome[] {RequestOutcome.REFUSED, RequestOutcome.REFUSED},
            new RequestOutcome[] {RequestOutcome.REFUSED, RequestOutcome.IN_PROGRESS},
            new RequestOutcome[] {RequestOutcome.NOT_SET, null},
            new RequestOutcome[] {RequestOutcome.NOT_SET, RequestOutcome.NOT_SET},
            new RequestOutcome[] {RequestOutcome.NOT_SET, RequestOutcome.GRANTED},
            new RequestOutcome[] {RequestOutcome.NOT_SET, RequestOutcome.REFUSED},
            new RequestOutcome[] {RequestOutcome.NOT_SET, RequestOutcome.IN_PROGRESS},
            new RequestOutcome[] {RequestOutcome.IN_PROGRESS, null},
            new RequestOutcome[] {RequestOutcome.IN_PROGRESS, RequestOutcome.NOT_SET},
            new RequestOutcome[] {RequestOutcome.IN_PROGRESS, RequestOutcome.GRANTED},
            new RequestOutcome[] {RequestOutcome.IN_PROGRESS, RequestOutcome.REFUSED},
            new RequestOutcome[] {RequestOutcome.IN_PROGRESS, RequestOutcome.IN_PROGRESS}
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

    private DatedRequestOutcome createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(RequestOutcome outcome, boolean treatNotSetAsNull) {
        return outcome == null || (treatNotSetAsNull && RequestOutcome.NOT_SET.equals(outcome)) ? null : DatedRequestOutcome.builder().date(LocalDate.now().minusDays(1)).requestOutcome(outcome).build();
    }

    private DatedRequestOutcome createDatedOutcomeForTodaysDateIfOutcomeIsPopulated(RequestOutcome outcome, boolean treatNotSetAsNull) {
        return outcome == null || (treatNotSetAsNull && RequestOutcome.NOT_SET.equals(outcome)) ? null : DatedRequestOutcome.builder().date(LocalDate.now()).requestOutcome(outcome).build();
    }

    @Parameters(named = "allConfidentialityRequestCombinations")
    @Test
    public void givenConfidentialityRequestsCombinationShouldResetNotSetValuesToNull(RequestOutcome appellantOutcome, RequestOutcome jointPartyOutcome) {

        sscsCaseData.setJointParty("Yes");
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome, false));
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome, false));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(appellantOutcome,  true), sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(createDatedOutcomeForPreviousDateIfOutcomeIsPopulated(jointPartyOutcome,  true), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "ABOUT_TO_START", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheRequest() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
