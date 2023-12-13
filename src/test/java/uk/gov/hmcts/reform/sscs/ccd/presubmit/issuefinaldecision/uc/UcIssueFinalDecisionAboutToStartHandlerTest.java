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
import java.util.HashMap;
import java.util.List;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.IssueFinalDecisionAboutToStartHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcWriteFinalDecisionPreviewDecisionService;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.service.*;

@RunWith(JUnitParamsRunner.class)
public class UcIssueFinalDecisionAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    public static final String APPELLANT_LAST_NAME = "APPELLANT Last'NamE";
    private IssueFinalDecisionAboutToStartHandler handler;
    private static final String URL = "http://dm-store/documents/123";
    private static final String UC_TEMPLATE_ID = "esanuts.docx";

    @Mock
    private UserDetailsService userDetailsService;

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

    @Mock
    private VenueDataLoader venueDataLoader;

    private SscsCaseData sscsCaseData;

    private ArgumentCaptor<GenerateFileParams> capture;

    private DecisionNoticeService decisionNoticeService;

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        when(previewDecisionService.getBenefitType()).thenReturn("UC");
        when(ucDecisionNoticeOutcomeService.getBenefitType()).thenReturn("UC");
        decisionNoticeService =
            new DecisionNoticeService(List.of(), List.of(ucDecisionNoticeOutcomeService), List.of(previewDecisionService));
        handler = new IssueFinalDecisionAboutToStartHandler(decisionNoticeService, false, false);
        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(userDetailsService.buildLoggedInUserName("Bearer token")).thenReturn("Judge Full Name");
        capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionGeneratedDate("2018-01-01")
                .writeFinalDecisionPreviewDocument(DocumentLink.builder().documentFilename("filename").build())
                .writeFinalDecisionGenerateNotice(YES)
                .build())
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code("UC").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT")
                        .lastName("Last'NamE")
                        .build())
                    .identity(Identity.builder().build())
                    .build())
                .build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(generateFile.assemble(any())).thenReturn(URL);

        Map<EventType, String> englishEventTypeDocs = new HashMap<>();
        englishEventTypeDocs.put(EventType.ISSUE_FINAL_DECISION, UC_TEMPLATE_ID);

        Map<EventType, String> welshEventTypeDocs = new HashMap<>();
        welshEventTypeDocs.put(EventType.ISSUE_FINAL_DECISION, "TB-SCS-GNO-WEL-00485.docx");

        Map<LanguagePreference, Map<EventType, String>> documents =  new HashMap<>();
        documents.put(LanguagePreference.ENGLISH, englishEventTypeDocs);
        documents.put(LanguagePreference.WELSH, welshEventTypeDocs);
        documentConfiguration.setDocuments(documents);
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
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        when(ucDecisionNoticeOutcomeService.getBenefitType()).thenReturn("UC");
        when(previewDecisionService.preview(callback, DocumentType.FINAL_DECISION_NOTICE, USER_AUTHORISATION, true)).thenReturn(response);

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        verify(previewDecisionService).preview(callback, DocumentType.FINAL_DECISION_NOTICE, USER_AUTHORISATION, true, false, false);
    }

    @Test
    public void givenAboutToStartRequestDescriptorFlow_willGeneratePreviewFileWithoutUpdatingGeneratedDate() throws IOException {
        when(ucDecisionNoticeOutcomeService.getBenefitType()).thenReturn("UC");
        UcDecisionNoticeQuestionService esaDecisionNoticeQuestionService = new UcDecisionNoticeQuestionService();
        final UcWriteFinalDecisionPreviewDecisionService previewDecisionService = new UcWriteFinalDecisionPreviewDecisionService(generateFile, userDetailsService,
            esaDecisionNoticeQuestionService, ucDecisionNoticeOutcomeService, documentConfiguration, venueDataLoader);
        when(generateFile.assemble(any())).thenReturn(URL);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(List.of("mobilisingUnaided"));
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1e");
        sscsCaseData.setWcaAppeal(YES);
        sscsCaseData.setSupportGroupOnlyAppeal("No");
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(NO);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision("2018-10-10");
        when(ucDecisionNoticeOutcomeService.determineOutcome(sscsCaseData)).thenReturn(Outcome.DECISION_IN_FAVOUR_OF_APPELLANT);

        final PreSubmitCallbackResponse<SscsCaseData> previewResponse = previewDecisionService.preview(callback, DocumentType.FINAL_DECISION_NOTICE, USER_AUTHORISATION, true);

        // CHeck that the document has the correct (updated) issued date.
        assertNotNull(previewResponse.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Final Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), previewResponse.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true, true, false,
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

        final UcWriteFinalDecisionPreviewDecisionService previewDecisionService = new UcWriteFinalDecisionPreviewDecisionService(generateFile, userDetailsService,
            esaDecisionNoticeQuestionService, ucDecisionNoticeOutcomeService, documentConfiguration, venueDataLoader);

        when(generateFile.assemble(any())).thenReturn(URL);
        sscsCaseData.setWcaAppeal(NO);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("refused");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision("2018-10-10");

        when(ucDecisionNoticeOutcomeService.determineOutcome(sscsCaseData)).thenReturn(Outcome.DECISION_IN_FAVOUR_OF_APPELLANT);

        final PreSubmitCallbackResponse<SscsCaseData> previewResponse = previewDecisionService.preview(callback, DocumentType.FINAL_DECISION_NOTICE, USER_AUTHORISATION, true);

        // CHeck that the document has the correct (updated) issued date.
        assertNotNull(previewResponse.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Final Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), previewResponse.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true, true, false,
            false, true);

        // Check that the generated date has not been updated
        Assert.assertNotNull(payload.getGeneratedDate());
        Assert.assertEquals(LocalDate.parse("2018-01-01"), payload.getGeneratedDate());
        Assert.assertNotNull(payload.getWriteFinalDecisionTemplateContent());
    }

    @Test
    public void givenNoPreviewDecisionFoundOnCase_thenShowError() {

        when(ucDecisionNoticeOutcomeService.getBenefitType()).thenReturn("UC");

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(null);
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
        assertEquals(isDescriptorFlow, body.isWcaAppeal());

        return payload;
    }
}
