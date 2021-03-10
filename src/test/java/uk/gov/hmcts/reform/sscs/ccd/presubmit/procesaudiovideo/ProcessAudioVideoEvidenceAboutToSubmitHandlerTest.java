package uk.gov.hmcts.reform.sscs.ccd.presubmit.procesaudiovideo;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoEvidenceAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@RunWith(JUnitParamsRunner.class)
public class ProcessAudioVideoEvidenceAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String DOCUMENT_URL = "dm-store/documents/123";


    private static final String URL = "http://dm-store/documents/123";

    private ProcessAudioVideoEvidenceAboutToSubmitHandler handler;

    @Spy
    private DocumentConfiguration documentConfiguration;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    private ArgumentCaptor<GenerateFileParams> capture;

    @Mock
    private FooterService footerService;

    private SscsDocument expectedDocument;

    @Before
    public void setUp() {
        openMocks(this);

        Map<EventType, String> englishEventTypeDocs = new HashMap<>();
        englishEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-ENG-00091.docx");

        Map<LanguagePreference, Map<EventType, String>> documents = new HashMap<>();
        documents.put(LanguagePreference.ENGLISH, englishEventTypeDocs);

        documentConfiguration.setDocuments(documents);
        handler = new ProcessAudioVideoEvidenceAboutToSubmitHandler(footerService);

        sscsCaseData = SscsCaseData.builder()
                .signedBy("User")
                .processAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.ISSUE_DIRECTIONS_NOTICE.getCode()))
                .signedRole("Judge")
                .dateAdded(LocalDate.now().minusDays(1))
                .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
                .previewDocument(DocumentLink.builder()
                        .documentUrl(DOCUMENT_URL)
                        .documentBinaryUrl(DOCUMENT_URL + "/binary")
                        .documentFilename("directionIssued.pdf")
                        .build())
                .interlocReviewState(InterlocReviewState.REVIEW_BY_TCW.getId())
                .audioVideoEvidence(singletonList(AudioVideoEvidence.builder().value(
                        AudioVideoEvidenceDetails.builder()
                                .documentLink(DocumentLink.builder().documentFilename("music.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                                .fileName("music.mp3")
                                .partyUploaded(UploadParty.APPELLANT)
                                .dateAdded(LocalDate.now())
                            .build())
                        .build()))
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("APPELLANT")
                                        .lastName("LastNamE")
                                        .build())
                                .identity(Identity.builder().build())
                                .build())
                        .build()).build();

        expectedDocument = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentFileName(sscsCaseData.getPreviewDocument().getDocumentFilename())
                        .documentLink(sscsCaseData.getPreviewDocument())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                        .build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.PROCESS_AUDIO_VIDEO);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenAValidHandleAndEventType_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void shouldShowError_whenThereIsNoActionSelected() {
        sscsCaseData.setProcessAudioVideoAction(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Select an action to process the audio/video evidence"));
    }

    @Test
    public void shouldShowError_whenThereIsNoPreviewDocument() {
        sscsCaseData.setPreviewDocument(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("There is no document notice"));
    }

    @Test
    public void givenIncludeEvidenceFromAppellant_willClearAudioVideoEvidenceAndInterlocReviewStateAndAddToSscsDocumentsCollection() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.INCLUDE_EVIDENCE.getCode()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getPreviewDocument());
        assertNull(response.getData().getSignedBy());
        assertNull(response.getData().getSignedRole());
        assertNull(response.getData().getGenerateNotice());
        assertNull(response.getData().getDateAdded());
        assertNull(response.getData().getReservedToJudge());

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(DocumentType.DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertNull(response.getData().getInterlocReviewState());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
        assertNull(response.getData().getAudioVideoEvidence());
        assertEquals(1, response.getData().getSscsDocument().size());
        assertEquals(DocumentLink.builder().documentFilename("music.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build(), response.getData().getSscsDocument().get(0).getValue().getDocumentLink());
        assertEquals(LocalDate.now().toString(), response.getData().getSscsDocument().get(0).getValue().getDateApproved());
        assertEquals(LocalDate.now().toString(), response.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("music.mp3", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("audioDocument", response.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals("Appellant", response.getData().getSscsDocument().get(0).getValue().getPartyUploaded().getLabel());
    }

    @Test
    public void givenIncludeEvidenceFromAppellantWithExistingSscsDocuments_willClearAudioVideoEvidenceAndInterlocReviewStateAndAddToSscsDocumentsCollection() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.INCLUDE_EVIDENCE.getCode()));

        List<SscsDocument> sscsDocuments = singletonList(SscsDocument.builder().value(
                SscsDocumentDetails.builder()
                        .documentLink(DocumentLink.builder().documentFilename("existing.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                        .documentFileName("existing.mp3")
                        .partyUploaded(UploadParty.DWP)
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .build())
                .build());
        sscsCaseData.setSscsDocument(sscsDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getPreviewDocument());
        assertNull(response.getData().getSignedBy());
        assertNull(response.getData().getSignedRole());
        assertNull(response.getData().getGenerateNotice());
        assertNull(response.getData().getDateAdded());
        assertNull(response.getData().getReservedToJudge());

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(DocumentType.DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertNull(response.getData().getInterlocReviewState());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
        assertNull(response.getData().getAudioVideoEvidence());
        assertEquals(2, response.getData().getSscsDocument().size());
        assertEquals(DocumentLink.builder().documentFilename("music.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build(), response.getData().getSscsDocument().get(0).getValue().getDocumentLink());
        assertEquals(LocalDate.now().toString(), response.getData().getSscsDocument().get(0).getValue().getDateApproved());
        assertEquals(LocalDate.now().toString(), response.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("music.mp3", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("audioDocument", response.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals("Appellant", response.getData().getSscsDocument().get(0).getValue().getPartyUploaded().getLabel());
    }

    @Test
    public void givenIncludeEvidenceIsNotAnMp3OrMp4_thenDisplayError() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.INCLUDE_EVIDENCE.getCode()));

        List<AudioVideoEvidence> videoList = singletonList(AudioVideoEvidence.builder().value(
                AudioVideoEvidenceDetails.builder()
                        .documentLink(DocumentLink.builder().documentFilename("nonvideo.pdf").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                        .fileName("nonvideo.pdf")
                        .partyUploaded(UploadParty.DWP)
                        .dateAdded(LocalDate.now())
                        .build())
                .build());

        sscsCaseData.setAudioVideoEvidence(videoList);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("Evidence cannot be included as it is not in .mp3 or .mp4 format", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenIncludeEvidenceFromDwp_willClearAudioVideoEvidenceAndInterlocReviewStateAndAddToDwpDocumentsCollection() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.INCLUDE_EVIDENCE.getCode()));

        List<AudioVideoEvidence> videoList = singletonList(AudioVideoEvidence.builder().value(
                AudioVideoEvidenceDetails.builder()
                        .documentLink(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                        .fileName("video.mp4")
                        .partyUploaded(UploadParty.DWP)
                        .dateAdded(LocalDate.now())
                        .build())
                .build());

        sscsCaseData.setAudioVideoEvidence(videoList);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getPreviewDocument());
        assertNull(response.getData().getSignedBy());
        assertNull(response.getData().getSignedRole());
        assertNull(response.getData().getGenerateNotice());
        assertNull(response.getData().getDateAdded());
        assertNull(response.getData().getReservedToJudge());

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(DocumentType.DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertNull(response.getData().getInterlocReviewState());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
        assertNull(response.getData().getAudioVideoEvidence());
        assertEquals(1, response.getData().getDwpDocuments().size());
        assertEquals(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build(), response.getData().getDwpDocuments().get(0).getValue().getDocumentLink());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpDocuments().get(0).getValue().getDateApproved());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpDocuments().get(0).getValue().getDocumentDateAdded());
        assertEquals("video.mp4", response.getData().getDwpDocuments().get(0).getValue().getDocumentFileName());
        assertEquals("videoDocument", response.getData().getDwpDocuments().get(0).getValue().getDocumentType());
        assertEquals("DWP", response.getData().getDwpDocuments().get(0).getValue().getPartyUploaded().getLabel());
    }

    @Test
    public void givenIncludeEvidenceFromDwpWithRip1Document_willClearAudioVideoEvidenceAndInterlocReviewStateAndAddToDwpDocumentsCollection() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.INCLUDE_EVIDENCE.getCode()));

        List<AudioVideoEvidence> videoList = singletonList(AudioVideoEvidence.builder().value(
                AudioVideoEvidenceDetails.builder()
                        .documentLink(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                        .fileName("video.mp4")
                        .partyUploaded(UploadParty.DWP)
                        .dateAdded(LocalDate.now())
                        .rip1Document(DocumentLink.builder().documentFilename("rip1.pdf").documentUrl("rip1.com").documentBinaryUrl("rip1.com/binary").build())
                        .build())
                .build());

        sscsCaseData.setAudioVideoEvidence(videoList);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getPreviewDocument());
        assertNull(response.getData().getSignedBy());
        assertNull(response.getData().getSignedRole());
        assertNull(response.getData().getGenerateNotice());
        assertNull(response.getData().getDateAdded());
        assertNull(response.getData().getReservedToJudge());

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(DocumentType.DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertNull(response.getData().getInterlocReviewState());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
        assertNull(response.getData().getAudioVideoEvidence());
        assertEquals(1, response.getData().getDwpDocuments().size());
        assertEquals(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build(), response.getData().getDwpDocuments().get(0).getValue().getDocumentLink());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpDocuments().get(0).getValue().getDateApproved());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpDocuments().get(0).getValue().getDocumentDateAdded());
        assertEquals("video.mp4", response.getData().getDwpDocuments().get(0).getValue().getDocumentFileName());
        assertEquals("videoDocument", response.getData().getDwpDocuments().get(0).getValue().getDocumentType());
        assertEquals("DWP", response.getData().getDwpDocuments().get(0).getValue().getPartyUploaded().getLabel());
        assertEquals(DocumentLink.builder().documentFilename("RIP 1 document uploaded on " + LocalDate.now().toString() + ".pdf").documentUrl("rip1.com").documentBinaryUrl("rip1.com/binary").build(), response.getData().getDwpDocuments().get(0).getValue().getRip1DocumentLink());
    }

    @Test
    public void givenIncludeEvidenceFromDwpWithExistingDwpDocuments_willClearAudioVideoEvidenceAndInterlocReviewStateAndAddToDwpDocumentsCollection() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.INCLUDE_EVIDENCE.getCode()));

        List<AudioVideoEvidence> videoList = singletonList(AudioVideoEvidence.builder().value(
                AudioVideoEvidenceDetails.builder()
                        .documentLink(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                        .fileName("video.mp4")
                        .partyUploaded(UploadParty.DWP)
                        .dateAdded(LocalDate.now())
                        .build())
                .build());

        sscsCaseData.setAudioVideoEvidence(videoList);

        List<DwpDocument> dwpDocuments = singletonList(DwpDocument.builder().value(
                DwpDocumentDetails.builder()
                        .documentLink(DocumentLink.builder().documentFilename("existing.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build())
                        .documentFileName("existing.mp4")
                        .partyUploaded(UploadParty.DWP)
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .build())
                .build());
        sscsCaseData.setDwpDocuments(dwpDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getPreviewDocument());
        assertNull(response.getData().getSignedBy());
        assertNull(response.getData().getSignedRole());
        assertNull(response.getData().getGenerateNotice());
        assertNull(response.getData().getDateAdded());
        assertNull(response.getData().getReservedToJudge());

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(DocumentType.DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertNull(response.getData().getInterlocReviewState());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
        assertNull(response.getData().getAudioVideoEvidence());
        assertEquals(2, response.getData().getDwpDocuments().size());
        assertEquals(DocumentLink.builder().documentFilename("video.mp4").documentUrl("test.com").documentBinaryUrl("test.com/binary").build(), response.getData().getDwpDocuments().get(0).getValue().getDocumentLink());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpDocuments().get(0).getValue().getDateApproved());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpDocuments().get(0).getValue().getDocumentDateAdded());
        assertEquals("video.mp4", response.getData().getDwpDocuments().get(0).getValue().getDocumentFileName());
        assertEquals("videoDocument", response.getData().getDwpDocuments().get(0).getValue().getDocumentType());
        assertEquals("DWP", response.getData().getDwpDocuments().get(0).getValue().getPartyUploaded().getLabel());
    }

    @Test
    public void excludeEvidence_willClearAudioVideoEvidenceAndInterlocReviewState() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.EXCLUDE_EVIDENCE.getCode()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getPreviewDocument());
        assertNull(response.getData().getSignedBy());
        assertNull(response.getData().getSignedRole());
        assertNull(response.getData().getGenerateNotice());
        assertNull(response.getData().getDateAdded());
        assertNull(response.getData().getReservedToJudge());

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(DocumentType.DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertNull(response.getData().getInterlocReviewState());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
        assertNull(response.getData().getAudioVideoEvidence());
    }

    @Test
    public void sendToJudge_shouldSetInterlocReviewState_toReviewByJudge() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.SEND_TO_JUDGE.getCode()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyNoInteractions(footerService);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getData().getInterlocReviewState(), is(InterlocReviewState.REVIEW_BY_JUDGE.getId()));
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now().toString()));
    }

    @Test
    public void sendToJudgeForWelshAppeal_shouldSetWelshInterlocReviewState_toReviewByJudge() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.SEND_TO_JUDGE.getCode()));
        sscsCaseData.setLanguagePreferenceWelsh(YesNo.YES.getValue());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyNoInteractions(footerService);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getData().getWelshInterlocNextReviewState(), is(InterlocReviewState.REVIEW_BY_JUDGE.getId()));
        assertThat(response.getData().getInterlocReviewState(), is(InterlocReviewState.WELSH_TRANSLATION.getId()));
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now().toString()));
    }

    @Test
    public void givenSendToAdminEventSelected_verifySetInterlocReviewState() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.SEND_TO_ADMIN.getCode()));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(InterlocReviewState.AWAITING_ADMIN_ACTION.getId()));
    }

    @Test
    public void givenSendToAdminEventSelectedForWelshAppeal_verifySetWelshInterlocNextReviewState() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.SEND_TO_ADMIN.getCode()));
        sscsCaseData.setLanguagePreferenceWelsh(YesNo.YES.getValue());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getWelshInterlocNextReviewState(), is(InterlocReviewState.AWAITING_ADMIN_ACTION.getId()));
        assertThat(response.getData().getInterlocReviewState(), is(InterlocReviewState.WELSH_TRANSLATION.getId()));
    }

    @Test
    @Parameters({
            "SEND_TO_JUDGE", "SEND_TO_ADMIN"
    })
    public void shouldAddNote_whenActionIsSelected(ProcessAudioVideoActionDynamicListItems action) {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(action.getCode()));
        final String note = "This is a note";
        sscsCaseData.setAppealNote(note);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyNoInteractions(footerService);
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
        assertNull(response.getData().getAppealNote());
        assertThat(response.getData().getAppealNotePad().getNotesCollection().size(), is(1));
        assertThat(response.getData().getAppealNotePad().getNotesCollection().get(0), is(Note.builder().value(NoteDetails.builder().noteDate(LocalDate.now().toString()).noteDetail(note).build()).build()));
    }
}
