package uk.gov.hmcts.reform.sscs.ccd.presubmit.playbackaudiovideo;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.time.LocalDate;
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
public class PlaybackAudioVideoEvidenceMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String DM_GW_URL = "http://gateway-ccd/documents";

    private static final String DOCUMENT_MANAGEMENT_URL = "http://dm-store/documents";

    private PlaybackAudioVideoEvidenceMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new PlaybackAudioVideoEvidenceMidEventHandler(DM_GW_URL, DOCUMENT_MANAGEMENT_URL);

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
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenSelectedAudioVideoEvidence_thenBuildAudioVideoEvidenceDetails() {
        sscsCaseData.setSscsDocument(Collections.singletonList(SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(DocumentType.AUDIO_DOCUMENT.getValue())
                .documentFileName("audioVideoFileName").documentDateAdded(LocalDate.now().toString()).partyUploaded(UploadParty.APPELLANT).documentLink(DocumentLink.builder().documentUrl(DOCUMENT_MANAGEMENT_URL + "/456").documentBinaryUrl(DOCUMENT_MANAGEMENT_URL + "/456/binary").documentUrl(DOCUMENT_MANAGEMENT_URL + "/456").build()).build()).build()));

        List<DynamicListItem> dynamicListItems = new ArrayList<>();
        dynamicListItems.add(new DynamicListItem(DOCUMENT_MANAGEMENT_URL + "/123", "value1"));
        DynamicListItem selectedDynamicListItem = new DynamicListItem(DOCUMENT_MANAGEMENT_URL + "/456", "value2");
        dynamicListItems.add(selectedDynamicListItem);
        dynamicListItems.add(new DynamicListItem(DOCUMENT_MANAGEMENT_URL + "/789", "value3"));

        sscsCaseData.setSelectedAudioVideoEvidence(new DynamicList(selectedDynamicListItem, dynamicListItems));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getData().getTempMediaUrl().contains(DM_GW_URL + "/456/binary"));

        AudioVideoEvidenceDetails expectedAudioVideoEvidenceDetails = AudioVideoEvidenceDetails.builder()
                .partyUploaded(UploadParty.APPELLANT)
                .documentType(DocumentType.AUDIO_DOCUMENT.getLabel())
                .dateAdded(LocalDate.now())
                .build();

        assertEquals(expectedAudioVideoEvidenceDetails, response.getData().getSelectedAudioVideoEvidenceDetails());
    }

}
