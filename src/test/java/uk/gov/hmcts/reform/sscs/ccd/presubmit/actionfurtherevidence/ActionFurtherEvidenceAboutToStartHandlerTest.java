package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class ActionFurtherEvidenceAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private ActionFurtherEvidenceAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new ActionFurtherEvidenceAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        ReflectionTestUtils.setField(handler, "issueFurtherEvidenceFeature", false);
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
    public void populateFurtherEvidenceDropdownWithIssueFurtherEvidenceFeatureFlagTrue_whenCaseInInterloc() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).interlocReviewState("any").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        ReflectionTestUtils.setField(handler, "issueFurtherEvidenceFeature", true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("issueFurtherEvidence", response.getData().getFurtherEvidenceAction().getListItems().get(0).getCode());
        assertEquals("otherDocumentManual", response.getData().getFurtherEvidenceAction().getListItems().get(1).getCode());
        assertEquals("informationReceivedForInterloc", response.getData().getFurtherEvidenceAction().getListItems().get(2).getCode());
        assertEquals(3, response.getData().getFurtherEvidenceAction().getListItems().size());
    }

    @Test
    public void populateFurtherEvidenceDropdownWithIssueFurtherEvidenceFeatureFlagFalse_whenCaseInInterloc() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).interlocReviewState("any").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        ReflectionTestUtils.setField(handler, "issueFurtherEvidenceFeature", false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("otherDocumentManual", response.getData().getFurtherEvidenceAction().getListItems().get(0).getCode());
        assertEquals("informationReceivedForInterloc", response.getData().getFurtherEvidenceAction().getListItems().get(1).getCode());
        assertEquals(2, response.getData().getFurtherEvidenceAction().getListItems().size());
    }

    @Test
    public void populateFurtherEvidenceDropdownWithIssueFurtherEvidenceFeatureFlagTrue_whenCaseNotInInterloc() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        ReflectionTestUtils.setField(handler, "issueFurtherEvidenceFeature", true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("issueFurtherEvidence", response.getData().getFurtherEvidenceAction().getListItems().get(0).getCode());
        assertEquals("otherDocumentManual", response.getData().getFurtherEvidenceAction().getListItems().get(1).getCode());
        assertEquals(2, response.getData().getFurtherEvidenceAction().getListItems().size());
    }

    @Test
    public void populateFurtherEvidenceDropdownWithIssueFurtherEvidenceFeatureFlagFalse_whenCaseNotInInterloc() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        ReflectionTestUtils.setField(handler, "issueFurtherEvidenceFeature", false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("otherDocumentManual", response.getData().getFurtherEvidenceAction().getListItems().get(0).getCode());
        assertEquals(1, response.getData().getFurtherEvidenceAction().getListItems().size());
    }

    @Test
    public void populateOriginalSenderDropdown_whenCaseHasRep() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().rep(Representative.builder().hasRepresentative("Yes").build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("appellant", response.getData().getOriginalSender().getListItems().get(0).getCode());
        assertEquals("dwp", response.getData().getOriginalSender().getListItems().get(1).getCode());
        assertEquals("representative", response.getData().getOriginalSender().getListItems().get(2).getCode());
        assertEquals(3, response.getData().getOriginalSender().getListItems().size());
    }

    @Test
    public void populateOriginalSenderDropdown_whenCaseHasNoRep() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().rep(Representative.builder().hasRepresentative("No").build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("appellant", response.getData().getOriginalSender().getListItems().get(0).getCode());
        assertEquals("dwp", response.getData().getOriginalSender().getListItems().get(1).getCode());
        assertEquals(2, response.getData().getOriginalSender().getListItems().size());
    }

    @Test
    public void populateOriginalSenderDropdown_whenCaseHasRepIsNull() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(null).build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals("appellant", response.getData().getOriginalSender().getListItems().get(0).getCode());
        assertEquals("dwp", response.getData().getOriginalSender().getListItems().get(1).getCode());
        assertEquals(2, response.getData().getOriginalSender().getListItems().size());
    }
}