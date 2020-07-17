package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;

@RunWith(JUnitParamsRunner.class)
public class AdjournCasePreviewServiceTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String TEMPLATE_ID = "nuts.docx";
    private static final String URL = "http://dm-store/documents/123";
    private AdjournCasePreviewService service;

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

    private ArgumentCaptor<GenerateFileParams> capture;

    private SscsCaseData sscsCaseData;

    private VenueDataLoader venueDataLoader;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        this.venueDataLoader = new VenueDataLoader();
        service = new AdjournCasePreviewService(generateFile, idamClient,
            venueDataLoader, TEMPLATE_ID);

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
        return new Object[] {
            new String[] {"2018-11-10", "standardRate", "lower", "lower"},
            new String[] {"2018-11-10", "standardRate", "same", "lower"},
            new String[] {"2018-11-10", "standardRate", "higher", "lower"},
            new String[] {"2018-11-10", "standardRate", "lower", "same"},
            new String[] {"2018-11-10", "standardRate", "same", "same"},
            new String[] {"2018-11-10", "standardRate", "higher", "same"},
            new String[] {"2018-11-10", "enhancedRate", "same", "lower"},
            new String[] {"2018-11-10", "enhancedRate", "higher", "lower"},
            new String[] {"2018-11-10", "enhancedRate", "same", "same"},
            new String[] {"2018-11-10", "enhancedRate", "higher", "same"},
            new String[] {"2018-11-10", "noAward", "lower", "lower"},
            new String[] {"2018-11-10", "noAward", "same", "lower"},
            new String[] {"2018-11-10", "noAward", "lower", "same"},
            new String[] {"2018-11-10", "noAward", "same", "same"},
            new String[] {"2018-11-10", "notConsidered", "lower", "lower"},
            new String[] {"2018-11-10", "notConsidered", "same", "lower"},
            new String[] {"2018-11-10", "notConsidered", "higher", "lower"},
            new String[] {"2018-11-10", "notConsidered", "lower", "same"},
            new String[] {"2018-11-10", "notConsidered", "same", "same"},
            new String[] {"2018-11-10", "notConsidered", "higher", "same"},
            new String[] {null, "standardRate", "lower", "lower"},
            new String[] {null, "standardRate", "same", "lower"},
            new String[] {null, "standardRate", "higher", "lower"},
            new String[] {null, "standardRate", "lower", "same"},
            new String[] {null, "standardRate", "same", "same"},
            new String[] {null, "standardRate", "higher", "same"},
            new String[] {null, "enhancedRate", "same", "lower"},
            new String[] {null, "enhancedRate", "higher", "lower"},
            new String[] {null, "enhancedRate", "same", "same"},
            new String[] {null, "enhancedRate", "higher", "same"},
            new String[] {null, "noAward", "lower", "lower"},
            new String[] {null, "noAward", "same", "lower"},
            new String[] {null, "noAward", "lower", "same"},
            new String[] {null, "noAward", "same", "same"},
            new String[] {null, "notConsidered", "lower", "lower"},
            new String[] {null, "notConsidered", "same", "lower"},
            new String[] {null, "notConsidered", "higher", "lower"},
            new String[] {null, "notConsidered", "lower", "same"},
            new String[] {null, "notConsidered", "same", "same"},
            new String[] {null, "notConsidered", "higher", "same"},

        };
    }

    private void setCommonPreviewParams(SscsCaseData sscsCaseData, String endDate) {
        sscsCaseData.setAdjournCaseReasons(Arrays.asList(new CollectionItem<>(null, "My reasons for decision")));
        sscsCaseData.setAdjournCaseAnythingElse("Something else.");
        sscsCaseData.setAdjournCaseTypeOfHearing("faceToFace");
        //sscsCaseData.setAdjournCaseTypeOfNextHearing("telephone");
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

    }

    private void assertCommonPreviewParams(AdjournCaseTemplateBody body, String endDate, boolean nullReasonsExpected) {
        //  assertEquals("2018-10-11",body.getStartDate());
        //  assertEquals(endDate,body.getEndDate());
        //  assertEquals("2018-10-10", body.getDateOfDecision());
        // assertEquals(endDate == null, body.isIndefinite());
        if (nullReasonsExpected) {
            assertNull(body.getReasonsForDecision());
        } else {
            assertNotNull(body.getReasonsForDecision());
            assertFalse(body.getReasonsForDecision().isEmpty());
            assertEquals("My reasons for decision", body.getReasonsForDecision().get(0));
        }
        assertEquals("Something else.", body.getAnythingElse());
        assertEquals("faceToFace", body.getHearingType());
        //assertEquals("A1", body.getPageNumber());
        // assertTrue(body.isAttendedHearing());
        // assertFalse(body.isPresentingOfficerAttended());
    }

    @Test
    public void willSetPreviewFileWithNullReasons_WhenReasonsListIsEmpty() {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.setAdjournCaseReasons(new ArrayList<>());

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, true);

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

    /*

    @Test
    @Parameters(named = "previewEndDateAndRateCombinations")
    public void willSetPreviewFile_whenMobilityDescriptorsOnly_ForEndDateAndRate(String endDate, String rate, String descriptorsComparedToDwp,
        String nonDescriptorsComparedWithDwp) {

        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setAdjournCaseIsDescriptorFlow("yes");
        sscsCaseData.setAdjournCaseGenerateNotice("yes");

        sscsCaseData.setPipAdjournCaseComparedToDwpMobilityQuestion(descriptorsComparedToDwp);
        sscsCaseData.setPipAdjournCaseComparedToDwpDailyLivingQuestion(nonDescriptorsComparedWithDwp);

        // Mobility specific parameters
        sscsCaseData.setPipAdjournCaseMobilityQuestion(rate);
        sscsCaseData.setPipAdjournCaseDailyLivingQuestion("notConsidered");
        sscsCaseData.setPipAdjournCaseMobilityActivitiesQuestion(Arrays.asList("movingAround"));
        sscsCaseData.setPipAdjournCaseMovingAroundQuestion("movingAround12d");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        boolean appealAllowedExpectation = !"notConsidered".equalsIgnoreCase(rate) && "higher".equals(descriptorsComparedToDwp);

        boolean setAsideExpectation = getConsideredComparissons(rate, "notConsidered", descriptorsComparedToDwp, nonDescriptorsComparedWithDwp).stream().anyMatch(comparission ->
            !"same".equalsIgnoreCase(comparission));

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true);

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

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
    @Parameters(named = "previewEndDateAndRateCombinations")
    public void willSetPreviewFile_whenDailyLivingDescriptorsOnly_ForEndDateAndRate(String endDate, String rate, String descriptorsComparedToDwp,
        String nonDescriptorsComparedWithDwp) {

        setCommonPreviewParams(sscsCaseData, endDate);

        sscsCaseData.setPipAdjournCaseComparedToDwpDailyLivingQuestion(descriptorsComparedToDwp);
        sscsCaseData.setPipAdjournCaseComparedToDwpMobilityQuestion(nonDescriptorsComparedWithDwp);

        sscsCaseData.setAdjournCaseIsDescriptorFlow("yes");
        sscsCaseData.setAdjournCaseGenerateNotice("yes");

        // Daily living specific params
        sscsCaseData.setPipAdjournCaseDailyLivingQuestion(rate);
        sscsCaseData.setPipAdjournCaseMobilityQuestion("notConsidered");
        sscsCaseData.setPipAdjournCaseDailyLivingActivitiesQuestion(Arrays.asList("preparingFood"));
        sscsCaseData.setPipAdjournCasePreparingFoodQuestion("preparingFood1f");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        boolean appealAllowedExpectation = !"notConsidered".equalsIgnoreCase(rate) && "higher".equals(descriptorsComparedToDwp);

        boolean setAsideExpectation = getConsideredComparissons(rate, "notConsidered", descriptorsComparedToDwp, nonDescriptorsComparedWithDwp).stream().anyMatch(comparission ->
            !"same".equalsIgnoreCase(comparission));

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, true);

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

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

        sscsCaseData.setAdjournCaseIsDescriptorFlow("yes");
        sscsCaseData.setAdjournCaseGenerateNotice("no");
        sscsCaseData.setAdjournCaseAllowedOrRefused("allowed");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        boolean appealAllowedExpectation = true;
        boolean setAsideExpectation = appealAllowedExpectation;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, true, false);

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

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

        sscsCaseData.setAdjournCaseIsDescriptorFlow("no");
        sscsCaseData.setAdjournCaseGenerateNotice("no");
        sscsCaseData.setAdjournCaseAllowedOrRefused("allowed");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        boolean appealAllowedExpectation = true;
        boolean setAsideExpectation = appealAllowedExpectation;

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, true, false, false);

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

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

        sscsCaseData.setAdjournCaseIsDescriptorFlow("yes");
        sscsCaseData.setAdjournCaseGenerateNotice("yes");

        sscsCaseData.setPipAdjournCaseComparedToDwpMobilityQuestion(descriptorsComparedToDwp);
        sscsCaseData.setPipAdjournCaseComparedToDwpDailyLivingQuestion(nonDescriptorsComparedWithDwp);

        // Mobility specific parameters
        sscsCaseData.setPipAdjournCaseMobilityQuestion(rate);
        sscsCaseData.setPipAdjournCaseDailyLivingQuestion("notConsidered");
        sscsCaseData.setPipAdjournCaseMobilityActivitiesQuestion(Arrays.asList("movingAround"));
        sscsCaseData.setPipAdjournCaseMovingAroundQuestion("movingAround12d");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.FINAL_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Final Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        boolean appealAllowedExpectation = !"notConsidered".equalsIgnoreCase(rate) && "higher".equals(descriptorsComparedToDwp);

        boolean setAsideExpectation = getConsideredComparissons(rate, "notConsidered", descriptorsComparedToDwp, nonDescriptorsComparedWithDwp).stream().anyMatch(comparission ->
            !"same".equalsIgnoreCase(comparission));

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10",
            appealAllowedExpectation, setAsideExpectation, false,
            true, true);

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DECISION NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

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

        sscsCaseData.setAdjournCaseIsDescriptorFlow("yes");
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setPipAdjournCaseComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipAdjournCaseComparedToDwpMobilityQuestion("higher");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournCasePreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine date of decision", error);
    }

    @Test
    public void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseIsDescriptorFlow("yes");
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setPipAdjournCaseComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipAdjournCaseComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setAdjournCaseDateOfDecision("2018-10-10");
        when(userDetails.getFullName()).thenReturn(null);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user name", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void givenSignedInJudgeUserDetailsNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseIsDescriptorFlow("yes");
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setPipAdjournCaseComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipAdjournCaseComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setAdjournCaseDateOfDecision("2018-10-10");
        when(idamClient.getUserDetails("Bearer token")).thenReturn(null);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void givenComparedToDwpMobilityQuestionNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseIsDescriptorFlow("yes");
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setPipAdjournCaseComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setAdjournCaseDateOfDecision("2018-10-10");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void givenComparedToDwpDailyLivingSetIncorrectly_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseIsDescriptorFlow("yes");
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setPipAdjournCaseComparedToDwpDailyLivingQuestion("someValue");
        sscsCaseData.setPipAdjournCaseComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setAdjournCaseDateOfDecision("2018-10-10");


        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void givenComparedToDwpMobilityQuestionSetIncorrectly_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseIsDescriptorFlow("yes");
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setPipAdjournCaseComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipAdjournCaseComparedToDwpMobilityQuestion("someValue");
        sscsCaseData.setAdjournCaseDateOfDecision("2018-10-10");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournCasePreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
    }
    */

    @Test
    public void givenGenerateNoticeNotSet_willNotSetPreviewFile() {

        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithVenues_thenCorrectlySetHeldAtUsingTheFirstHearingInList() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("venue 2 name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);
        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("venue 2 name", body.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing venue", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenue_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing venue", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayAnErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = null;

        sscsCaseData.setHearings(Arrays.asList(hearing2, hearing1));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDetails_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());

    }

    @Test
    public void givenCaseWithEmptyHearingsList_thenDefaultHearingData() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals("In chambers", body.getHeldAt());

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void givenCaseWithNullHearingsList_thenDefaultHearingData() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals("In chambers", body.getHeldAt());

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithHearingDates_thenCorrectlySetTheHeldOnUsingTheFirstHearingInList() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("Venue Name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("2019-01-02", body.getHeldOn().toString());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDate_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("Venue Name").build())
            .build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayTwoErrorsAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01")
            .venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = null;

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournCasePreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
        assertNull(response.getData().getAdjournCasePreviewDocument());

    }

    @Test
    public void givenCaseWithTwoPanelMembers_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        sscsCaseData.setAdjournCaseDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        sscsCaseData.setAdjournCaseMedicallyQualifiedPanelMemberName("Ms Panel Member 2");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1 and Ms Panel Member 2", body.getHeldBefore());

    }

    @Test
    public void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        sscsCaseData.setAdjournCaseDisabilityQualifiedPanelMemberName("Mr Panel Member 1");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name and Mr Panel Member 1", body.getHeldBefore());

    }

    @Test
    public void givenCaseWithNoPanelMembersWithNullValues_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseGenerateNotice("yes");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());

    }

    @Test
    public void givenCaseWithNoPanelMembersWithEmptyValues_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseMedicallyQualifiedPanelMemberName("");
        sscsCaseData.setAdjournCaseDisabilityQualifiedPanelMemberName("");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());

    }

    @Test
    public void scottishRpcWillShowAScottishImage() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        verifyTemplateBody(NoticeIssuedTemplateBody.SCOTTISH_IMAGE, "Appellant Lastname", true);
    }

    @Test
    public void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppended() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.getAppeal().getAppellant().setIsAppointee("Yes");
        sscsCaseData.getAppeal().getAppellant().setAppointee(Appointee.builder()
            .name(Name.builder().firstName("APPOINTEE")
                .lastName("SurNamE")
                .build())
            .identity(Identity.builder().build())
            .build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appointee Surname, appointee for Appellant Lastname", true);
    }

    @Test
    public void givenDateIssuedParameterIsTrue_thenShowIssuedDateOnDocument() {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        assertEquals(LocalDate.now(), payload.getDateIssued());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetGeneratedDescriptorFlow_thenDoNotSetNewGeneratedDate() {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseGeneratedDate("2018-10-10");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        assertEquals("2018-10-10", payload.getGeneratedDate().toString());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetGeneratedNonDescriptorFlow_thenDoNotSetNewGeneratedDate() {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseGeneratedDate("2018-10-10");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        assertEquals("2018-10-10", payload.getGeneratedDate().toString());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetNonGeneratedDescriptorFlow_thenDoNotSetNewGeneratedDate() {
        sscsCaseData.setAdjournCaseGenerateNotice("no");
        sscsCaseData.setAdjournCaseGeneratedDate("2018-10-10");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        assertEquals("2018-10-10", payload.getGeneratedDate().toString());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetNonGeneratedNonDescriptorFlow_thenDoNotSetNewGeneratedDate() {
        sscsCaseData.setAdjournCaseGenerateNotice("no");
        sscsCaseData.setAdjournCaseGeneratedDate("2018-10-10");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", true);

        assertEquals("2018-10-10", payload.getGeneratedDate().toString());
    }


    private NoticeIssuedTemplateBody verifyTemplateBody(String image, String expectedName, boolean isDraft) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        assertEquals(image, payload.getImage());
        if (isDraft) {
            assertEquals("DRAFT ADJOURNMENT NOTICE", payload.getNoticeType());
        } else {
            assertEquals("ADJOURNMENT NOTICE", payload.getNoticeType());
        }
        assertEquals(expectedName, payload.getAppellantFullName());
        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);
        /*
        assertEquals(dateOfDecision, body.getDateOfDecision());
        assertEquals(allowed, body.isAllowed());
        assertEquals(isSetAside, body.isSetAside());
        assertNull(body.getDetailsOfDecision());
        assertEquals(isDescriptorFlow, body.isDescriptorFlow());

         */

        return payload;
    }
}
