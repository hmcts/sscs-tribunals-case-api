package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpraiseexception;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.io.IOException;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;

@RunWith(JUnitParamsRunner.class)
public class DwpRaiseExceptionAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private DwpRaiseExceptionAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        handler = new DwpRaiseExceptionAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.DWP_RAISE_EXCEPTION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .appeal(Appeal.builder().build())
                .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonDwpRaiseExceptionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAHandleDwpLapseEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void setMoveToGapsFields() {
        sscsCaseData = sscsCaseData.toBuilder().state(State.WITH_DWP).build();
        assertIsProgressingToGaps();
    }

    @Test
    public void setMoveToGapsFieldsIfInterlocReviewStateIsEmptyString() {
        sscsCaseData = sscsCaseData.toBuilder().state(State.WITH_DWP).build();
        sscsCaseData.setInterlocReviewState(null);
        assertIsProgressingToGaps();
    }

    private void assertIsProgressingToGaps() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Yes", response.getData().getIsProgressingViaGaps());
    }

    @Test
    public void setInterlocReviewStateToNone() {
        sscsCaseData = sscsCaseData.toBuilder().state(State.WITH_DWP).build();
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Yes", response.getData().getIsProgressingViaGaps());
        assertEquals(InterlocReviewState.NONE, response.getData().getInterlocReviewState());
    }

}
