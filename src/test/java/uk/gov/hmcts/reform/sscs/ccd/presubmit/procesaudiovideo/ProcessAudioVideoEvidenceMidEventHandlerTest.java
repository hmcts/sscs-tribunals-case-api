package uk.gov.hmcts.reform.sscs.ccd.presubmit.procesaudiovideo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoEvidenceMidEventHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;

@RunWith(JUnitParamsRunner.class)
public class ProcessAudioVideoEvidenceMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String URL = "http://dm-store/documents/123";

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

        assertNotNull(response.getData().getPreviewDocument());
        assertEquals(DocumentLink.builder()
                .documentFilename(String.format("Directions Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .documentBinaryUrl(URL + "/binary")
                .documentUrl(URL)
                .build(), response.getData().getPreviewDocument());

        verifyTemplateBody(
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.DIRECTION_ISSUED));
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
