package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class ActionFurtherEvidenceDropdownHandlerTest {

    private ActionFurtherEvidenceDropdownHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new ActionFurtherEvidenceDropdownHandler();

        when(callback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAHandleEvidenceEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void populateFurtherEvidenceDropdown_whenCaseInInterloc() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).interlocReviewState("any").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback);

        assertEquals("issueFurtherEvidence", response.getData().getFurtherEvidenceAction().getListItems().get(0).getCode());
        assertEquals("otherDocumentManual", response.getData().getFurtherEvidenceAction().getListItems().get(1).getCode());
        assertEquals("informationReceivedForInterloc", response.getData().getFurtherEvidenceAction().getListItems().get(2).getCode());
        assertEquals(3, response.getData().getFurtherEvidenceAction().getListItems().size());
    }

    @Test
    public void populateFurtherEvidenceDropdown_whenCaseNotInInterloc() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback);

        assertEquals("issueFurtherEvidence", response.getData().getFurtherEvidenceAction().getListItems().get(0).getCode());
        assertEquals("otherDocumentManual", response.getData().getFurtherEvidenceAction().getListItems().get(1).getCode());
        assertEquals(2, response.getData().getFurtherEvidenceAction().getListItems().size());
    }

    @Test
    public void populateOriginalSenderDropdown_whenCaseHasRep() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().rep(Representative.builder().hasRepresentative("Yes").build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback);

        assertEquals("appellant", response.getData().getOriginalSender().getListItems().get(0).getCode());
        assertEquals("representative", response.getData().getOriginalSender().getListItems().get(1).getCode());
        assertEquals(2, response.getData().getOriginalSender().getListItems().size());
    }

    @Test
    public void populateOriginalSenderDropdown_whenCaseHasNoRep() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().rep(Representative.builder().hasRepresentative("No").build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback);

        assertEquals("appellant", response.getData().getOriginalSender().getListItems().get(0).getCode());
        assertEquals(1, response.getData().getOriginalSender().getListItems().size());
    }
}