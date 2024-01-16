package uk.gov.hmcts.reform.sscs.ccd.presubmit.informationreceived;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INTERLOC_INFORMATION_RECEIVED;

import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@RunWith(JUnitParamsRunner.class)
public class InformationReceivedAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private InformationReceivedAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private UserDetailsService userDetailsService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        AddNoteService addNoteService = new AddNoteService(userDetailsService);
        handler = new InformationReceivedAboutToSubmitHandler(addNoteService);
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(UserDetails.builder()
                .forename("Chris").surname("Davis").build().getFullName());
        when(callback.getEvent()).thenReturn(INTERLOC_INFORMATION_RECEIVED);
        SscsCaseData sscsCaseData = SscsCaseData.builder().interlocReviewState(InterlocReviewState.REVIEW_BY_TCW).interlocReferralReason(InterlocReferralReason.OVER_300_PAGES).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenCanHandleIsCalled_shouldReturnCorrectResult() {
        boolean actualResult = handler.canHandle(ABOUT_TO_SUBMIT, callback);
        assertTrue(actualResult);
    }

    @Test
    public void givenReviewByTcwState_addRelevantNote_withoutTempNote() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now()));
        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Referred to interloc for review by TCW – Over 300 pages",
                response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
    }

    @Test
    public void givenReviewByJudgeState_addRelevantNote_withTempNote() {
        callback.getCaseDetails().getCaseData().setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        callback.getCaseDetails().getCaseData().setTempNoteDetail("new note to add");
        callback.getCaseDetails().getCaseData().setInterlocReferralReason(InterlocReferralReason.COMPLEX_CASE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now()));
        assertEquals(1, response.getData().getAppealNotePad().getNotesCollection().size());
        assertEquals("Referred to interloc for review by judge – Complex Case - new note to add",
                response.getData().getAppealNotePad().getNotesCollection().get(0).getValue().getNoteDetail());
    }
}
