package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class WriteFinalDecisionPreviewDecisionServiceTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String URL = "http://dm-store/documents/123";
    private WriteFinalDecisionPreviewDecisionService service;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private GenerateFile generateFile;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamClient idamClient;

    @Mock
    private UserDetails userDetails;

    @Spy
    private DocumentConfiguration documentConfiguration;

    private ArgumentCaptor<GenerateFileParams> capture;

    private SscsCaseData sscsCaseData;

    private DecisionNoticeOutcomeService decisionNoticeOutcomeService;
    private PipDecisionNoticeQuestionService pipDecisionNoticeQuestionService;


    @Before
    public void setUp() throws IOException {
        openMocks(this);
        Map<EventType, String> englishEventTypeDocs = new HashMap<>();
        englishEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-ENG-00091.docx");
        englishEventTypeDocs.put(EventType.DECISION_ISSUED, "TB-SCS-GNO-ENG-00091.docx");
        englishEventTypeDocs.put(EventType.ISSUE_FINAL_DECISION, "TB-SCS-GNO-ENG-00453.docx");


        Map<EventType, String> welshEventTypeDocs = new HashMap<>();
        welshEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-WEL-00485.docx");
        welshEventTypeDocs.put(EventType.DECISION_ISSUED, "TB-SCS-GNO-WEL-00485.docx");
        welshEventTypeDocs.put(EventType.ISSUE_FINAL_DECISION, "TB-SCS-GNO-WEL-00485.docx");

        Map<LanguagePreference, Map<EventType, String>> documents =  new HashMap<>();
        documents.put(LanguagePreference.ENGLISH, englishEventTypeDocs);
        documents.put(LanguagePreference.WELSH, welshEventTypeDocs);

        documentConfiguration.setDocuments(documents);

        this.decisionNoticeOutcomeService = new DecisionNoticeOutcomeService();
        this.pipDecisionNoticeQuestionService = new PipDecisionNoticeQuestionService();
        service = new WriteFinalDecisionPreviewDecisionService(generateFile, idamClient, decisionNoticeOutcomeService,
            pipDecisionNoticeQuestionService, documentConfiguration);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer token")).thenReturn(userDetails);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
            .appeal(Appeal.builder()
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

    private void setCommonPreviewParams(SscsCaseData sscsCaseData, String endDate) {
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

    private void assertCommonPreviewParams(WriteFinalDecisionTemplateBody body, String endDate, boolean nullReasonsExpected) {
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

    @Test
    public void willSetPreviewFileWithNullReasons_WhenReasonsListIsEmpty() {

        final String endDate = "10-10-2020";
        final String rate = "standardRate";
        final String descriptorsComparedToDwp = "higher";
        final String nonDescriptorsComparedWithDwp = "higher";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.setWriteFinalDecisionReasons(new ArrayList<>());

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(descriptorsComparedToDwp);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(nonDescriptorsComparedWithDwp);

        // Mobility specific parameters
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(rate);
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(Arrays.asList("movingAround"));
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = "higher".equals(descriptorsComparedToDwp) && !"lower".equals(nonDescriptorsComparedWithDwp);

        boolean setAsideExpectation = true;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, true);

        assertEquals("standard rate", body.getMobilityAwardRate());
        assertEquals(false, body.isMobilityIsSeverelyLimited());
        assertEquals(true, body.isMobilityIsEntited());

        assertNotNull(body.getMobilityDescriptors());
        assertEquals(1, body.getMobilityDescriptors().size());
        assertNotNull(body.getMobilityDescriptors().get(0));
        assertEquals(10, body.getMobilityDescriptors().get(0).getActivityAnswerPoints());
        assertEquals("d", body.getMobilityDescriptors().get(0).getActivityAnswerLetter());
        assertEquals("Can stand and then move using an aid or appliance more than 20 metres but no more than 50 metres.", body.getMobilityDescriptors().get(0).getActivityAnswerValue());
        assertEquals("Moving around", body.getMobilityDescriptors().get(0).getActivityQuestionValue());
        assertEquals("12", body.getMobilityDescriptors().get(0).getActivityQuestionNumber());
        assertNotNull(body.getMobilityNumberOfPoints());
        assertEquals(10, body.getMobilityNumberOfPoints().intValue());

        // Daily living specific assertions
        assertEquals(false, body.isDailyLivingIsEntited());
        assertEquals(false, body.isDailyLivingIsSeverelyLimited());
        assertNull(body.getDailyLivingAwardRate());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    private List<String> getConsideredComparissons(String rate, String nonDescriptorsRate, String descriptorsComparedToDwp,
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
    @Parameters(named = "previewEndDateAndRateCombinations")
    public void willSetPreviewFile_whenMobilityDescriptorsOnly_ForEndDateAndRate(String endDate, String rate, String descriptorsComparedToDwp,
        String nonDescriptorsComparedWithDwp) {

        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(descriptorsComparedToDwp);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(nonDescriptorsComparedWithDwp);

        if ("noAward".equals(rate) || "notConsidered".equals(rate)) {
            sscsCaseData.setWriteFinalDecisionEndDateType("na");
        }

        // Mobility specific parameters
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(rate);
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("notConsidered");
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(Arrays.asList("movingAround"));
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = !"notConsidered".equalsIgnoreCase(rate) && "higher".equals(descriptorsComparedToDwp);

        boolean setAsideExpectation = getConsideredComparissons(rate, "notConsidered", descriptorsComparedToDwp, nonDescriptorsComparedWithDwp).stream().anyMatch(comparission ->
            !"same".equalsIgnoreCase(comparission));

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        // Mobility specific assertions
        if ("standardRate".equals(rate)) {
            assertEquals("standard rate", body.getMobilityAwardRate());
            assertEquals(false, body.isMobilityIsSeverelyLimited());
            assertEquals(true, body.isMobilityIsEntited());
        } else if ("enhancedRate".equals(rate)) {
            assertEquals("enhanced rate", body.getMobilityAwardRate());
            assertEquals(true, body.isMobilityIsSeverelyLimited());
            assertEquals(true, body.isMobilityIsEntited());

        } else {
            assertEquals(false, body.isMobilityIsEntited());
        }

        if ("notConsidered".equals(rate)) {

            assertNull(body.getMobilityDescriptors());
            assertNull(body.getMobilityNumberOfPoints());

        } else {

            assertNotNull(body.getMobilityDescriptors());
            assertEquals(1, body.getMobilityDescriptors().size());
            assertNotNull(body.getMobilityDescriptors().get(0));
            assertEquals(10, body.getMobilityDescriptors().get(0).getActivityAnswerPoints());
            assertEquals("d", body.getMobilityDescriptors().get(0).getActivityAnswerLetter());
            assertEquals("Can stand and then move using an aid or appliance more than 20 metres but no more than 50 metres.", body.getMobilityDescriptors().get(0).getActivityAnswerValue());
            assertEquals("Moving around", body.getMobilityDescriptors().get(0).getActivityQuestionValue());
            assertEquals("12", body.getMobilityDescriptors().get(0).getActivityQuestionNumber());
            assertNotNull(body.getMobilityNumberOfPoints());
            assertEquals(10, body.getMobilityNumberOfPoints().intValue());

        }

        // Daily living specific assertions
        assertEquals(false, body.isDailyLivingIsEntited());
        assertEquals(false, body.isDailyLivingIsSeverelyLimited());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());
    }

    @Test
    @Parameters(named = "previewEndDateAndRateCombinations")
    public void willSetPreviewFile_whenDailyLivingDescriptorsOnly_ForEndDateAndRate(String endDate, String rate, String descriptorsComparedToDwp,
        String nonDescriptorsComparedWithDwp) {

        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(descriptorsComparedToDwp);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(nonDescriptorsComparedWithDwp);

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        // Daily living specific params
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion(rate);
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("notConsidered");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(Arrays.asList("preparingFood"));
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = !"notConsidered".equalsIgnoreCase(rate) && "higher".equals(descriptorsComparedToDwp);

        boolean setAsideExpectation = getConsideredComparissons(rate, "notConsidered", descriptorsComparedToDwp, nonDescriptorsComparedWithDwp).stream().anyMatch(comparission ->
            !"same".equalsIgnoreCase(comparission));

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        // Daily living specific assertions
        if ("standardRate".equals(rate)) {
            assertEquals("standard rate", body.getDailyLivingAwardRate());
            assertEquals(false, body.isDailyLivingIsSeverelyLimited());
            assertEquals(true, body.isDailyLivingIsEntited());

        } else if ("enhancedRate".equals(rate)) {
            assertEquals("enhanced rate", body.getDailyLivingAwardRate());
            assertEquals(true, body.isDailyLivingIsSeverelyLimited());
            assertEquals(true, body.isDailyLivingIsEntited());

        } else {
            assertEquals(false, body.isDailyLivingIsEntited());
        }

        if ("notConsidered".equals(rate)) {
            assertNull(body.getDailyLivingDescriptors());
            assertNull(body.getDailyLivingNumberOfPoints());
        } else {
            assertNotNull(body.getDailyLivingDescriptors());
            assertEquals(1, body.getDailyLivingDescriptors().size());
            assertNotNull(body.getDailyLivingDescriptors().get(0));
            assertEquals(8, body.getDailyLivingDescriptors().get(0).getActivityAnswerPoints());
            assertEquals("f", body.getDailyLivingDescriptors().get(0).getActivityAnswerLetter());
            assertEquals("Cannot prepare and cook food.", body.getDailyLivingDescriptors().get(0).getActivityAnswerValue());
            assertEquals("Preparing food", body.getDailyLivingDescriptors().get(0).getActivityQuestionValue());
            assertEquals("1", body.getDailyLivingDescriptors().get(0).getActivityQuestionNumber());
            assertNotNull(body.getDailyLivingNumberOfPoints());
            assertEquals(8, body.getDailyLivingNumberOfPoints().intValue());

        }


        // Mobility specific assertions
        assertEquals(false, body.isMobilityIsEntited());
        assertEquals(false, body.isMobilityIsSeverelyLimited());
        assertNull(body.getMobilityDescriptors());
    }



    @Test
    public void willSetPreviewFileForDailyLivingMobility_whenNotGeneratingNotice() {

        setCommonPreviewParams(sscsCaseData, null);

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("no");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;
        boolean setAsideExpectation = appealAllowedExpectation;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, false, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, null, false);

        assertNull(body.getMobilityAwardRate());
        assertFalse(body.isMobilityIsSeverelyLimited());
        assertFalse(body.isMobilityIsEntited());
        assertNull(body.getDailyLivingAwardRate());
        assertFalse(body.isDailyLivingIsSeverelyLimited());
        assertFalse(body.isDailyLivingIsEntited());
        assertNull(body.getMobilityDescriptors());
        assertNull(body.getMobilityNumberOfPoints());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(body.getDailyLivingNumberOfPoints());

        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    @Test
    public void willSetPreviewFileForNotDailyLivingMobility_whenNotGeneratingNotice() {

        setCommonPreviewParams(sscsCaseData, null);

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("no");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("no");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = true;
        boolean setAsideExpectation = appealAllowedExpectation;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, false, false, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, null, false);

        assertNull(body.getMobilityAwardRate());
        assertFalse(body.isMobilityIsSeverelyLimited());
        assertFalse(body.isMobilityIsEntited());
        assertNull(body.getDailyLivingAwardRate());
        assertFalse(body.isDailyLivingIsSeverelyLimited());
        assertFalse(body.isDailyLivingIsEntited());
        assertNull(body.getMobilityDescriptors());
        assertNull(body.getMobilityNumberOfPoints());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(body.getDailyLivingNumberOfPoints());

        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    @Test
    @Parameters(named = "previewEndDateAndRateCombinations")
    public void willSetPreviewFile_whenMobilityDescriptorsOnly_ForEndDateAndRateForIssueDecision(String endDate, String rate, String descriptorsComparedToDwp,
        String nonDescriptorsComparedWithDwp) {

        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion(descriptorsComparedToDwp);
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(nonDescriptorsComparedWithDwp);

        // Mobility specific parameters
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion(rate);
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("notConsidered");
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(Arrays.asList("movingAround"));
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.FINAL_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Final Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        boolean appealAllowedExpectation = !"notConsidered".equalsIgnoreCase(rate) && "higher".equals(descriptorsComparedToDwp);

        boolean setAsideExpectation = getConsideredComparissons(rate, "notConsidered", descriptorsComparedToDwp, nonDescriptorsComparedWithDwp).stream().anyMatch(comparission ->
            !"same".equalsIgnoreCase(comparission));

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, false,
            true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DECISION NOTICE", payload.getNoticeType());

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        // Mobility specific assertions
        if ("standardRate".equals(rate)) {
            assertEquals("standard rate", body.getMobilityAwardRate());
            assertEquals(false, body.isMobilityIsSeverelyLimited());
            assertEquals(true, body.isMobilityIsEntited());
        } else if ("enhancedRate".equals(rate)) {
            assertEquals("enhanced rate", body.getMobilityAwardRate());
            assertEquals(true, body.isMobilityIsSeverelyLimited());
            assertEquals(true, body.isMobilityIsEntited());

        } else {
            assertEquals(false, body.isMobilityIsEntited());
        }

        if ("notConsidered".equals(rate)) {

            assertNull(body.getMobilityDescriptors());
            assertNull(body.getMobilityNumberOfPoints());

        } else {

            assertNotNull(body.getMobilityDescriptors());
            assertEquals(1, body.getMobilityDescriptors().size());
            assertNotNull(body.getMobilityDescriptors().get(0));
            assertEquals(10, body.getMobilityDescriptors().get(0).getActivityAnswerPoints());
            assertEquals("d", body.getMobilityDescriptors().get(0).getActivityAnswerLetter());
            assertEquals("Can stand and then move using an aid or appliance more than 20 metres but no more than 50 metres.", body.getMobilityDescriptors().get(0).getActivityAnswerValue());
            assertEquals("Moving around", body.getMobilityDescriptors().get(0).getActivityQuestionValue());
            assertEquals("12", body.getMobilityDescriptors().get(0).getActivityQuestionNumber());
            assertNotNull(body.getMobilityNumberOfPoints());
            assertEquals(10, body.getMobilityNumberOfPoints().intValue());

        }

        // Daily living specific assertions
        assertEquals(false, body.isDailyLivingIsEntited());
        assertEquals(false, body.isDailyLivingIsSeverelyLimited());
        assertNull(body.getDailyLivingDescriptors());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    @Test
    public void givenDateOfDecisionNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine date of decision", error);
    }

    @Test
    public void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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
            true, true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("venue 2 name", body.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",
            true, true, true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals("In chambers", body.getHeldAt());

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenCaseWithNullHearingsList_thenDefaultHearingData() {

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);


        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true,
            true,  true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals("In chambers", body.getHeldAt());

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithHearingDates_thenCorrectlySetTheHeldOnUsingTheFirstHearingInList() {

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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
            "2018-10-10", true, true, true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("2019-01-02", body.getHeldOn().toString());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDate_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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
    public void givenCaseWithTwoPanelMembers_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        sscsCaseData.setWriteFinalDecisionDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        sscsCaseData.setWriteFinalDecisionMedicallyQualifiedPanelMemberName("Ms Panel Member 2");

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
            true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1 and Ms Panel Member 2", body.getHeldBefore());
    }

    @Test
    public void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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
            true, true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);


        assertEquals("Judge Full Name and Mr Panel Member 1", body.getHeldBefore());

    }

    @Test
    public void givenCaseWithNoPanelMembersWithNullValues_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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
            "2018-10-10", true,true, true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());

    }

    @Test
    public void givenCaseWithNoPanelMembersWithEmptyValues_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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
            true,true, true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));
        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());

    }

    @Test
    public void scottishRpcWillShowAScottishImage() {

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        verifyTemplateBody(NoticeIssuedTemplateBody.SCOTTISH_IMAGE, "Appellant Lastname", null,
            "2018-10-10", true, true, true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));
    }

    @Test
    public void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppendedAndAppointeeFullNameSet() {

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
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
            true, true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));
    }

    @Test
    public void givenDateIssuedParameterIsTrue_thenShowIssuedDateOnDocument() {
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true,
            true, true, true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now(), payload.getDateIssued());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetGeneratedDescriptorFlow_thenSetNewGeneratedDate() {
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true, true, true,
            true, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetGeneratedNonDescriptorFlow_thenSetNewGeneratedDate() {
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("no");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true, true, true, false, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetNonGeneratedDescriptorFlow_thenDoSetNewGeneratedDate() {
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("no");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10", true, true, true,
            true, false, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetNonGeneratedNonDescriptorFlow_thenDoSetNewGeneratedDate() {
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("no");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("no");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",  true, true, true,
            false, true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Test
    public void givenWelsh_GeneratedDateIsAlreadySet_thenDoNotSetNewGeneratedDate() {
        sscsCaseData.setLanguagePreferenceWelsh("yes");
        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("no");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("no");
        sscsCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-10-10");

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", null, "2018-10-10",  true, true, true,
                false, true, documentConfiguration.getDocuments().get(LanguagePreference.WELSH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    private NoticeIssuedTemplateBody verifyTemplateBody(String image, String expectedName, String expectedAppointeeName, String dateOfDecision, boolean allowed, boolean isSetAside, boolean isDraft,
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
        assertEquals(isDescriptorFlow, body.isDescriptorFlow());
        assertEquals(templateId, generateFileParams.getTemplateId());

        return payload;
    }
}
