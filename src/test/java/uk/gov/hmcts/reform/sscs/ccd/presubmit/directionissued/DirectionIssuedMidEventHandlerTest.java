package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

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
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;

@RunWith(JUnitParamsRunner.class)
public class DirectionIssuedMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    //private static final String TEMPLATE_ID = "nuts.docx";
    private static final String URL = "http://dm-store/documents/123";
    public static final String APPELLANT_LAST_NAME = "APPELLANT Last'NamE";

    private DirectionIssuedMidEventHandler handler;

    @Spy
    private DocumentConfiguration documentConfiguration;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private GenerateFile generateFile;

    private SscsCaseData sscsCaseData;

    private ArgumentCaptor<GenerateFileParams> capture;

    @Before
    public void setUp() {
        openMocks(this);
        Map<EventType, String> englishEventTypeDocs = new HashMap<>();
        englishEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-ENG-directions-notice.docx");
        englishEventTypeDocs.put(EventType.DECISION_ISSUED, "TB-SCS-GNO-ENG-draft-decision-notice.docx");
        englishEventTypeDocs.put(EventType.ISSUE_FINAL_DECISION, "TB-SCS-GNO-ENG-00453.docx");


        Map<EventType, String> welshEventTypeDocs = new HashMap<>();
        welshEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-WEL-00485.docx");
        welshEventTypeDocs.put(EventType.DECISION_ISSUED, "TB-SCS-GNO-WEL-00485.docx");
        welshEventTypeDocs.put(EventType.ISSUE_FINAL_DECISION, "TB-SCS-GNO-WEL-00485.docx");

        Map<LanguagePreference, Map<EventType, String>> documents =  new HashMap<>();
        documents.put(LanguagePreference.ENGLISH, englishEventTypeDocs);
        documents.put(LanguagePreference.WELSH, welshEventTypeDocs);

        documentConfiguration.setDocuments(documents);
        handler = new DirectionIssuedMidEventHandler(generateFile, documentConfiguration);

        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);

        sscsCaseData = SscsCaseData.builder()
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YES)
                .build())
                .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
                .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("APPELLANT")
                                        .lastName("Last'NamE")
                                        .build())
                                .identity(Identity.builder().build())
                                .build())
                        .build()).build();

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(generateFile.assemble(any())).thenReturn(URL);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenGenerateNoticeIsNo_thenReturnFalse() {
        sscsCaseData.getDocumentGeneration().setGenerateNotice(NO);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenGenerateNoticeIsYes_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void willSetPreviewFile() {

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getDocumentStaging().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Directions Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getDocumentStaging().getPreviewDocument());

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME,
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.DIRECTION_ISSUED),
                sscsCaseData.isLanguagePreferenceWelsh());
    }

    @Test
    public void scottishRpcWillShowAScottishImage() {
        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verifyTemplateBody(NoticeIssuedTemplateBody.SCOTTISH_IMAGE, APPELLANT_LAST_NAME,
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.DIRECTION_ISSUED),
                sscsCaseData.isLanguagePreferenceWelsh());
    }

    @Test
    public void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppended() {
        sscsCaseData.getAppeal().getAppellant().setIsAppointee("Yes");
        sscsCaseData.getAppeal().getAppellant().setAppointee(Appointee.builder()
                .name(Name.builder().firstName("APPOINTEE")
                        .lastName("Sur-NamE")
                        .build())
                .identity(Identity.builder().build())
                .build());

        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "APPOINTEE Sur-NamE, appointee for APPELLANT Last'NamE",
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.DIRECTION_ISSUED),
                sscsCaseData.isLanguagePreferenceWelsh());
    }

    @Test
    public void givenWelsh_CaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppended() {
        sscsCaseData.setLanguagePreferenceWelsh("yes");
        sscsCaseData.getAppeal().getAppellant().setIsAppointee("Yes");
        sscsCaseData.getAppeal().getAppellant().setAppointee(Appointee.builder()
                .name(Name.builder().firstName("APPOINTEE")
                        .lastName("Sur-NamE")
                        .build())
                .identity(Identity.builder().build())
                .build());

        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "APPOINTEE Sur-NamE, appointee for APPELLANT Last'NamE",
                documentConfiguration.getDocuments().get(LanguagePreference.WELSH).get(EventType.DIRECTION_ISSUED),
                sscsCaseData.isLanguagePreferenceWelsh());
    }

    @Test
    public void givenDirectionTypeIsNull_displayAnError() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("Direction Type cannot be empty", response.getErrors().toArray()[0]);
    }

    private void verifyTemplateBody(String image, String expectedName, String templateId, boolean isLanguageWelsh) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        var value = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) value.getFormPayload();
        assertEquals(image, payload.getImage());
        assertEquals("DIRECTIONS NOTICE", payload.getNoticeType());
        assertEquals(expectedName, payload.getAppellantFullName());
        assertEquals(templateId, value.getTemplateId());
        if (isLanguageWelsh) {
            assertNotNull(payload.getWelshDateAdded());
            assertNotNull(payload.getWelshGeneratedDate());
        }
    }

    @Test
    public void shouldErrorWhenDirectionTypeIsProvideInformationAndNoDueDate() {
        sscsCaseData.setDirectionDueDate(null);
        sscsCaseData.getDirectionTypeDl().setValue(new DynamicListItem(DirectionType.PROVIDE_INFORMATION.toString(), "appeal To Proceed"));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        assertEquals("Please populate the direction due date", response.getErrors().toArray()[0]);
    }

}

