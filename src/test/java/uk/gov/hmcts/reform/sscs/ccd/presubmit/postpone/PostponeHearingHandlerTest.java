package uk.gov.hmcts.reform.sscs.ccd.presubmit.postpone;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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


@RunWith(JUnitParamsRunner.class)
public class PostponeHearingHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private PostponeHearingHandler handler;
    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new PostponeHearingHandler();

        when(callback.getEvent()).thenReturn(EventType.POSTPONED);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .createdInGapsFrom(State.READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandlePostponeEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"POSTPONED"})
    public void givenAHandlePostponeEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void setDwpStateToPostpone() {
        sscsCaseData = sscsCaseData.toBuilder().createdInGapsFrom(State.VALID_APPEAL.getId()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("hearingPostponed", response.getData().getDwpState());
    }
}
