package uk.gov.hmcts.reform.sscs.ccd.presubmit.struckout;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_INFORMATION;

import java.time.LocalDate;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class StruckOutAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private StruckOutAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new StruckOutAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.STRUCK_OUT);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .interlocReviewState(AWAITING_INFORMATION.getId())
                .directionDueDate(LocalDate.now().toString())
                .dwpState(DwpState.IN_PROGRESS.getId())
                .state(INTERLOCUTORY_REVIEW_STATE)
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetails));
        when(caseDetails.getState()).thenReturn(INTERLOCUTORY_REVIEW_STATE);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonHandleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenStruckOutEvent_thenDwpStateToStrikeOutActioned() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(nullValue()));
        assertThat(response.getData().getDirectionDueDate(), is(nullValue()));
        assertThat(response.getData().getPreviousState(), is(INTERLOCUTORY_REVIEW_STATE));
        assertThat(response.getData().getDwpState(), is(DwpState.STRIKE_OUT_ACTIONED.getId()));
    }
}
