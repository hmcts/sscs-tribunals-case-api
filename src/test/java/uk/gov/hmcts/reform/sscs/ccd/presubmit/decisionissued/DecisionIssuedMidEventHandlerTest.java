package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;


@RunWith(JUnitParamsRunner.class)
public class DecisionIssuedMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String TEMPLATE_ID = "nuts.docx";
    private static final String URL = "http://dm-store/documents/123";
    public static final String APPELLANT_LAST_NAME = "APPELLANT Last'NamE";

    private DecisionIssuedMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private GenerateFile generateFile;

    @Spy
    private DocumentConfiguration documentConfiguration;

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
        handler = new DecisionIssuedMidEventHandler(generateFile, documentConfiguration);

        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);

        sscsCaseData = SscsCaseData.builder()
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YES)
                .build())
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
                .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .documentBinaryUrl(URL + "/binary")
                .documentUrl(URL)
                .build(), response.getData().getDocumentStaging().getPreviewDocument());

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME,
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.DIRECTION_ISSUED));
    }

    @Test
    public void scottishRpcWillShowAScottishImage() {
        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verifyTemplateBody(NoticeIssuedTemplateBody.SCOTTISH_IMAGE, APPELLANT_LAST_NAME,
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.DIRECTION_ISSUED));
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

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE,
                "APPOINTEE Sur-NamE, appointee for APPELLANT Last'NamE",
                documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.DIRECTION_ISSUED));
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
                documentConfiguration.getDocuments().get(LanguagePreference.WELSH).get(EventType.DECISION_ISSUED));
    }

    private void verifyTemplateBody(String image, String expectedName, String templateId) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        var value = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) value.getFormPayload();
        assertEquals(image, payload.getImage());
        assertEquals("DECISION NOTICE", payload.getNoticeType());
        assertEquals(expectedName, payload.getAppellantFullName());
        assertEquals(templateId, value.getTemplateId());

    }
}

