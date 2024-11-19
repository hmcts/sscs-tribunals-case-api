package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwplapse;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

@RunWith(JUnitParamsRunner.class)
public class DwpLapseCaseMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private DwpLapseCaseMidEventHandler handler;
    private SscsCaseData sscsCaseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new DwpLapseCaseMidEventHandler();

        when(callback.getEvent()).thenReturn(EventType.DWP_LAPSE_CASE);

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
    public void givenANonHandleDwpLapseEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenAHandleDwpLapseAboutToSubmitEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.DWP_LAPSE_CASE);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAHandleDwpLapseEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.DWP_LAPSE_CASE);
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenNullCallback_thenThrowError() {
        when(callback.getEvent()).thenReturn(EventType.DWP_LAPSE_CASE);
        assertThrows(NullPointerException.class, () -> handler.canHandle(ABOUT_TO_SUBMIT, null));
        assertThrows(NullPointerException.class, () -> handler.canHandle(null, callback));
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleDwpLapseEvent_thenThrowError(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertThrows(IllegalStateException.class, () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));
    }

    @Test
    public void givenAHandleDwpLapseEvent_thenThrowError() {
        when(callback.getEvent()).thenReturn(EventType.DWP_LAPSE_CASE);
        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    public void passIbaCaseIfInterlocReviewByJudge() {
        when(callback.getEvent()).thenReturn(EventType.DWP_LAPSE_CASE);
        sscsCaseData.setBenefitCode("093");
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    @Parameters({"AWAITING_ADMIN_ACTION", "AWAITING_INFORMATION", "NONE", "REVIEW_BY_TCW", "WELSH_TRANSLATION"})
    public void failIbaCaseIfNotInterlocReviewByJudge(InterlocReviewState interlocReviewState) {
        when(callback.getEvent()).thenReturn(EventType.DWP_LAPSE_CASE);
        sscsCaseData.setBenefitCode("093");
        sscsCaseData.setInterlocReviewState(interlocReviewState);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains("Interlocutory review state must be set to 'Review by Judge'"));
    }

    @Test
    public void passNonIbaCaseIfNonNullLt203() {
        when(callback.getEvent()).thenReturn(EventType.DWP_LAPSE_CASE);
        sscsCaseData.setBenefitCode("001");
        sscsCaseData.setDwpLT203(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("lt203Link").build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void failNonIbaCaseIfNullLt203() {
        when(callback.getEvent()).thenReturn(EventType.DWP_LAPSE_CASE);
        sscsCaseData.setBenefitCode("001");
        sscsCaseData.setDwpLT203(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains("Select or fill the required Select document for upload field"));
    }
}
