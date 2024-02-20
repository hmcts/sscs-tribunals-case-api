package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.IN_CHAMBERS;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@RunWith(JUnitParamsRunner.class)
public abstract class WriteFinalDecisionPreviewDecisionServiceTestBase {
    protected static final String ORIGINAL_HELD_AT = "held at testtest";
    protected static final String ORIGINAL_JUDGE_NAME = "test judge name name";
    protected static final String USER_AUTHORISATION = "Bearer token";
    protected static final String URL = "http://dm-store/documents/123";
    public static final String APPELLANT_LAST_NAME = "APPELLANT Last'NamE";
    protected WriteFinalDecisionPreviewDecisionServiceBase service;
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
    protected UserDetailsService userDetailsService;

    @Spy
    protected DocumentConfiguration documentConfiguration;

    protected ArgumentCaptor<GenerateFileParams> capture;

    protected SscsCaseData sscsCaseData;

    protected static String APPEllANT_NAME = "Appellant Lastname";
    protected static String DATE_OF_DECISION = "2018-10-10";


    protected abstract WriteFinalDecisionPreviewDecisionServiceBase createPreviewDecisionService(GenerateFile generateFile, UserDetailsService userDetailsService,
        DocumentConfiguration documentConfiguration);

    protected abstract Map<LanguagePreference, Map<EventType, String>> getBenefitSpecificDocuments();

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        final Map<EventType, String> englishEventTypeDocs = new HashMap<>();
        englishEventTypeDocs.put(EventType.CORRECTION_GRANTED, "TB-SCS-GNO-ENG-corrected-decision-notice.docx");
        englishEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-ENG-directions-notice.docx");
        englishEventTypeDocs.put(EventType.DECISION_ISSUED, "TB-SCS-GNO-ENG-draft-decision-notice.docx");
        Map<EventType, String> welshEventTypeDocs = new HashMap<>();
        welshEventTypeDocs.put(EventType.CORRECTION_GRANTED, "TB-SCS-GNO-ENG-corrected-decision-notice.docx");
        welshEventTypeDocs.put(EventType.DIRECTION_ISSUED, "TB-SCS-GNO-WEL-00485.docx");
        welshEventTypeDocs.put(EventType.DECISION_ISSUED, "TB-SCS-GNO-WEL-00485.docx");
        final Map<LanguagePreference, Map<EventType, String>> documents =  new HashMap<>();
        documents.put(LanguagePreference.ENGLISH, englishEventTypeDocs);
        documents.put(LanguagePreference.WELSH, welshEventTypeDocs);

        documentConfiguration.setDocuments(documents);

        documentConfiguration.setDocuments(getBenefitSpecificDocuments());

