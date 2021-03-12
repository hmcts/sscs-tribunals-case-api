package uk.gov.hmcts.reform.sscs.ccd.presubmit.procesaudiovideo;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoEvidenceMidEventHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;

@RunWith(JUnitParamsRunner.class)
public class ProcessAudioVideoEvidenceMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String URL = "http://dm-store/documents/123";

    private static final LocalDate DATE = LocalDate.of(2021, 1,1);

    private static final DocumentLink DOCUMENT_LINK = DocumentLink.builder().documentFilename("music.mp3").documentUrl("test.com").documentBinaryUrl("test.com/binary").build();

    private ProcessAudioVideoEvidenceMidEventHandler handler;

    @Spy
    private DocumentConfiguration documentConfiguration;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    private ArgumentCaptor<GenerateFileParams> capture;

    @Mock
    private GenerateFile generateFile;

    @Before
    public void setUp() {
        openMocks(this);

        Map<EventType, String> englishEventTypeDocs = new HashMap<>();
        englishEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-ENG-00091.docx");

        Map<LanguagePreference, Map<EventType, String>> documents =  new HashMap<>();
        documents.put(LanguagePreference.ENGLISH, englishEventTypeDocs);

        documentConfiguration.setDocuments(documents);
        handler = new ProcessAudioVideoEvidenceMidEventHandler(generateFile, documentConfiguration);

        sscsCaseData = SscsCaseData.builder()
                .generateNotice("Yes")
                .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
                .processAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.ISSUE_DIRECTIONS_NOTICE.getCode()))
                .selectedAudioVideoEvidence(new DynamicList("test.com"))
                .audioVideoEvidence(singletonList(AudioVideoEvidence.builder().value(
                        AudioVideoEvidenceDetails.builder()
                                .documentLink(DOCUMENT_LINK)
                                .fileName("music.mp3")
                                .partyUploaded(UploadParty.APPELLANT)
                                .dateAdded(DATE)
                                .build())
                        .build()))
                .selectedAudioVideoEvidenceDetails(AudioVideoEvidenceDetails.builder().build())
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("APPELLANT")
                                        .lastName("LastNamE")
                                        .build())
                                .identity(Identity.builder().build())
                                .build())
                        .build()).build();

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.PROCESS_AUDIO_VIDEO);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(generateFile.assemble(any())).thenReturn(URL);
    }

    @Test
    public void givenLanguagePreferenceIsEnglish_NoticeIsGeneratedAndPopulatedInPreviewDocumentField() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getPreviewDocument(), is(notNullValue()));
        final DocumentLink expectedDocumentLink = DocumentLink.builder()
                .documentFilename(String.format("Directions Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .documentBinaryUrl(URL + "/binary")
                .documentUrl(URL)
                .build();
        assertThat(response.getData().getPreviewDocument(), is(expectedDocumentLink));

        verify(generateFile, times(1)).assemble(any());
        verifyTemplateBody(
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.DIRECTION_ISSUED));
    }

    @Test
    public void giveSendToJudge_thenDoNotSetPreviewDocument() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.SEND_TO_JUDGE.getCode()));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getPreviewDocument(), is(nullValue()));
        verifyNoInteractions(generateFile);
    }

    @Test
    public void giveSendToAdmin_thenDoNotSetPreviewDocument() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.SEND_TO_ADMIN.getCode()));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getPreviewDocument(), is(nullValue()));
        verifyNoInteractions(generateFile);
    }

    @Test
    public void givenSelectedAudioVideoEvidenceDetailsIsNull_thenDoNotSetPreviewDocument_AndBuildNewSelectedAudioVideoEvidenceDetailsObject() {
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ProcessAudioVideoActionDynamicListItems.SEND_TO_ADMIN.getCode()));
        sscsCaseData.setSelectedAudioVideoEvidenceDetails(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        final AudioVideoEvidenceDetails expectedEvidenceDetails = AudioVideoEvidenceDetails.builder()
                .partyUploaded(UploadParty.APPELLANT)
                .dateAdded(DATE)
                .documentType("Audio document")
                .documentLink(DOCUMENT_LINK)
                .fileName("music.mp3")
                .build();

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getPreviewDocument(), is(nullValue()));
        verifyNoInteractions(generateFile);
        assertEquals(response.getData().getSelectedAudioVideoEvidenceDetails(), expectedEvidenceDetails);
    }

    private void verifyTemplateBody(String templateId) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        var value = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) value.getFormPayload();
        assertEquals(NoticeIssuedTemplateBody.ENGLISH_IMAGE, payload.getImage());
        assertEquals("DIRECTIONS NOTICE", payload.getNoticeType());
        assertEquals("Appellant Lastname", payload.getAppellantFullName());
        assertEquals(templateId, value.getTemplateId());
    }
}
