package uk.gov.hmcts.reform.sscs.ccd.presubmit.playbackaudiovideo;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class PlaybackAudioVideoEvidenceAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private PlaybackAudioVideoEvidenceAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new PlaybackAudioVideoEvidenceAboutToStartHandler();

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.PLAYBACK_AUDIO_VIDEO_EVIDENCE);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonPlaybackAudioVideoEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenValidCallback_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenSscsDocumentsAndDwpDocumentsDoesNotContainAudioVideoEvidence_thenReturnError() {
        sscsCaseData.setSscsDocument(Collections.singletonList(SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(DocumentType.APPELLANT_EVIDENCE.getValue()).build()).build()));
        sscsCaseData.setDwpDocuments(Collections.singletonList(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DocumentType.DWP_EVIDENCE.getValue()).build()).build()));
        assertNoEvidenceError();
    }

    @Test
    public void givenSscsDocumentsAndDwpDocumentsAreEmpty_thenReturnError() {
        sscsCaseData.setSscsDocument(null);
        sscsCaseData.setDwpDocuments(null);
        assertNoEvidenceError();
    }

    private void assertNoEvidenceError() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("Before running this event audio and video evidence must be uploaded and approved", response.getErrors().toArray()[0]);
    }

    @Test
    @Parameters({"AUDIO_DOCUMENT", "VIDEO_DOCUMENT"})
    public void givenSscsDocumentsListContainsAudioVideoEvidence_thenSelectedAudioVideoEvidenceListIsCreated(DocumentType documentType) {
        sscsCaseData.setSscsDocument(Collections.singletonList(SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(documentType.getValue())
                .documentFileName("audioVideoFileName").documentLink(DocumentLink.builder().documentUrl("sscsAudioVideo.url").documentUrl("testAv.com").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(response.getData().getSelectedAudioVideoEvidence().getValue().getCode(), "testAv.com");
        assertEquals(response.getData().getSelectedAudioVideoEvidence().getValue().getLabel(), "audioVideoFileName");
    }

    @Test
    @Parameters({"AUDIO_DOCUMENT", "VIDEO_DOCUMENT"})
    public void givenDwpDocumentsListContainsAudioVideoEvidence_thenSelectedAudioVideoEvidenceListIsCreated(DocumentType documentType) {
        sscsCaseData.setDwpDocuments(Collections.singletonList(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(documentType.getValue())
                .documentFileName("audioVideoFileName").documentLink(DocumentLink.builder().documentUrl("dwpAudioVideo.url").documentUrl("testAv.com").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(response.getData().getSelectedAudioVideoEvidence().getValue().getCode(), "testAv.com");
        assertEquals(response.getData().getSelectedAudioVideoEvidence().getValue().getLabel(), "audioVideoFileName");
    }

    @Test
    @Parameters({"AUDIO_DOCUMENT", "VIDEO_DOCUMENT"})
    public void givenMultipleSscsDocumentsListAndMultipleDwpDocumentsListContainsAudioVideoEvidence_thenCombinedAudioVideoEvidenceListIsCreated(DocumentType documentType) {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        List<DwpDocument> dwpDocuments = new ArrayList<>();

        sscsDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(documentType.getValue())
                .documentFileName("audioVideoFileName1").documentLink(DocumentLink.builder().documentUrl("sscsAudioVideo.url").documentUrl("testAv1.com").build()).build()).build());

        sscsDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(documentType.getValue())
                .documentFileName("audioVideoFileName2").documentLink(DocumentLink.builder().documentUrl("sscsAudioVideo.url").documentUrl("testAv2.com").build()).build()).build());

        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(documentType.getValue())
                .documentFileName("audioVideoFileName3").documentLink(DocumentLink.builder().documentUrl("dwpAudioVideo.url").documentUrl("testAv3.com").build()).build()).build());

        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(documentType.getValue())
                .documentFileName("audioVideoFileName4").documentLink(DocumentLink.builder().documentUrl("dwpAudioVideo.url").documentUrl("testAv4.com").build()).build()).build());

        sscsCaseData.setSscsDocument(sscsDocuments);
        sscsCaseData.setDwpDocuments(dwpDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(response.getData().getSelectedAudioVideoEvidence().getValue().getCode(), "testAv1.com");
        assertEquals(response.getData().getSelectedAudioVideoEvidence().getValue().getLabel(), "audioVideoFileName1");

        assertEquals(response.getData().getSelectedAudioVideoEvidence().getListItems().size(), 4);
        assertEquals(response.getData().getSelectedAudioVideoEvidence().getListItems().get(0).getCode(), "testAv1.com");
        assertEquals(response.getData().getSelectedAudioVideoEvidence().getListItems().get(0).getLabel(), "audioVideoFileName1");
        assertEquals(response.getData().getSelectedAudioVideoEvidence().getListItems().get(1).getCode(), "testAv2.com");
        assertEquals(response.getData().getSelectedAudioVideoEvidence().getListItems().get(1).getLabel(), "audioVideoFileName2");
        assertEquals(response.getData().getSelectedAudioVideoEvidence().getListItems().get(2).getCode(), "testAv3.com");
        assertEquals(response.getData().getSelectedAudioVideoEvidence().getListItems().get(2).getLabel(), "audioVideoFileName3");
        assertEquals(response.getData().getSelectedAudioVideoEvidence().getListItems().get(3).getCode(), "testAv4.com");
        assertEquals(response.getData().getSelectedAudioVideoEvidence().getListItems().get(3).getLabel(), "audioVideoFileName4");

    }
}