        service = createPreviewDecisionService(generateFile, userDetailsService,
            documentConfiguration);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(userDetailsService.buildLoggedInUserName("Bearer token")).thenReturn("Judge Full Name");

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(benefitType).build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("APPELLANT")
                        .lastName("Last'NamE")
                        .build())
                    .identity(Identity.builder().build())
                    .build())
                .build()).build();


        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);

        when(generateFile.assemble(any())).thenReturn(URL);
    }

    protected void setCommonPreviewParams(SscsCaseData sscsCaseData, String endDate) {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionStartDate("2018-10-11");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDate(endDate);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionReasons(Arrays.asList(new CollectionItem<>(null, "My reasons for decision")));
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAnythingElse("Something else.");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionTypeOfHearing("telephone");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPageSectionReference("A1");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAppellantAttendedQuestion("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAppointeeAttendedQuestion("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPresentingOfficerAttendedQuestion("No");
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));

    }

    protected void setCommonNonDescriptorRoutePreviewParams(SscsCaseData sscsCaseData) {
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionReasons(Arrays.asList(new CollectionItem<>(null, "My reasons for decision")));
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAnythingElse("Something else.");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionTypeOfHearing("telephone");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPageSectionReference("A1");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAppellantAttendedQuestion("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAppointeeAttendedQuestion("Yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPresentingOfficerAttendedQuestion("No");
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));

    }

    protected void assertCommonPreviewParams(WriteFinalDecisionTemplateBody body, String endDate, boolean nullReasonsExpected) {
        assertEquals("2018-10-11",body.getStartDate());
        assertEquals(endDate,body.getEndDate());
        assertEquals(DATE_OF_DECISION, body.getDateOfDecision());
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
        assertNull(body.getOtherPartyNamesAttendedHearing());
        assertFalse(body.isPresentingOfficerAttended());
    }

    protected void assertCommonNonDescriptorFlowPreviewParams(WriteFinalDecisionTemplateBody body, boolean nullReasonsExpected) {
        assertNull(body.getStartDate());
        assertNull(body.getEndDate());
        assertEquals(DATE_OF_DECISION, body.getDateOfDecision());
        assertTrue(body.isIndefinite());
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
        assertNull(body.getOtherPartyNamesAttendedHearing());
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

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(null);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine date of decision", error);
    }

    @Test
    public void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        assertNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenSignedInJudgeUserDetailsNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        assertNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenComparedToDwpMobilityQuestionNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
        assertNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenComparedToDwpDailyLivingSetIncorrectly_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("someValue");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);


        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
        assertNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenComparedToDwpMobilityQuestionSetIncorrectly_thenDisplayErrorAndDoNotGenerateDocument() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("someValue");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
    }

    @Test
    public void givenGenerateNoticeNotSet_willNotSetPreviewFile() {

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithVenues_thenCorrectlySetHeldAtUsingTheFirstHearingInList() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").address(Address.builder().postcode("postcode").build()).build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("venue 2 name").address(Address.builder().postcode("postcode").build()).build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, DATE_OF_DECISION, true,
            true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("venue 2 name", body.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenCorrectlySetHeldAtUsingTheFirstHearingInList() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "Yes" : "No", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").address(Address.builder().postcode("postcode").build()).build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().address(Address.builder().postcode("postcode").build()).build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true,
            true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("venue 1 name", body.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenue_thenCorrectlySetHeldAtUsingTheSecondHearingInList() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes"  : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").address(Address.builder().postcode("postcode").build()).build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true,
            true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("venue 1 name", body.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenCorrectlySetHeldAtUsingTheSecondHearingInList() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder().hearingDate(DATE_OF_DECISION)
            .venue(Venue.builder().name("venue 1 name").address(Address.builder().postcode("postcode").build()).build()).build()).build();

        Hearing hearing2 = null;

        sscsCaseData.setHearings(Arrays.asList(hearing2, hearing1));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true,
            true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("venue 1 name", body.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDetails_thenCorrectlySetHeldAtUsingTheSecondHearingInList() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder().hearingDate(DATE_OF_DECISION)
                .venue(Venue.builder().name("venue 1 name").address(Address.builder().postcode("postcode").build()).build()).build()).build();

        Hearing hearing2 = Hearing.builder().build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true,
            true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("venue 1 name", body.getHeldAt());

    }

    @Test
    public void givenCaseWithEmptyHearingsList_thenDefaultHearingData() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10",
            true, true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals(IN_CHAMBERS, body.getHeldAt());

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenCaseWithNullHearingsList_thenDefaultHearingData() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.setHearings(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);


        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true,
            true,  true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals(IN_CHAMBERS, body.getHeldAt());

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithHearingDates_thenCorrectlySetTheHeldOnUsingTheFirstHearingInList() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null,
            "2018-10-10", true, true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("2019-01-02", body.getHeldOn().toString());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDate_thenCorrectlySetHeldOnUsingTheSecondHearingInList() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build())
            .build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null,
            "2018-10-10", true, true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("2019-01-01", body.getHeldOn().toString());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenUseTheSecondHearing() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDisabilityQualifiedPanelMemberName("Mr Panel Member 1");

        sscsCaseData.setHearings(Arrays.asList(null, Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true,
            true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);


        assertEquals("Judge Full Name and Mr Panel Member 1", body.getHeldBefore());

    }

    @Test
    public void givenCaseWithMultiplePanelMembers_thenCorrectlySetTheHeldBefore() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionMedicallyQualifiedPanelMemberName("Ms Panel Member 2");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionOtherPanelMemberName("Miss other");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true, true, true,
            isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1, Ms Panel Member 2 and Miss other", body.getHeldBefore());
    }

    @Test
    public void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDisabilityQualifiedPanelMemberName("Mr Panel Member 1");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true,
            true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);


        assertEquals("Judge Full Name and Mr Panel Member 1", body.getHeldBefore());
    }

    @Test
    public void givenCaseWithNoPanelMembersWithNullValues_thenCorrectlySetTheHeldBefore() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);


        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null,
            "2018-10-10", true,true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());

    }

    @Test
    public void givenValidCase_WhenNoTemplatesForEnglishLanguageRegisteredThenDisplayError() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        documentConfiguration.getDocuments().clear();


        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        assertEquals(1, response.getErrors().size());

        assertEquals("Unable to obtain benefit specific documents for language:ENGLISH", response.getErrors().iterator().next());
    }

    @Test
    public void givenValidCase_WhenNoTemplatesForEventRegisteredThenDisplayError() {
        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).clear();

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        assertEquals(1, response.getErrors().size());

        assertEquals("Unable to obtain template id for ISSUE_FINAL_DECISION and language:ENGLISH", response.getErrors().iterator().next());
    }

    @Test
    public void givenCaseWithNoPanelMembersWithEmptyValues_thenCorrectlySetTheHeldBefore() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionMedicallyQualifiedPanelMemberName("");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDisabilityQualifiedPanelMemberName("");


        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10",
            true,true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));
        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());

    }

    @Test
    public void scottishRpcWillShowAScottishImage() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        verifyTemplateBody(NoticeIssuedTemplateBody.SCOTTISH_IMAGE, APPELLANT_LAST_NAME, null,
            "2018-10-10", true, true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));
    }

    protected abstract void setDescriptorFlowIndicator(String value, SscsCaseData sscsCaseData);

    protected abstract boolean getDescriptorFlowIndicator(WriteFinalDecisionTemplateBody body);

    protected abstract void setHigherRateScenarioFields(SscsCaseData caseData);

    @Test
    public void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppendedAndAppointeeFullNameSet() {

        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);
        sscsCaseData.getAppeal().getAppellant().setIsAppointee("Yes");
        sscsCaseData.getAppeal().getAppellant().setAppointee(Appointee.builder()
            .name(Name.builder().firstName("APPOINTEE")
                .lastName("Sur-NamE")
                .build())
            .identity(Identity.builder().build())
            .build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").address(Address.builder().postcode("postcode").build()).build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, false);

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "APPOINTEE Sur-NamE, appointee for APPELLANT Last'NamE", "APPOINTEE Sur-NamE", "2018-10-10", true,
            true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));
    }

    @Test
    public void givenDateIssuedParameterIsTrue_thenShowIssuedDateOnDocument() {
        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true,
            true, true, isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now(), payload.getDateIssued());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetGeneratedDescriptorFlow_thenSetNewGeneratedDate() {
        setDescriptorFlowIndicator(isDescriptorFlowSupported() ? "yes" : "no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        setHigherRateScenarioFields(sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGeneratedDate(DATE_OF_DECISION);

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10", true, true, true,
            isDescriptorFlowSupported(), true, documentConfiguration.getDocuments().get(LanguagePreference.ENGLISH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Test
    public void givenPostHearingIsEnabledAndFinalHeldAtHasNotBeenSet_thenDontUpdateHeldAt() {
        when(caseDetails.getState()).thenReturn(State.POST_HEARING);
        sscsCaseData.getSscsFinalDecisionCaseData().setFinalDecisionHeldAt(null);
        PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.CORRECTED_DECISION_NOTICE, USER_AUTHORISATION, false, true, true);

        assertNull(response.getData().getSscsFinalDecisionCaseData().getFinalDecisionHeldAt());
    }

    @Test
    public void givenPostHearingIsEnabledAndFinalHeldAtHasBeenSet_thenDontUpdateFinalDecisionHeldAt() {
        when(caseDetails.getState()).thenReturn(State.POST_HEARING);
        sscsCaseData.getSscsFinalDecisionCaseData().setFinalDecisionHeldAt(ORIGINAL_HELD_AT);
        PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.CORRECTED_DECISION_NOTICE, USER_AUTHORISATION, false, true, true);

        assertEquals(ORIGINAL_HELD_AT, response.getData().getSscsFinalDecisionCaseData().getFinalDecisionHeldAt());
    }

    @Test
    public void givenFinalDecisionJudgeIsSet_thenDontGetSignedInJudgeName() {
        when(caseDetails.getState()).thenReturn(State.POST_HEARING);
        sscsCaseData.getSscsFinalDecisionCaseData().setFinalDecisionJudge(ORIGINAL_JUDGE_NAME);
        PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.CORRECTED_DECISION_NOTICE, USER_AUTHORISATION, false, true, true);

        verify(userDetailsService, atMostOnce()).buildLoggedInUserName(USER_AUTHORISATION);
        assertEquals(ORIGINAL_JUDGE_NAME, response.getData().getSscsFinalDecisionCaseData().getFinalDecisionJudge());
    }

    @Test
    public abstract void givenGeneratedDateIsAlreadySetGeneratedNonDescriptorFlow_thenSetNewGeneratedDate();

    @Test
    public abstract void givenGeneratedDateIsAlreadySetNonGeneratedDescriptorFlow_thenDoSetNewGeneratedDate();

    @Test
    public abstract void givenGeneratedDateIsAlreadySetNonGeneratedNonDescriptorFlow_thenDoSetNewGeneratedDate();

    @Test
    public void givenWelsh_GeneratedDateIsAlreadySet_thenDoNotSetNewGeneratedDate() {
        sscsCaseData.setLanguagePreferenceWelsh("yes");
        setDescriptorFlowIndicator("no", sscsCaseData);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(NO);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(DATE_OF_DECISION);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGeneratedDate(DATE_OF_DECISION);

        service.preview(callback, DocumentType.DRAFT_DECISION_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_LAST_NAME, null, "2018-10-10",  true, true, true,
            false, true, documentConfiguration.getDocuments().get(LanguagePreference.WELSH).get(EventType.ISSUE_FINAL_DECISION));

        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    protected abstract boolean isDescriptorFlowSupported();

    protected NoticeIssuedTemplateBody verifyTemplateBody(String image, String expectedName, String expectedAppointeeName, String dateOfDecision, boolean allowed, boolean isSetAside, boolean isDraft,
                                                          boolean isDescriptorFlow, boolean isGenerateFile, String templateId) {
        return verifyTemplateBody(image, expectedName, expectedAppointeeName, dateOfDecision, allowed, isSetAside, isDraft, isDescriptorFlow, isGenerateFile, false, templateId);
    }

    protected NoticeIssuedTemplateBody verifyTemplateBody(String image, String expectedName, String expectedAppointeeName, String dateOfDecision, boolean allowed, boolean isSetAside, boolean isDraft,
        boolean isDescriptorFlow, boolean isGenerateFile, boolean isCorrection, String templateId) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        GenerateFileParams generateFileParams = capture.getValue();
        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) generateFileParams.getFormPayload();
        assertEquals(image, payload.getImage());
        if (isCorrection) {
            assertEquals(DocumentType.CORRECTION_GRANTED.getLabel().toUpperCase(), payload.getNoticeType());
        } else if (isDraft) {
            assertEquals(DocumentType.DRAFT_DECISION_NOTICE.getLabel().toUpperCase(), payload.getNoticeType());
        } else {
            assertEquals(DocumentType.DECISION_NOTICE.getLabel().toUpperCase(), payload.getNoticeType());
        }
        assertEquals(expectedName, payload.getAppellantFullName());
        assertEquals(expectedAppointeeName, payload.getAppointeeFullName());
        WriteFinalDecisionTemplateBody body = payload.getWriteFinalDecisionTemplateBody();
        assertNotNull(body);
        assertEquals(APPELLANT_LAST_NAME, body.getAppellantName());
        assertEquals(dateOfDecision, body.getDateOfDecision());
        assertEquals(allowed, body.isAllowed());
        assertEquals(isSetAside, body.isSetAside());
        assertNull(body.getDetailsOfDecision());
        assertEquals(isDescriptorFlow, getDescriptorFlowIndicator(body));
        assertEquals(templateId, generateFileParams.getTemplateId());

        return payload;
    }
}
