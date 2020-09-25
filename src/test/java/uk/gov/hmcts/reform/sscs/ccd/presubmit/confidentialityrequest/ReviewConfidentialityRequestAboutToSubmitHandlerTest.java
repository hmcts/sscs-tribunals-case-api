package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentialityrequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.io.IOException;
import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
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

    @Test
    public void givenNoConfidentialityRequestsAreInProgressShouldDisplayAnError() {

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("At least one confidentiality request should be in progress. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertNull(sscsCaseData.getConfidentialityRequestDate());

    }

    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfGrantedOrRefusedNotSet() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(1, response.getErrors().size());
        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Appellant confidentiality request is in progress but value set for granted or refused is:null. Please check case data. If problem continues please contact support", error);

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.REVIEW_BY_JUDGE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now().minusDays(1), sscsCaseData.getConfidentialityRequestDate());

    }

    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldDisplayAnErrorIfGrantedOrRefusedSetIncorrectly() {


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

    }

    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfGrantedSet() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("grantConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());
        
        Assert.assertEquals(DwpState.CONFIDENTIALITY_ACTION_REQUIRED.getId(), sscsCaseData.getDwpState());
        Assert.assertEquals(InterlocReviewState.AWAITING_ADMIN_ACTION.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(RequestOutcome.GRANTED, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestDate());
    }

    @Test
    public void givenAppellantConfidentialityRequestOnlyIsInProgressShouldNotDisplayAnErrorIfRefusedSet() {

        sscsCaseData.setConfidentialityRequestOutcomeAppellant(RequestOutcome.IN_PROGRESS);
        sscsCaseData.setConfidentialityRequestDate(LocalDate.now().minusDays(1));
        sscsCaseData.setConfidentialityRequestAppellantGrantedOrRefused("refuseConfidentialityRequest");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        Assert.assertEquals(0, response.getErrors().size());

        Assert.assertEquals("previousDwpState", sscsCaseData.getDwpState());
        Assert.assertEquals(RequestOutcome.REFUSED, sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        Assert.assertEquals(InterlocReviewState.NONE.getId(), sscsCaseData.getInterlocReviewState());
        Assert.assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestDate());
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
