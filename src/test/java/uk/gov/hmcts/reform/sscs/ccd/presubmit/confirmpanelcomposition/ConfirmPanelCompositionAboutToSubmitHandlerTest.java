package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpanelcomposition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;

import java.time.LocalDate;

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

        when(callback.getEvent()).thenReturn(EventType.CONFIRM_PANEL_COMPOSITION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .directionNoticeContent("Body Content")
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().build()).build();
        sscsCaseData.setAppeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenFqpmRequiredYesOrNoAndInterlocByJudge_thenClearInterloc(String isFqpmRequired) {

        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE.getId());
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE.getId());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(nullValue()));
        assertThat(response.getData().getInterlocReferralReason(), is(nullValue()));
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenFqpmRequiredYesOrNoAndNoInterlocByJudge_thenInterlocNotChanged(String isFqpmRequired) {

        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW.getId());
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE.getId());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(InterlocReviewState.REVIEW_BY_TCW.getId()));
        assertThat(response.getData().getInterlocReferralReason(), is(InterlocReferralReason.NONE.getId()));
    }

    @Test
    public void givenNoFqpmRequiredSet_thenInterlocNotChanged() {

        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE.getId());
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE.getId());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(InterlocReviewState.REVIEW_BY_JUDGE.getId()));
        assertThat(response.getData().getInterlocReferralReason(), is(InterlocReferralReason.NONE.getId()));
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenFqpmRequiredYesOrNoAndNoDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {

        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
        sscsCaseData.setDwpDueDate(null);
        //TODO: set at least one hearing options

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.READY_TO_LIST));
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenFqpmRequiredYesOrNoAndNoDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {

        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
        sscsCaseData.setDwpDueDate(null);
        //TODO: set no hearing options

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.READY_TO_LIST));
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenFqpmRequiredYesOrNoAndDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsNotListable(String isFqpmRequired) {

        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
        sscsCaseData.setDwpDueDate(LocalDate.now().toString());
        //TODO: set no hearing options

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.NOT_LISTABLE));
    }
}