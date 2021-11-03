package uk.gov.hmcts.reform.sscs.ccd.presubmit.attachscanneddocs;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class AttachScannedDocsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private AttachScannedDocsAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new AttachScannedDocsAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.ATTACH_SCANNED_DOCS);
        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonAttachScannedDocsEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenAnAttachScannedDocsEvent_thenSetEvidenceAndAvFlagsToNo() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.NO.getValue(), response.getData().getEvidenceHandled());
        assertEquals(YesNo.NO, response.getData().getHasUnprocessedAudioVideoEvidence());
    }

    @Test
    public void givenAnAttachScannedDocsEventAndAvEvidenceAlreadyOnCase_thenSetEvidenceFlagToNoAndAvFlagToYes() {
        sscsCaseData.setAudioVideoEvidence(Collections.singletonList(AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder().build()).build()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(YesNo.NO.getValue(), response.getData().getEvidenceHandled());
        assertEquals(YesNo.YES, response.getData().getHasUnprocessedAudioVideoEvidence());
    }
}