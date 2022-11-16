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
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.DATE_TO_BE_FIXED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingPeriod.TWENTY_EIGHT_DAYS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingVenue.SAME_VENUE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingVenue.SOMEWHERE_ELSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing.FACE_TO_FACE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTime;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.docassembly.AdjournCaseTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.service.LanguageService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;

@RunWith(JUnitParamsRunner.class)
class AdjournCasePreviewServiceTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String TEMPLATE_ID = "nuts.docx";
    private static final String URL = "http://dm-store/documents/123";
    private static final LocalDate LOCAL_DATE = LocalDate.parse("2018-10-10");
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
    
    private Adjournment adjournment;

    @Mock
    private VenueDataLoader venueDataLoader;

    @BeforeEach
    void setUp() throws IOException {
        openMocks(this);
        service = new AdjournCasePreviewService(generateFile, userDetailsService,
            venueDataLoader, new LanguageService(), TEMPLATE_ID);

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
                .build())
            .adjournment(Adjournment.builder()
                .reasons(List.of(new CollectionItem<>(null, "My reasons for decision")))
                .additionalDirections(List.of(new CollectionItem<>(null, "Something else.")))
                .typeOfHearing(FACE_TO_FACE)
                .generateNotice(YES)
                .typeOfNextHearing(FACE_TO_FACE)
                .nextHearingDateType(FIRST_AVAILABLE_DATE)
                .build())
            .hearings(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingDate("2019-01-01")
                    .venue(Venue.builder()
                        .name("Venue Name")
                        .build())
                    .venueId("someVenueId").build())
                .build()))
            .build();
        
        adjournment = sscsCaseData.getAdjournment();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);

        when(generateFile.assemble(any())).thenReturn(URL);
    }

    private static Stream<Arguments> allNextHearingTypeParameters() {
        return Stream.of(
            Arguments.of("telephone", "telephone hearing"),
            Arguments.of("video", "video hearing"),
            Arguments.of("paper", "decision on the papers"),
            Arguments.of("faceToFace", "face to face hearing")
        );
    }

    private static Stream<Arguments> nonFaceToFaceNextHearingTypeParameters() {
        return Stream.of(
            Arguments.of("telephone", "telephone hearing"),
            Arguments.of("video", "video hearing"),
            Arguments.of("paper", "decision on the papers")
        );
    }

    private static Stream<Arguments> paperNextHearingTypeParameters() {
        return Stream.of(
            Arguments.of("paper", "decision on the papers")
        );
    }

    private static Stream<Arguments> oralNextHearingTypeParameters() {
        return Stream.of(
            Arguments.of("telephone", "telephone hearing"),
            Arguments.of("video", "video hearing"),
            Arguments.of("faceToFace", "face to face hearing")
        );
    }

    private static Stream<Arguments> faceToFaceNextHearingTypeParameters() {
        return Stream.of(
            Arguments.of("faceToFace", "face to face hearing")
        );
    }

    private static boolean isOralHearing(String nextHearingType) {
        return HearingType.FACE_TO_FACE.getKey().equals(nextHearingType)
            || HearingType.TELEPHONE.getKey().equals(nextHearingType)
            || HearingType.VIDEO.getKey().equals(nextHearingType);
    }

    private static AdjournCaseTemplateBody checkCommonPreviewParams(NoticeIssuedTemplateBody payload) {
        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

        assertNotNull(body);
        assertEquals("Something else.", body.getAdditionalDirections().get(0));
        assertEquals("faceToFace", body.getHearingType());
        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());

        return body;
    }

    private static AdjournCaseTemplateBody checkCommonPreviewParamsWithReasons(NoticeIssuedTemplateBody payload) {
        AdjournCaseTemplateBody body = checkCommonPreviewParams(payload);
        assertNotNull(body.getReasonsForDecision());
        assertFalse(body.getReasonsForDecision().isEmpty());
        assertEquals("My reasons for decision", body.getReasonsForDecision().get(0));
        return body;
    }

    private static void checkCommonPreviewParamsWithNullReasons(NoticeIssuedTemplateBody payload) {
        AdjournCaseTemplateBody body = checkCommonPreviewParams(payload);
        assertNull(body.getReasonsForDecision());
    }

    private void setAdjournmentNextHearingType(String nextHearingType) {
        adjournment.setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
    }

    private static void checkPreviewDocument(PreSubmitCallbackResponse<SscsCaseData> response) {
        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());
    }

    private PreSubmitCallbackResponse<SscsCaseData> getSscsCaseDataPreSubmitCallbackResponse(boolean showIssueDate) {
        return service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, showIssueDate);
    }
    
    private PreSubmitCallbackResponse<SscsCaseData> getResponseShowIssueDate() {
        return getSscsCaseDataPreSubmitCallbackResponse(true);
    }

    private PreSubmitCallbackResponse<SscsCaseData> getResponseDoNotShowIssueDate() {
        return getSscsCaseDataPreSubmitCallbackResponse(false);
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithNullReasons_WhenReasonsListIsEmpty(String nextHearingType, String nextHearingTypeText) {

        adjournment.setReasons(new ArrayList<>());

        setAdjournmentNextHearingType(nextHearingType);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        checkCommonPreviewParamsWithNullReasons(payload);
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithNullReasons_WhenReasonsListIsNotEmpty(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(
            NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        checkCommonPreviewParamsWithReasons(payload);
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithInterpreterDescription_WhenInterpreterRequiredAndLanguageIsSet(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setInterpreterRequired(YES);
        adjournment.setInterpreterLanguage("french");

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = checkCommonPreviewParamsWithReasons(payload);

        assertEquals("an interpreter in French", body.getInterpreterDescription());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willNotSetPreviewFileButWillDisplayError_WithInterpreterDescription_WhenInterpreterRequiredAndLanguageIsNotSet(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setInterpreterRequired(YES);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithoutInterpreterDescription_WhenInterpreterNotRequiredAndLanguageIsSet(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setInterpreterRequired(NO);
        adjournment.setInterpreterLanguage("french");

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = checkCommonPreviewParamsWithReasons(payload);

        assertNull(body.getInterpreterDescription());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithoutInterpreterDescription_WhenInterpreterRequiredNotSetAndLanguageIsSet(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setInterpreterLanguage("french");

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = checkCommonPreviewParamsWithReasons(payload);

        assertNull(body.getInterpreterDescription());
    }


    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        when(userDetailsService.buildLoggedInUserName("Bearer token")).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenSignedInJudgeUserDetailsNotSet_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        when(userDetailsService.buildLoggedInUserName("Bearer token")).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenGenerateNoticeNotSet_willNotSetPreviewFile(String nextHearingType) {
        adjournment.setGenerateNotice(null);
        setAdjournmentNextHearingType(nextHearingType);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithVenues_thenCorrectlySetHeldAtUsingTheFirstHearingInList(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("venue 2 name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Gap venue name", body.getHeldAt());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(null);;
        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstInListWithNoVenue_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayAnErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = null;

        sscsCaseData.setHearings(Arrays.asList(hearing2, hearing1));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDetails_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithEmptyHearingsList_thenDefaultHearingData(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals("In chambers", body.getHeldAt());

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithNullHearingsList_thenDefaultHearingData(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);
        sscsCaseData.setHearings(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals("In chambers", body.getHeldAt());

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithHearingDates_thenCorrectlySetTheHeldOnUsingTheFirstHearingInList(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("Venue Name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("2019-01-02", body.getHeldOn().toString());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDate_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("Venue Name").build())
            .build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayTwoErrorsAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01")
            .venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = null;

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        assertNull(response.getData().getAdjournment().getPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithNoDurationEnumSource_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        if (isOralHearing(nextHearingType)) {
            assertEquals("a standard time slot", body.getNextHearingTimeslot());
        }
    }

    @ParameterizedTest
    @MethodSource("oralNextHearingTypeParameters")
    void givenCaseWithDurationParameterButMissingUnitsWhenOralHearing_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);
        adjournment.setNextHearingListingDuration(2);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Timeslot duration units not supplied on case data", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("paperNextHearingTypeParameters")
    void givenCaseWithDurationParameterButMissingUnitsWhenPaperHearing_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);
        adjournment.setNextHearingListingDuration(2);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertNull(body.getNextHearingTimeslot());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithTwoHourDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.MINUTES);
        adjournment.setNextHearingListingDuration(120);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        if (isOralHearing(nextHearingType)) {
            assertEquals("120 minutes", body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithSixtyMinuteDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.MINUTES);
        adjournment.setNextHearingListingDuration(60);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        if (isOralHearing(nextHearingType)) {
            assertEquals("60 minutes", body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
    }



    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithTwoSessionDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS);
        adjournment.setNextHearingListingDuration(2);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        if (isOralHearing(nextHearingType)) {
            assertEquals("2 sessions", body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithOneSessionDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS);
        adjournment.setNextHearingListingDuration(1);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        if (isOralHearing(nextHearingType)) {
            assertEquals("1 session", body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
    }


    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithThreePanelMembers_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        adjournment.setMedicallyQualifiedPanelMemberName("Ms Panel Member 2");
        adjournment.setOtherPanelMemberName("Other Panel Member");

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1, Ms Panel Member 2 and Other Panel Member", body.getHeldBefore());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithTwoPanelMembers_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        adjournment.setMedicallyQualifiedPanelMemberName("Ms Panel Member 2");

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1 and Ms Panel Member 2", body.getHeldBefore());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setDisabilityQualifiedPanelMemberName("Mr Panel Member 1");

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name and Mr Panel Member 1", body.getHeldBefore());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithNoPanelMembersWithNullValues_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithNoPanelMembersWithEmptyValues_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setMedicallyQualifiedPanelMemberName("");
        adjournment.setDisabilityQualifiedPanelMemberName("");

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithNoSelectedVenueNotSetForFaceToFace_thenCorrectlySetTheVenueToBeTheExistingVenue(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("Gap venue name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @ParameterizedTest
    @MethodSource("nonFaceToFaceNextHearingTypeParameters")
    void givenCaseWithNoSelectedVenueNotSetForNonFaceToFace_thenCorrectlySetTheVenueToBeNull(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
    }

    // Scenarios for next hearing date
    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the morning session on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the afternoon session on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionNotSelectedAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        List<String> sessions = new ArrayList<>();
        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionSelectedAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the session on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the morning session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the afternoon session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the morning session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the afternoon session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        adjournment.setTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }
    
    @Nested
    class FirstAvailableDateAfterPeriod {
        
        @BeforeEach
        void setUp() {
            adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
            adjournment.setNextHearingDateOrPeriod(PROVIDE_PERIOD);
            adjournment.setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);
        }
        
        @Test
        void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
            List<String> sessions = new ArrayList<>();
            sessions.add("firstOnSession");
            adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").adjournCaseNextHearingFirstOnSession(sessions).build());
    
            sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                    .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));
    
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);
    
            String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    
            NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
            assertEquals("It will be first in the morning session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        }
    
        @Test
        void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
            List<String> sessions = new ArrayList<>();
            sessions.add("firstOnSession");
            adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").adjournCaseNextHearingFirstOnSession(sessions).build());
    
            sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                    .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));
    
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);
    
            String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    
            NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
            assertEquals("It will be first in the afternoon session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        }
    
        @Test
        void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
            List<String> sessions = new ArrayList<>();
            sessions.add("firstOnSession");
            adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());
    
            sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                    .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));
    
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);
    
            String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    
            NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
            assertEquals("It will be first in the session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        }
    
        @Test
        void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
            adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());
    
            sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                    .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));
    
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);
    
            String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    
            NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
            assertEquals("It will be in the morning session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        }
    
        @Test
        void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
            adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());
    
            sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                    .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));
    
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);
    
            String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    
            NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
            assertEquals("It will be in the afternoon session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        }
    
        @Test
        void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
            adjournment.setTime(AdjournCaseTime.builder().build());
    
            sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                    .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));
    
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);
    
            String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    
            NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
            assertEquals("It will be re-scheduled on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        }

        @Test
        void givenCaseWithFirstAvailableDateAfterWithProvidePeriodAndNoPeriodSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {
            adjournment.setNextHearingFirstAvailableDateAfterPeriod(null);

            sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

            final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

            String error = response.getErrors().stream().findFirst().orElse("");
            assertEquals("No value set for adjournCaseNextHearingFirstAvailableDateAfterPeriod in case data", error);
            assertNull(response.getData().getAdjournment().getPreviewDocument());
        }
    }

    @Test
    void givenCaseWithDateToBeFixedAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        adjournment.setTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndFirstOnSessionAndMorningSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the morning session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndFirstOnSessionAndAfternoonSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the afternoon session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndMorningSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the morning session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndAfternoonSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the afternoon session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDate() {
        adjournment.setTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDateAfterDate() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));
        adjournment.setTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFixedDate() {
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);
        adjournment.setTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAndNotAdjournCaseTimeAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDateAfterDate() {
        adjournment.setTime(null);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithNoDateOrPeriodIndicator_ThenDisplayErrorAndDoNotDisplayTheDocument() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Date or period indicator not available in case data", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithProvideDateAndNoDateSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("No value set for adjournCaseNextHearingFirstAvailableDateAfterDate in case data", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithNeitherProvideDateOrPeriodSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Date or period indicator not available in case data", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSameVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeThePreviousVenue(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        adjournment.setNextHearingVenue(SAME_VENUE);
        adjournment.setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("Gap venue name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeTheNewVenue(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("Gap venue name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @ParameterizedTest
    @MethodSource("nonFaceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueSetForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("adjournCaseNextHearingVenueSelected field should not be set", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueSetIncorrectlyForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(listItem, List.of(listItem));

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to load venue details for id:someUnknownVenueId", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueMissingListItemForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(null, List.of(listItem));

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("A next hearing venue of somewhere else has been specified but no venue has been selected", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueMissingListItemCodeForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(new DynamicListItem(null, ""), List.of(listItem));

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("A next hearing venue of somewhere else has been specified but no venue has been selected", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("nonFaceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueSetIncorrectlyForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(listItem, List.of(listItem));

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = getResponseDoNotShowIssueDate();

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("adjournCaseNextHearingVenueSelected field should not be set", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void scottishRpcWillShowAScottishImage(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        verifyTemplateBody(NoticeIssuedTemplateBody.SCOTTISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppended(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        sscsCaseData.getAppeal().getAppellant().setIsAppointee("yes");
        sscsCaseData.getAppeal().getAppellant().setAppointee(Appointee.builder()
            .name(Name.builder().firstName("APPOINTEE")
                .lastName("SurNamE")
                .build())
            .identity(Identity.builder().build())
            .build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appointee Surname, appointee for Appellant Lastname", nextHearingTypeText, true);
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenDateIssuedParameterIsTrue_thenShowIssuedDateOnDocument(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals(LocalDate.now(), payload.getDateIssued());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenGeneratedDateIsAlreadySetForGeneratedFlow_thenDoSetNewGeneratedDate(String nextHearingType, String nextHearingTypeText) {
        adjournment.setGeneratedDate(LOCAL_DATE);
        setAdjournmentNextHearingType(nextHearingType);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenGeneratedDateIsAlreadySetNonGeneratedFlow_thenDoSetNewGeneratedDate(String nextHearingType, String nextHearingTypeText) {
        adjournment.setGenerateNotice(NO);
        adjournment.setGeneratedDate(LOCAL_DATE);
        setAdjournmentNextHearingType(nextHearingType);

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