package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionService;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class IssueFinalDecisionAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueFinalDecisionAboutToStartHandler handler;
    private static final String URL = "http://dm-store/documents/123";
    private static final String TEMPLATE_ID = "nuts.docx";
    private static final String ESA_TEMPLATE_ID = "esanuts.docx";

    @Mock
    private IdamClient idamClient;

    @Mock
    private UserDetails userDetails;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private DecisionNoticeOutcomeService decisionNoticeOutcomeService;

    @Mock
    private WriteFinalDecisionPreviewDecisionService previewDecisionService;

    @Spy
    private DocumentConfiguration documentConfiguration;

    @Mock
    private GenerateFile generateFile;

    private SscsCaseData sscsCaseData;

    private ArgumentCaptor<GenerateFileParams> capture;

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        handler = new IssueFinalDecisionAboutToStartHandler(previewDecisionService);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer token")).thenReturn(userDetails);


        capture = ArgumentCaptor.forClass(GenerateFileParams.class);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .writeFinalDecisionGeneratedDate("2018-01-01")
            .writeFinalDecisionPreviewDocument(DocumentLink.builder().build())
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("PIP").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT")
                        .lastName("LastNamE")
                        .build())
                    .identity(Identity.builder().build())
                    .build())
                .build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(generateFile.assemble(any())).thenReturn(URL);

        Map<EventType, String> englishEventTypePipDocs = new HashMap<>();
        englishEventTypePipDocs.put(EventType.ISSUE_FINAL_DECISION, TEMPLATE_ID);

        Map<EventType, String> englishEventTypeEsaDocs = new HashMap<>();
        englishEventTypeEsaDocs.put(EventType.ISSUE_FINAL_DECISION, ESA_TEMPLATE_ID);

        Map<EventType, String> welshEventTypePipDocs = new HashMap<>();
        welshEventTypePipDocs.put(EventType.ISSUE_FINAL_DECISION, "TB-SCS-GNO-WEL-00485.docx");

        Map<LanguagePreference, Map<EventType, String>> pipDocuments =  new HashMap<>();
        Map<LanguagePreference, Map<EventType, String>> esaDocuments =  new HashMap<>();
        pipDocuments.put(LanguagePreference.ENGLISH, englishEventTypePipDocs);
        pipDocuments.put(LanguagePreference.WELSH, welshEventTypePipDocs);
        esaDocuments.put(LanguagePreference.ENGLISH, englishEventTypeEsaDocs);
        Map<String, Map<LanguagePreference, Map<EventType, String>>> benefitSpecificDocuments = new HashMap<>();
        benefitSpecificDocuments.put("pip", pipDocuments);
        benefitSpecificDocuments.put("esa", esaDocuments);

        documentConfiguration.setBenefitSpecificDocuments(benefitSpecificDocuments);
    }

    @Test
    public void givenANonIssueFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAboutToStartRequest_willGeneratePreviewFile() {

        when(decisionNoticeOutcomeService.getBenefitType()).thenReturn("PIP");

        PreSubmitCallbackResponse response = new PreSubmitCallbackResponse(sscsCaseData);

        when(previewDecisionService.preview(callback, DocumentType.FINAL_DECISION_NOTICE, USER_AUTHORISATION, true)).thenReturn(response);
        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        verify(previewDecisionService).preview(callback, DocumentType.FINAL_DECISION_NOTICE, USER_AUTHORISATION, true);

    }

    @Test
    public void givenAboutToStartRequestDescriptorFlow_willGeneratePreviewFileWithoutUpdatingGeneratedDate() throws IOException {

        when(decisionNoticeOutcomeService.getBenefitType()).thenReturn("PIP");

        PipDecisionNoticeQuestionService pipDecisionNoticeQuestionService = new PipDecisionNoticeQuestionService();

        DecisionNoticeService decisionNoticeService = new DecisionNoticeService(Arrays.asList(pipDecisionNoticeQuestionService), Arrays.asList(decisionNoticeOutcomeService));

        final WriteFinalDecisionPreviewDecisionService previewDecisionService = new WriteFinalDecisionPreviewDecisionService(generateFile, idamClient,
            decisionNoticeService, documentConfiguration);

        when(generateFile.assemble(any())).thenReturn(URL);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        when(decisionNoticeOutcomeService.determineOutcome(sscsCaseData)).thenReturn(Outcome.DECISION_IN_FAVOUR_OF_APPELLANT);

        final PreSubmitCallbackResponse<SscsCaseData> previewResponse = previewDecisionService.preview(callback, DocumentType.FINAL_DECISION_NOTICE, USER_AUTHORISATION, true);

        // CHeck that the document has the correct (updated) issued date.
        assertNotNull(previewResponse.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Final Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), previewResponse.getData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true, true, false,
            true, true);

        // Check that the generated date has not been updated
        Assert.assertNotNull(payload.getGeneratedDate());
        Assert.assertEquals(LocalDate.parse("2018-01-01"), payload.getGeneratedDate());
    }

    @Test
    public void givenAboutToStartRequestNonDescriptorFlow_willGeneratePreviewFileWithoutUpdatingGeneratedDate() throws IOException {

        PipDecisionNoticeQuestionService pipDecisionNoticeQuestionService = new PipDecisionNoticeQuestionService();

        when(decisionNoticeOutcomeService.getBenefitType()).thenReturn("PIP");

        DecisionNoticeService decisionNoticeService = new DecisionNoticeService(Arrays.asList(pipDecisionNoticeQuestionService), Arrays.asList(decisionNoticeOutcomeService));

        final WriteFinalDecisionPreviewDecisionService previewDecisionService = new WriteFinalDecisionPreviewDecisionService(generateFile, idamClient,
            decisionNoticeService, documentConfiguration);

        when(generateFile.assemble(any())).thenReturn(URL);

        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("no");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        when(decisionNoticeOutcomeService.determineOutcome(sscsCaseData)).thenReturn(Outcome.DECISION_IN_FAVOUR_OF_APPELLANT);

        final PreSubmitCallbackResponse<SscsCaseData> previewResponse = previewDecisionService.preview(callback, DocumentType.FINAL_DECISION_NOTICE, USER_AUTHORISATION, true);

        // CHeck that the document has the correct (updated) issued date.
        assertNotNull(previewResponse.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Final Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), previewResponse.getData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true, true, false,
            false, true);

        // Check that the generated date has not been updated
        Assert.assertNotNull(payload.getGeneratedDate());
        Assert.assertEquals(LocalDate.parse("2018-01-01"), payload.getGeneratedDate());
    }

    @Test
    public void givenNoPreviewDecisionFoundOnCase_thenShowError() {

        when(decisionNoticeOutcomeService.getBenefitType()).thenReturn("PIP");

        sscsCaseData.setWriteFinalDecisionPreviewDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String error = result.getErrors().stream().findFirst().orElse("");
        assertEquals("No draft final decision notice found on case. Please use 'Write final decision' event before trying to issue.", error);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {

        when(decisionNoticeOutcomeService.getBenefitType()).thenReturn("PIP");

        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
    }

    private NoticeIssuedTemplateBody verifyTemplateBody(String image, String expectedName, String expectedAppointeeName, String dateOfDecision, boolean allowed, boolean isSetAside, boolean isDraft,
        boolean isDescriptorFlow, boolean isGenerateFile) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        when(decisionNoticeOutcomeService.getBenefitType()).thenReturn("PIP");

        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        assertEquals(image, payload.getImage());
        if (isDraft) {
            assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());
        } else {
            assertEquals("DECISION NOTICE", payload.getNoticeType());
        }
        assertEquals(expectedName, payload.getAppellantFullName());
        assertEquals(expectedAppointeeName, payload.getAppointeeFullName());
        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);
        assertEquals(dateOfDecision, body.getDateOfDecision());
        assertEquals(allowed, body.isAllowed());
        assertEquals(isSetAside, body.isSetAside());
        assertNull(body.getDetailsOfDecision());
        assertEquals(isDescriptorFlow, body.isDescriptorFlow());

        return payload;
    }
}
