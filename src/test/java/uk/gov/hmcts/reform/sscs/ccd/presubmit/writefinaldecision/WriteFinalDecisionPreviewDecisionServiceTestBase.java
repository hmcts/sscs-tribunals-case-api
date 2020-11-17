package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.EsaDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.EsaDecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public abstract class WriteFinalDecisionPreviewDecisionServiceTestBase {

    protected static final String USER_AUTHORISATION = "Bearer token";
    protected static final String URL = "http://dm-store/documents/123";
    protected WriteFinalDecisionPreviewDecisionService service;
    protected String benefitType;

    protected WriteFinalDecisionPreviewDecisionServiceTestBase(String benefitType) {
        this.benefitType = benefitType;
    }

    @Mock
    protected Callback<SscsCaseData> callback;

    @Mock
    protected GenerateFile generateFile;

    @Mock
    protected CaseDetails<SscsCaseData> caseDetails;

    @Mock
    protected IdamClient idamClient;

    @Mock
    protected UserDetails userDetails;

    @Spy
    protected DocumentConfiguration documentConfiguration;

    protected ArgumentCaptor<GenerateFileParams> capture;

    protected SscsCaseData sscsCaseData;

    protected DecisionNoticeOutcomeService pipDecisionNoticeOutcomeService;

    protected PipDecisionNoticeQuestionService pipDecisionNoticeQuestionService;

    protected DecisionNoticeOutcomeService esaDecisionNoticeOutcomeService;

    protected EsaDecisionNoticeQuestionService esaDecisionNoticeQuestionService;
    

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        final Map<EventType, String> englishEventTypeDocs = new HashMap<>();
        final Map<EventType, String> englishEventTypePipDocs = new HashMap<>();
        final Map<EventType, String> englishEventTypeEsaDocs = new HashMap<>();
        englishEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-ENG-00091.docx");
        englishEventTypeDocs.put(EventType.DECISION_ISSUED, "TB-SCS-GNO-ENG-00091.docx");
        englishEventTypePipDocs.put(EventType.ISSUE_FINAL_DECISION, "TB-SCS-GNO-ENG-00453.docx");
        englishEventTypeEsaDocs.put(EventType.ISSUE_FINAL_DECISION, "TB-SCS-GNO-ENG-00642.docx");

        Map<EventType, String> welshEventTypeDocs = new HashMap<>();
        Map<EventType, String> welshEventTypePipDocs = new HashMap<>();
        welshEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-WEL-00485.docx");
        welshEventTypeDocs.put(EventType.DECISION_ISSUED, "TB-SCS-GNO-WEL-00485.docx");

        welshEventTypePipDocs.put(EventType.ISSUE_FINAL_DECISION, "TB-SCS-GNO-WEL-00485.docx");

        final Map<LanguagePreference, Map<EventType, String>> documents =  new HashMap<>();
        final Map<LanguagePreference, Map<EventType, String>> pipDocuments =  new HashMap<>();
        final Map<LanguagePreference, Map<EventType, String>> esaDocuments =  new HashMap<>();

        documents.put(LanguagePreference.ENGLISH, englishEventTypeDocs);
        documents.put(LanguagePreference.WELSH, welshEventTypeDocs);
        pipDocuments.put(LanguagePreference.ENGLISH, englishEventTypePipDocs);
        pipDocuments.put(LanguagePreference.WELSH, welshEventTypePipDocs);
        esaDocuments.put(LanguagePreference.ENGLISH, englishEventTypeEsaDocs);

        documentConfiguration.setDocuments(documents);

        Map<String, Map<LanguagePreference, Map<EventType, String>>> benefitSpecificDocuments = new HashMap<>();
        benefitSpecificDocuments.put("pip", pipDocuments);
        benefitSpecificDocuments.put("esa", esaDocuments);

        documentConfiguration.setBenefitSpecificDocuments(benefitSpecificDocuments);

        this.pipDecisionNoticeOutcomeService = new PipDecisionNoticeOutcomeService();
        this.pipDecisionNoticeQuestionService = new PipDecisionNoticeQuestionService();

        this.esaDecisionNoticeQuestionService = new EsaDecisionNoticeQuestionService();

        this.esaDecisionNoticeOutcomeService = new EsaDecisionNoticeOutcomeService(esaDecisionNoticeQuestionService);

        DecisionNoticeService decisionNoticeService = new DecisionNoticeService(Arrays.asList(pipDecisionNoticeQuestionService, esaDecisionNoticeQuestionService),
            Arrays.asList(pipDecisionNoticeOutcomeService, esaDecisionNoticeOutcomeService));

        service = new WriteFinalDecisionPreviewDecisionService(generateFile, idamClient,
            decisionNoticeService, documentConfiguration);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer token")).thenReturn(userDetails);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(benefitType).build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT")
                        .lastName("LastNamE")
                        .build())
                    .identity(Identity.builder().build())
                    .build())
                .build()).build();


        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);

        when(generateFile.assemble(any())).thenReturn(URL);
    }

    @NamedParameters("previewEndDateAndRateCombinations")
    @SuppressWarnings("unused")
    private Object[] previewEndDateAndRateCombinations() {
        return new Object[]{
            new String[]{"2018-11-10", "standardRate", "lower", "lower"},
            new String[]{"2018-11-10", "standardRate", "same", "lower"},
            new String[]{"2018-11-10", "standardRate", "higher", "lower"},
            new String[]{"2018-11-10", "standardRate", "lower", "same"},
            new String[]{"2018-11-10", "standardRate", "same", "same"},
            new String[]{"2018-11-10", "standardRate", "higher", "same"},
            new String[]{"2018-11-10", "enhancedRate", "same", "lower"},
            new String[]{"2018-11-10", "enhancedRate", "higher", "lower"},
            new String[]{"2018-11-10", "enhancedRate", "same", "same"},
            new String[]{"2018-11-10", "enhancedRate", "higher", "same"},
            new String[]{"2018-11-10", "noAward", "lower", "lower"},
            new String[]{"2018-11-10", "noAward", "same", "lower"},
            new String[]{"2018-11-10", "noAward", "lower", "same"},
            new String[]{"2018-11-10", "noAward", "same", "same"},
            new String[]{"2018-11-10", "notConsidered", "lower", "lower"},
            new String[]{"2018-11-10", "notConsidered", "same", "lower"},
            new String[]{"2018-11-10", "notConsidered", "higher", "lower"},
            new String[]{"2018-11-10", "notConsidered", "lower", "same"},
            new String[]{"2018-11-10", "notConsidered", "same", "same"},
            new String[]{"2018-11-10", "notConsidered", "higher", "same"},
            new String[]{null, "standardRate", "lower", "lower"},
            new String[]{null, "standardRate", "same", "lower"},
            new String[]{null, "standardRate", "higher", "lower"},
            new String[]{null, "standardRate", "lower", "same"},
            new String[]{null, "standardRate", "same", "same"},
            new String[]{null, "standardRate", "higher", "same"},
            new String[]{null, "enhancedRate", "same", "lower"},
            new String[]{null, "enhancedRate", "higher", "lower"},
            new String[]{null, "enhancedRate", "same", "same"},
            new String[]{null, "enhancedRate", "higher", "same"},
            new String[]{null, "noAward", "lower", "lower"},
            new String[]{null, "noAward", "same", "lower"},
            new String[]{null, "noAward", "lower", "same"},
            new String[]{null, "noAward", "same", "same"},
            new String[]{null, "notConsidered", "lower", "lower"},
            new String[]{null, "notConsidered", "same", "lower"},
            new String[]{null, "notConsidered", "higher", "lower"},
            new String[]{null, "notConsidered", "lower", "same"},
            new String[]{null, "notConsidered", "same", "same"},
            new String[]{null, "notConsidered", "higher", "same"},

        };
    }

    protected void setCommonPreviewParams(SscsCaseData sscsCaseData, String endDate) {
        sscsCaseData.setWriteFinalDecisionStartDate("2018-10-11");
        sscsCaseData.setWriteFinalDecisionEndDate(endDate);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionReasons(Arrays.asList(new CollectionItem<>(null, "My reasons for decision")));
        sscsCaseData.setWriteFinalDecisionAnythingElse("Something else.");
        sscsCaseData.setWriteFinalDecisionTypeOfHearing("telephone");
        sscsCaseData.setWriteFinalDecisionPageSectionReference("A1");
        sscsCaseData.setWriteFinalDecisionAppellantAttendedQuestion("Yes");
        sscsCaseData.setWriteFinalDecisionPresentingOfficerAttendedQuestion("No");
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

    }

    protected void assertCommonPreviewParams(WriteFinalDecisionTemplateBody body, String endDate, boolean nullReasonsExpected) {
        assertEquals("2018-10-11",body.getStartDate());
        assertEquals(endDate,body.getEndDate());
        assertEquals("2018-10-10", body.getDateOfDecision());
        assertEquals(endDate == null, body.isIndefinite());
        if (nullReasonsExpected) {
            assertNull(body.getReasonsForDecision());
        } else {
            assertNotNull(body.getReasonsForDecision());
            assertFalse(body.getReasonsForDecision().isEmpty());
            assertEquals("My reasons for decision", body.getReasonsForDecision().get(0));
        }
        assertEquals("Something else.", body.getAnythingElse());
        assertEquals("telephone", body.getHearingType());
        assertEquals("A1", body.getPageNumber());
        assertTrue(body.isAttendedHearing());
        assertFalse(body.isPresentingOfficerAttended());
    }

    protected List<String> getConsideredComparissons(String rate, String nonDescriptorsRate, String descriptorsComparedToDwp,
        String nonDescriptorsComparedWithDwp) {
        List<String> consideredComparissions = new ArrayList<>();
        if (!"notConsidered".equalsIgnoreCase(rate)) {
            consideredComparissions.add(descriptorsComparedToDwp);
        }
        if (!"notConsidered".equalsIgnoreCase(nonDescriptorsRate)) {
            consideredComparissions.add(nonDescriptorsComparedWithDwp);
        }
        return consideredComparissions;
    }

    @Test
    public void givenDateOfDecisionNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine date of decision", error);
    }

    @Test
    public void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        when(userDetails.getFullName()).thenReturn(null);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user name", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenSignedInJudgeUserDetailsNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        when(idamClient.getUserDetails("Bearer token")).thenReturn(null);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenComparedToDwpMobilityQuestionNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenComparedToDwpDailyLivingSetIncorrectly_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("someValue");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");


        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenComparedToDwpMobilityQuestionSetIncorrectly_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("someValue");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
    }

    @Test
    public void givenGenerateNoticeNotSet_willNotSetPreviewFile() {

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithVenues_thenCorrectlySetHeldAtUsingTheFirstHearingInList() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("venue 2 name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true,
            true, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("venue 2 name", body.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenue_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayAnErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = null;

        sscsCaseData.setHearings(Arrays.asList(hearing2, hearing1));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDetails_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithEmptyHearingsList_thenDefaultHearingData() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            true, true, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals("In chambers", body.getHeldAt());

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenCaseWithNullHearingsList_thenDefaultHearingData() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);


        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true,
            true,  true, true, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals("In chambers", body.getHeldAt());

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithHearingDates_thenCorrectlySetTheHeldOnUsingTheFirstHearingInList() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("Venue Name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null,
            "2018-10-10", true, true, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("2019-01-02", body.getHeldOn().toString());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDate_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("Venue Name").build())
            .build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayTwoErrorsAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01")
            .venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = null;

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithMultiplePanelMembers_thenCorrectlySetTheHeldBefore() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        sscsCaseData.setWriteFinalDecisionDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        sscsCaseData.setWriteFinalDecisionMedicallyQualifiedPanelMemberName("Ms Panel Member 2");
        sscsCaseData.setWriteFinalDecisionOtherPanelMemberName("Miss other");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true, true, true,
            true, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1, Ms Panel Member 2 and Miss other", body.getHeldBefore());
    }

    @Test
    public void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        sscsCaseData.setWriteFinalDecisionDisabilityQualifiedPanelMemberName("Mr Panel Member 1");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true,
            true, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);


        assertEquals("Judge Full Name and Mr Panel Member 1", body.getHeldBefore());

    }

    @Test
    public void givenCaseWithNoPanelMembersWithNullValues_thenCorrectlySetTheHeldBefore() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");


        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null,
            "2018-10-10", true,true, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());

    }

    @Test
    public void givenCaseWithNoPanelMembersWithEmptyValues_thenCorrectlySetTheHeldBefore() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionMedicallyQualifiedPanelMemberName("");
        sscsCaseData.setWriteFinalDecisionDisabilityQualifiedPanelMemberName("");


        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            true,true, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));
        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());

    }

    @Test
    public void scottishRpcWillShowAScottishImage() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        verifyTemplateBody(NoticeIssuedTemplateBody.SCOTTISH_IMAGE, "Appellant Lastname", null,
            "2018-10-10", true, true, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));
    }

    protected abstract void setDescriptorFlowIndicator(String value, SscsCaseData sscsCaseData);

    protected abstract boolean getDescriptorFlowIndicator(WriteFinalDecisionTemplateBody body);

    protected abstract void setHigherRateScenarioFields(SscsCaseData caseData);

    @Test
    public void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppendedAndAppointeeFullNameSet() {

        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.getAppeal().getAppellant().setIsAppointee("Yes");
        sscsCaseData.getAppeal().getAppellant().setAppointee(Appointee.builder()
            .name(Name.builder().firstName("APPOINTEE")
                .lastName("SurNamE")
                .build())
            .identity(Identity.builder().build())
            .build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appointee Surname, appointee for Appellant Lastname", "Appointee Surname", "2018-10-10", true,
            true, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));
    }

    @Test
    public void givenDateIssuedParameterIsTrue_thenShowIssuedDateOnDocument() {
        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true,
            true, true, true, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now(), payload.getDateIssued());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetGeneratedDescriptorFlow_thenSetNewGeneratedDate() {
        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true, true, true,
            true, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetGeneratedNonDescriptorFlow_thenSetNewGeneratedDate() {
        setDescriptorFlowIndicator("no", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true, true, true, false, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetNonGeneratedDescriptorFlow_thenDoSetNewGeneratedDate() {
        setDescriptorFlowIndicator("yes", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("no");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true, true, true,
            true, false, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetNonGeneratedNonDescriptorFlow_thenDoSetNewGeneratedDate() {
        setDescriptorFlowIndicator("no", sscsCaseData);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("no");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",  true, true, true,
            false, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Test
    public void givenWelsh_GeneratedDateIsAlreadySet_thenDoNotSetNewGeneratedDate() {
        sscsCaseData.setLanguagePreferenceWelsh("yes");
        setDescriptorFlowIndicator("no", sscsCaseData);
        sscsCaseData.setWriteFinalDecisionGenerateNotice("no");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",  true, true, true,
                false, true, documentConfiguration.getBenefitSpecificDocuments().get(benefitType.toLowerCase()).get(LanguagePreference.WELSH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    protected NoticeIssuedTemplateBody verifyTemplateBody(String image, String expectedName, String expectedAppointeeName, String dateOfDecision, boolean allowed, boolean isSetAside, boolean isDraft,
        boolean isDescriptorFlow, boolean isGenerateFile, String templateId) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        GenerateFileParams generateFileParams = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) generateFileParams.getFormPayload();
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
        assertEquals(isDescriptorFlow, getDescriptorFlowIndicator(body));
        assertEquals(templateId, generateFileParams.getTemplateId());

        return payload;
    }
}
