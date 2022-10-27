package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

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
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.service.LanguageService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
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
    private UserDetailsService userDetailsService;

    private ArgumentCaptor<GenerateFileParams> capture;

    private SscsCaseData sscsCaseData;

    @Mock
    private VenueDataLoader venueDataLoader;

    @Mock
    private SignLanguagesService signLanguagesService;

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        service = new AdjournCasePreviewService(generateFile, userDetailsService,
            venueDataLoader, new LanguageService(), TEMPLATE_ID, signLanguagesService);

        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(userDetailsService.buildLoggedInUserName("Bearer token")).thenReturn("Judge Full Name");

        Map<String, VenueDetails> venueDetailsMap = new HashMap<>();
        VenueDetails venueDetails = VenueDetails.builder().venName("Venue Name").gapsVenName("Gap venue name").build();
        venueDetailsMap.put("someVenueId", venueDetails);

        when(venueDataLoader.getVenueDetailsMap()).thenReturn(venueDetailsMap);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn("Gap venue name");

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
            .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
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

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);

        when(generateFile.assemble(any())).thenReturn(URL);
    }


    @NamedParameters("allNextHearingTypeParameters")
    @SuppressWarnings("unused")
    private Object[] allNextHearingTypeParameters() {
        return new Object[] {
            new String[] {"faceToFace", "face to face hearing"},
            new String[] {"telephone", "telephone hearing"},
            new String[] {"video", "video hearing"},
            new String[] {"paper", "decision on the papers"},
        };
    }

    @NamedParameters("nonFaceToFaceNextHearingTypeParameters")
    @SuppressWarnings("unused")
    private Object[] nonFaceToFaceNextHearingTypeParameters() {
        return new Object[] {
            new String[] {"telephone", "telephone hearing"},
            new String[] {"video", "video hearing"},
            new String[] {"paper", "decision on the papers"},
        };
    }

    @NamedParameters("paperNextHearingTypeParameters")
    @SuppressWarnings("unused")
    private Object[] paperNextHearingTypeParameters() {
        return new Object[] {
            new String[] {"paper", "decision on the papers"},
        };
    }

    @NamedParameters("oralNextHearingTypeParameters")
    @SuppressWarnings("unused")
    private Object[] oralNextHearingTypeParameters() {
        return new Object[] {
            new String[] {"faceToFace", "face to face hearing"},
            new String[] {"telephone", "telephone hearing"},
            new String[] {"video", "video hearing"},
        };
    }

    @NamedParameters("faceToFaceNextHearingTypeParameter")
    @SuppressWarnings("unused")
    private Object[] faceToFaceNextHearingTypeParameter() {
        return new Object[] {
            new String[] {"faceToFace", "face to face hearing"},
        };
    }

    private void setCommonPreviewParams(SscsCaseData sscsCaseData, String endDate) {
        sscsCaseData.setAdjournCaseReasons(Arrays.asList(new CollectionItem<>(null, "My reasons for decision")));
        sscsCaseData.setAdjournCaseAdditionalDirections(Arrays.asList(new CollectionItem<>(null, "Something else.")));
        sscsCaseData.setAdjournCaseTypeOfHearing("faceToFace");
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).venueId("someVenueId").build()).build()));

    }

    private void assertCommonPreviewParams(AdjournCaseTemplateBody body, String endDate, boolean nullReasonsExpected) {

        if (nullReasonsExpected) {
            assertNull(body.getReasonsForDecision());
        } else {
            assertNotNull(body.getReasonsForDecision());
            assertFalse(body.getReasonsForDecision().isEmpty());
            assertEquals("My reasons for decision", body.getReasonsForDecision().get(0));
        }
        assertEquals("Something else.", body.getAdditionalDirections().get(0));
        assertEquals("faceToFace", body.getHearingType());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void willSetPreviewFileWithNullReasons_WhenReasonsListIsEmpty(String nextHearingType, String nextHearingTypeText) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.setAdjournCaseReasons(new ArrayList<>());

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, true);

        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void willSetPreviewFileWithNullReasons_WhenReasonsListIsNotEmpty(String nextHearingType, String nextHearingTypeText) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void willSetPreviewFileWithInterpreterDescription_WhenInterpreterRequiredAndLanguageIsSet(String nextHearingType, String nextHearingTypeText) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseInterpreterRequired("Yes");
        sscsCaseData.setAdjournCaseInterpreterLanguage( new DynamicList("French"));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());

        assertEquals("an interpreter in French", body.getInterpreterDescription());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void willNotSetPreviewFileButWillDisplayError_WithInterpreterDescription_WhenInterpreterRequiredAndLanguageIsNotSet(String nextHearingType, String nextHearingTypeText) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseInterpreterRequired("Yes");


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void willSetPreviewFileWithoutInterpreterDescription_WhenInterpreterNotRequiredAndLanguageIsSet(String nextHearingType, String nextHearingTypeText) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseInterpreterRequired("No");
        sscsCaseData.setAdjournCaseInterpreterLanguage( new DynamicList("French"));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());

        assertNull(body.getInterpreterDescription());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void willSetPreviewFileWithoutInterpreterDescription_WhenInterpreterRequiredNotSetAndLanguageIsSet(String nextHearingType, String nextHearingTypeText) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseInterpreterLanguage(new DynamicList("French"));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());

        assertNull(body.getInterpreterDescription());
    }


    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        when(userDetailsService.buildLoggedInUserName("Bearer token")).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenSignedInJudgeUserDetailsNotSet_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        when(userDetailsService.buildLoggedInUserName("Bearer token")).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenGenerateNoticeNotSet_willNotSetPreviewFile(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithMultipleHearingsWithVenues_thenCorrectlySetHeldAtUsingTheFirstHearingInList(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("venue 2 name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Gap venue name", body.getHeldAt());

    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(null);;
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
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenue_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

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
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayAnErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

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
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDetails_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

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
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithEmptyHearingsList_thenDefaultHearingData(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals("In chambers", body.getHeldAt());

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithNullHearingsList_thenDefaultHearingData(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals("In chambers", body.getHeldAt());

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithMultipleHearingsWithHearingDates_thenCorrectlySetTheHeldOnUsingTheFirstHearingInList(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("Venue Name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("2019-01-02", body.getHeldOn().toString());

    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDate_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

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
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayTwoErrorsAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

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
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithNoDurationParameters_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        final boolean isOralHearing = HearingType.FACE_TO_FACE.getValue().equals(nextHearingType)
            || HearingType.TELEPHONE.getValue().equals(nextHearingType)
            || HearingType.VIDEO.getValue().equals(nextHearingType);

        if (isOralHearing) {
            assertEquals("a standard time slot", body.getNextHearingTimeslot());
        }
    }

    @Test
    @Parameters(named = "oralNextHearingTypeParameters")
    public void givenCaseWithDurationParameterButMissingUnitsWhenOralHearing_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseNextHearingListingDuration("2");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Timeslot duration units not supplied on case data", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "paperNextHearingTypeParameters")
    public void givenCaseWithDurationParameterButMissingUnitsWhenPaperHearing_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseNextHearingListingDuration("2");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertNull(body.getNextHearingTimeslot());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithTwoHourDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setAdjournCaseNextHearingListingDurationUnits("hours");
        sscsCaseData.setAdjournCaseNextHearingListingDuration("2");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        final boolean isOralHearing = HearingType.FACE_TO_FACE.getKey().equals(nextHearingType)
            || HearingType.TELEPHONE.getKey().equals(nextHearingType)
            || HearingType.VIDEO.getKey().equals(nextHearingType);

        if (isOralHearing) {
            assertEquals("2 hours", body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithOneHourDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setAdjournCaseNextHearingListingDurationUnits("hours");
        sscsCaseData.setAdjournCaseNextHearingListingDuration("1");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        final boolean isOralHearing = HearingType.FACE_TO_FACE.getKey().equals(nextHearingType)
            || HearingType.TELEPHONE.getKey().equals(nextHearingType)
            || HearingType.VIDEO.getKey().equals(nextHearingType);

        if (isOralHearing) {
            assertEquals("1 hour", body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithTwoSessionDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setAdjournCaseNextHearingListingDurationUnits("sessions");
        sscsCaseData.setAdjournCaseNextHearingListingDuration("2");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        final boolean isOralHearing = HearingType.FACE_TO_FACE.getKey().equals(nextHearingType)
            || HearingType.TELEPHONE.getKey().equals(nextHearingType)
            || HearingType.VIDEO.getKey().equals(nextHearingType);

        if (isOralHearing) {
            assertEquals("2 sessions", body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithOneSessionDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setAdjournCaseNextHearingListingDurationUnits("sessions");
        sscsCaseData.setAdjournCaseNextHearingListingDuration("1");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        final boolean isOralHearing = HearingType.FACE_TO_FACE.getKey().equals(nextHearingType)
            || HearingType.TELEPHONE.getKey().equals(nextHearingType)
            || HearingType.VIDEO.getKey().equals(nextHearingType);

        if (isOralHearing) {
            assertEquals("1 session", body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithThreePanelMembers_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setAdjournCaseDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        sscsCaseData.setAdjournCaseMedicallyQualifiedPanelMemberName("Ms Panel Member 2");
        sscsCaseData.setAdjournCaseOtherPanelMemberName("Other Panel Member");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1, Ms Panel Member 2 and Other Panel Member", body.getHeldBefore());

    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithTwoPanelMembers_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setAdjournCaseDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        sscsCaseData.setAdjournCaseMedicallyQualifiedPanelMemberName("Ms Panel Member 2");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1 and Ms Panel Member 2", body.getHeldBefore());

    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setAdjournCaseDisabilityQualifiedPanelMemberName("Mr Panel Member 1");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name and Mr Panel Member 1", body.getHeldBefore());

    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithNoPanelMembersWithNullValues_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseGenerateNotice("yes");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());

    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithNoPanelMembersWithEmptyValues_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseMedicallyQualifiedPanelMemberName("");
        sscsCaseData.setAdjournCaseDisabilityQualifiedPanelMemberName("");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());
    }

    @Test
    @Parameters(named = "faceToFaceNextHearingTypeParameter")
    public void givenCaseWithNoSelectedVenueNotSetForFaceToFace_thenCorrectlySetTheVenueToBeTheExistingVenue(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("Gap venue name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @Test
    @Parameters(named = "nonFaceToFaceNextHearingTypeParameters")
    public void givenCaseWithNoSelectedVenueNotSetForNonFaceToFace_thenCorrectlySetTheVenueToBeNull(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);

        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
    }

    // Scenarios for next hearing date
    @Test
    public void givenCaseWithFirstAvailableDateAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the morning session on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the afternoon session on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAndFirstOnSessionNotSelectedAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        List<String> sessions = new ArrayList<>();
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAndFirstOnSessionSelectedAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the session on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate("2020-01-01");

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the morning session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate("2020-01-01");

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the afternoon session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate("2020-01-01");

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate("2020-01-01");

        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the morning session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate("2020-01-01");

        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the afternoon session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate("2020-01-01");

        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("providePeriod");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterPeriod("28");

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the morning session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("providePeriod");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterPeriod("28");

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the afternoon session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("providePeriod");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterPeriod("28");

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("providePeriod");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterPeriod("28");

        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the morning session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("providePeriod");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterPeriod("28");

        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the afternoon session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("providePeriod");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterPeriod("28");

        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithDateToBeFixedAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("dateToBeFixed");

        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithDateToBeFixedAndFirstOnSessionAndMorningSessionProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("dateToBeFixed");

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the morning session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithDateToBeFixedAndFirstOnSessionAndAfternoonSessionProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("dateToBeFixed");

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the afternoon session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndMorningSessionProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("dateToBeFixed");

        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the morning session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndAfternoonSessionProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("dateToBeFixed");

        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the afternoon session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDateAfterDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate("2020-01-01");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFixedDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("dateToBeFixed");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAndNotAdjournCaseTimeAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDateAfterDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseTime(null);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithDateToBeFixedAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("dateToBeFixed");

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.setAdjournCaseTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterWithNoDateOrPeriodIndicator_ThenDisplayErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate("2020-01-01");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Date or period indicator not available in case data", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());

    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterWithProvideDateAndNoDateSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("No value set for adjournCaseNextHearingFirstAvailableDateAfterDate in case data", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());

    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterWithProvidePeriodAndNoPeriodSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("providePeriod");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("No value set for adjournCaseNextHearingFirstAvailableDateAfterPeriod in case data", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());

    }

    @Test
    public void givenCaseWithFirstAvailableDateAfterWithNeitherProvideDateOrPeriodSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Date or period indicator not available in case data", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());

    }

    @Test
    public void givenCaseWithFirstWithInvalidDateTime_thenDisplayAnErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("unknownDateType");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unknown next hearing date type for:unknownDateType", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "faceToFaceNextHearingTypeParameter")
    public void givenCaseWithSameVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeThePreviousVenue(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, Arrays.asList());

        sscsCaseData.setAdjournCaseNextHearingVenue("sameVenue");
        sscsCaseData.setAdjournCaseNextHearingVenueSelected(list);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("Gap venue name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @Test
    @Parameters(named = "faceToFaceNextHearingTypeParameter")
    public void givenCaseWithSelectedVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeTheNewVenue(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, Arrays.asList());

        sscsCaseData.setAdjournCaseNextHearingVenue("somewhereElse");
        sscsCaseData.setAdjournCaseNextHearingVenueSelected(list);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("Gap venue name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @Test
    @Parameters(named = "nonFaceToFaceNextHearingTypeParameters")
    public void givenCaseWithSelectedVenueSetForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, Arrays.asList());

        sscsCaseData.setAdjournCaseNextHearingVenue("somewhereElse");
        sscsCaseData.setAdjournCaseNextHearingVenueSelected(list);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("adjournCaseNextHearingVenueSelected field should not be set", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "faceToFaceNextHearingTypeParameter")
    public void givenCaseWithSelectedVenueSetIncorrectlyForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(listItem, Arrays.asList(listItem));

        sscsCaseData.setAdjournCaseNextHearingVenue("somewhereElse");
        sscsCaseData.setAdjournCaseNextHearingVenueSelected(list);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to load venue details for id:someUnknownVenueId", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "faceToFaceNextHearingTypeParameter")
    public void givenCaseWithSelectedVenueMissingListItemForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(null, Arrays.asList(listItem));

        sscsCaseData.setAdjournCaseNextHearingVenue("somewhereElse");
        sscsCaseData.setAdjournCaseNextHearingVenueSelected(list);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("A next hearing venue of somewhere else has been specified but no venue has been selected", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "faceToFaceNextHearingTypeParameter")
    public void givenCaseWithSelectedVenueMissingListItemCodeForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(new DynamicListItem(null, ""), Arrays.asList(listItem));

        sscsCaseData.setAdjournCaseNextHearingVenue("somewhereElse");
        sscsCaseData.setAdjournCaseNextHearingVenueSelected(list);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("A next hearing venue of somewhere else has been specified but no venue has been selected", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "nonFaceToFaceNextHearingTypeParameters")
    public void givenCaseWithSelectedVenueSetIncorrectlyForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(listItem, Arrays.asList(listItem));

        sscsCaseData.setAdjournCaseNextHearingVenue("somewhereElse");
        sscsCaseData.setAdjournCaseNextHearingVenueSelected(list);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("adjournCaseNextHearingVenueSelected field should not be set", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void scottishRpcWillShowAScottishImage(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        verifyTemplateBody(NoticeIssuedTemplateBody.SCOTTISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppended(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
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

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appointee Surname, appointee for Appellant Lastname", nextHearingTypeText, true);
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenDateIssuedParameterIsTrue_thenShowIssuedDateOnDocument(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals(LocalDate.now(), payload.getDateIssued());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenGeneratedDateIsAlreadySetForGeneratedFlow_thenDoSetNewGeneratedDate(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseGeneratedDate("2018-10-10");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenGeneratedDateIsAlreadySetNonGeneratedFlow_thenDoSetNewGeneratedDate(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("no");
        sscsCaseData.setAdjournCaseGeneratedDate("2018-10-10");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    private NoticeIssuedTemplateBody verifyTemplateBody(String image, String expectedName, String nextHearingType, boolean isDraft) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        final boolean hasVenue = HearingType.FACE_TO_FACE.getValue().equals(nextHearingType);
        final boolean isOralHearing = HearingType.FACE_TO_FACE.getValue().equals(nextHearingType)
            || HearingType.TELEPHONE.getValue().equals(nextHearingType)
            || HearingType.VIDEO.getValue().equals(nextHearingType);

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
        if (hasVenue) {
            assertNotNull(body.getNextHearingVenue());
        } else {
            assertNull(body.getNextHearingVenue());
        }
        if (AdjournCasePreviewService.IN_CHAMBERS.equals(body.getNextHearingVenue())) {
            assertFalse(body.isNextHearingAtVenue());
        } else {
            assertEquals(hasVenue, body.isNextHearingAtVenue());
        }
        assertNotNull(body.getNextHearingDate());
        assertNotNull(body.getNextHearingType());
        assertEquals(nextHearingType, body.getNextHearingType());
        if (isOralHearing) {
            assertNotNull(body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
        assertNotNull(body.getHeldAt());
        assertNotNull(body.getHeldBefore());
        assertNotNull(body.getHeldOn());
        return payload;
    }


}
