package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpanelcomposition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;

@RunWith(JUnitParamsRunner.class)
public class ConfirmPanelCompositionAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    ConfirmPanelCompositionAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        openMocks(this);

        handler = new ConfirmPanelCompositionAboutToSubmitHandler();

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.CONFIRM_PANEL_COMPOSITION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenFqpmRequiredNull_thenNoChange() {

        sscsCaseData.setIsFqpmRequired(null);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(InterlocReviewState.REVIEW_BY_JUDGE));
        assertThat(response.getData().getInterlocReferralReason(), is(InterlocReferralReason.NONE));
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenFqpmRequiredYesOrNoAndInterlocByJudge_thenClearInterloc(String isFqpmRequired) {

        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(nullValue()));
        assertThat(response.getData().getInterlocReferralReason(), is(nullValue()));
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenFqpmRequiredYesOrNoAndNoInterlocByJudge_thenInterlocNotChanged(String isFqpmRequired) {

        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(InterlocReviewState.REVIEW_BY_TCW));
        assertThat(response.getData().getInterlocReferralReason(), is(InterlocReferralReason.NONE));
    }

    @Test
    public void givenNoFqpmRequiredSet_thenInterlocNotChanged() {

        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(InterlocReviewState.REVIEW_BY_JUDGE));
        assertThat(response.getData().getInterlocReferralReason(), is(InterlocReferralReason.NONE));
    }
}
