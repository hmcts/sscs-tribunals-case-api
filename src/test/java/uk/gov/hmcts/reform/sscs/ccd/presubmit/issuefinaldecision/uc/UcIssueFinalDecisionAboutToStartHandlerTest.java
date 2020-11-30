package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.uc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

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
import org.mockito.Mockito;
import org.mockito.Spy;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.IssueFinalDecisionAboutToStartHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcWriteFinalDecisionPreviewDecisionService;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.UcDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.UcDecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class UcIssueFinalDecisionAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueFinalDecisionAboutToStartHandler handler;
    private static final String URL = "http://dm-store/documents/123";
    private static final String TEMPLATE_ID = "nuts.docx";
    private static final String UC_TEMPLATE_ID = "esanuts.docx";

    @Mock
    private IdamClient idamClient;

    @Mock
    private UserDetails userDetails;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private UcDecisionNoticeOutcomeService ucDecisionNoticeOutcomeService;

    @Mock
    private WriteFinalDecisionPreviewDecisionServiceBase previewDecisionService;

    @Spy
    private DocumentConfiguration documentConfiguration;

    @Mock
    private GenerateFile generateFile;

    private SscsCaseData sscsCaseData;

    private ArgumentCaptor<GenerateFileParams> capture;

    private DecisionNoticeService decisionNoticeService;

    @Before
    public void setUp() throws IOException {
        openMocks(this);

        Mockito.when(previewDecisionService.getBenefitType()).thenReturn("UC");
        Mockito.when(ucDecisionNoticeOutcomeService.getBenefitType()).thenReturn("UC");

        decisionNoticeService =
            new DecisionNoticeService(Arrays.asList(),
                Arrays.asList(ucDecisionNoticeOutcomeService), Arrays.asList(previewDecisionService));

        handler = new IssueFinalDecisionAboutToStartHandler(decisionNoticeService);

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
                .benefitType(BenefitType.builder().code("UC").build())
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

        Map<EventType, String> englishEventTypeUcDocs = new HashMap<>();
        englishEventTypeUcDocs.put(EventType.ISSUE_FINAL_DECISION, UC_TEMPLATE_ID);

        Map<EventType, String> welshEventTypePipDocs = new HashMap<>();
        welshEventTypePipDocs.put(EventType.ISSUE_FINAL_DECISION, "TB-SCS-GNO-WEL-00485.docx");

        Map<LanguagePreference, Map<EventType, String>> pipDocuments =  new HashMap<>();
        Map<LanguagePreference, Map<EventType, String>> ucDocuments =  new HashMap<>();
        pipDocuments.put(LanguagePreference.ENGLISH, englishEventTypePipDocs);
        pipDocuments.put(LanguagePreference.WELSH, welshEventTypePipDocs);
        ucDocuments.put(LanguagePreference.ENGLISH, englishEventTypeUcDocs);
        Map<String, Map<LanguagePreference, Map<EventType, String>>> benefitSpecificDocuments = new HashMap<>();
        benefitSpecificDocuments.put("pip", pipDocuments);
        benefitSpecificDocuments.put("uc", ucDocuments);

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

        when(ucDecisionNoticeOutcomeService.getBenefitType()).thenReturn("UC");

        PreSubmitCallbackResponse response = new PreSubmitCallbackResponse(sscsCaseData);

        when(previewDecisionService.preview(callback, DocumentType.FINAL_DECISION_NOTICE, USER_AUTHORISATION, true)).thenReturn(response);
        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        verify(previewDecisionService).preview(callback, DocumentType.FINAL_DECISION_NOTICE, USER_AUTHORISATION, true);

    }

    @Test
    public void givenAboutToStartRequestDescriptorFlow_willGeneratePreviewFileWithoutUpdatingGeneratedDate() throws IOException {

        when(ucDecisionNoticeOutcomeService.getBenefitType()).thenReturn("UC");

        UcDecisionNoticeQuestionService esaDecisionNoticeQuestionService = new UcDecisionNoticeQuestionService();

        final UcWriteFinalDecisionPreviewDecisionService previewDecisionService = new UcWriteFinalDecisionPreviewDecisionService(generateFile, idamClient,
            esaDecisionNoticeQuestionService, ucDecisionNoticeOutcomeService, documentConfiguration);

        when(generateFile.assemble(any())).thenReturn(URL);

        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList("mobilisingUnaided"));
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");
        sscsCaseData.getSscsUcCaseData().setLcwaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(NO);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        when(ucDecisionNoticeOutcomeService.determineOutcome(sscsCaseData)).thenReturn(Outcome.DECISION_IN_FAVOUR_OF_APPELLANT);

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
        Assert.assertNotNull(payload.getWriteFinalDecisionTemplateContent());
    }

    @Test
    public void givenAboutToStartRequestNonDescriptorFlow_willGeneratePreviewFileWithoutUpdatingGeneratedDate() throws IOException {

        UcDecisionNoticeQuestionService esaDecisionNoticeQuestionService = new UcDecisionNoticeQuestionService();

        when(ucDecisionNoticeOutcomeService.getBenefitType()).thenReturn("UC");

        final UcWriteFinalDecisionPreviewDecisionService previewDecisionService = new UcWriteFinalDecisionPreviewDecisionService(generateFile, idamClient,
            esaDecisionNoticeQuestionService, ucDecisionNoticeOutcomeService, documentConfiguration);

        when(generateFile.assemble(any())).thenReturn(URL);
        sscsCaseData.getSscsUcCaseData().setLcwaAppeal(NO);
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        when(ucDecisionNoticeOutcomeService.determineOutcome(sscsCaseData)).thenReturn(Outcome.DECISION_IN_FAVOUR_OF_APPELLANT);

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
        Assert.assertNotNull(payload.getWriteFinalDecisionTemplateContent());
    }

    @Test
    public void givenNoPreviewDecisionFoundOnCase_thenShowError() {

        when(ucDecisionNoticeOutcomeService.getBenefitType()).thenReturn("UC");

        sscsCaseData.setWriteFinalDecisionPreviewDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String error = result.getErrors().stream().findFirst().orElse("");
        assertEquals("No draft final decision notice found on case. Please use 'Write final decision' event before trying to issue.", error);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {

        when(ucDecisionNoticeOutcomeService.getBenefitType()).thenReturn("UC");

        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
    }

    private NoticeIssuedTemplateBody verifyTemplateBody(String image, String expectedName, String expectedAppointeeName, String dateOfDecision, boolean allowed, boolean isSetAside, boolean isDraft,
        boolean isDescriptorFlow, boolean isGenerateFile) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        when(ucDecisionNoticeOutcomeService.getBenefitType()).thenReturn("UC");

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
        assertEquals(isDescriptorFlow, body.isLcwaAppeal());

        return payload;
    }
}
