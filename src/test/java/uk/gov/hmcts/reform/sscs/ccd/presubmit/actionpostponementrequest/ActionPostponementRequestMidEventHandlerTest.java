package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.ISSUE_DIRECTIONS_NOTICE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;


@RunWith(JUnitParamsRunner.class)
public class ActionPostponementRequestMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String URL = "http://dm-store/documents/123";

    private ActionPostponementRequestMidEventHandler handler;

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
        englishEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-ENG-directions-notice.docx");

        Map<LanguagePreference, Map<EventType, String>> documents = new HashMap<>();
        documents.put(LanguagePreference.ENGLISH, englishEventTypeDocs);

        documentConfiguration.setDocuments(documents);
        handler = new ActionPostponementRequestMidEventHandler(generateFile, documentConfiguration);

        sscsCaseData = SscsCaseData.builder()
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YES)
                .build())
            .appeal(Appeal.builder().appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT").lastName("Last'NamE").build())
                    .identity(Identity.builder().build()).build()).build())
            .directionDueDate(LocalDate.now().plusDays(1).toString())
            .postponementRequest(PostponementRequest.builder().build())
            .build();

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.ACTION_POSTPONEMENT_REQUEST);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(generateFile.assemble(any())).thenReturn(URL);
    }

    @Test
    public void givenAValidMidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.ACTION_POSTPONEMENT_REQUEST);
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenDirectionsDueDateIsToday_ThenDisplayAnError() {
        sscsCaseData.setDirectionDueDate(LocalDate.now().toString());
        sscsCaseData.setProcessAudioVideoAction(new DynamicList(ISSUE_DIRECTIONS_NOTICE.getCode()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Directions due date must be in the future", error);
    }

    @Test
    @Parameters({"grant", "refuse"})
    public void givenLanguagePreferenceIsEnglish_NoticeIsGeneratedAndPopulatedInPreviewDocumentField(String actionSelected) {
        sscsCaseData.getPostponementRequest().setActionPostponementRequestSelected(actionSelected);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getDocumentStaging().getPreviewDocument(), is(notNullValue()));
        final DocumentLink expectedDocumentLink = DocumentLink.builder()
                .documentFilename(String.format("Directions Notice issued on %s.pdf",
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .documentBinaryUrl(URL + "/binary")
                .documentUrl(URL)
                .build();
        assertThat(response.getData().getDocumentStaging().getPreviewDocument(), is(expectedDocumentLink));

        verify(generateFile, times(1)).assemble(any());
        verifyTemplateBody(
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.DIRECTION_ISSUED));
    }

    private void verifyTemplateBody(String templateId) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        var value = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) value.getFormPayload();
        assertEquals(NoticeIssuedTemplateBody.ENGLISH_IMAGE, payload.getImage());
        assertEquals("DIRECTIONS NOTICE", payload.getNoticeType());
        assertEquals("APPELLANT Last'NamE", payload.getAppellantFullName());
        assertEquals(templateId, value.getTemplateId());
    }

}
