package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;


@RunWith(JUnitParamsRunner.class)
public class ActionPostponementRequestAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    ActionPostponementRequestAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private UserDetailsService userDetailsService;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ActionPostponementRequestAboutToSubmitHandler(userDetailsService);

        when(callback.getEvent()).thenReturn(EventType.PROCESS_HEARING_RECORDING_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAGrantedFromRequestedDwpHearingRecording_thenRemoveFromRequestedListAndAddToReleasedListAndDwpStatusReleased() {
        sscsCaseData.setPostponementRequest(PostponementRequest.builder().actionPostponementRequestSelected("sendToJudge").postponementRequestDetails("Request Detail Test").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(InterlocReviewState.REVIEW_BY_JUDGE.getId()));
        assertThat(response.getData().getInterlocReferralReason(), is(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST
                .getId()));
        assertThat(response.getData().getPostponementRequest().getUnprocessedPostponementRequest(), is(YesNo.YES));
        assertThat(response.getData().getAppealNotePad().getNotesCollection().stream()
                .anyMatch(note -> note.getValue().getNoteDetail().equals(ActionPostponementRequestAboutToSubmitHandler
                        .POSTPONEMENT_DETAILS_SENT_TO_JUDGE_PREFIX + "Request Detail Test")), is(true));
    }
}