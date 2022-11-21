package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.jetbrains.annotations.NotNull;
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
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.service.LanguageService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;

@RunWith(JUnitParamsRunner.class)
class AdjournCasePreviewServiceTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String TEMPLATE_ID = "nuts.docx";
    private static final String URL = "http://dm-store/documents/123";
    private static final LocalDate LOCAL_DATE = LocalDate.parse("2018-10-10");

    private static final String ALL_NEXT_HEARING_TYPE_PARAMETERS =
        "uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.AdjournCasePreviewServiceTest#allNextHearingTypeParameters";
    public static final Arguments TELEPHONE_ARGUMENTS = Arguments.of("telephone", "telephone hearing");
    public static final Arguments VIDEO_ARGUMENTS = Arguments.of("video", "video hearing");
    public static final Arguments PAPER_ARGUMENTS = Arguments.of("paper", "decision on the papers");
    public static final Arguments FACE_TO_FACE_ARGUMENTS = Arguments.of("faceToFace", "face to face hearing");

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

    @Mock
    private SignLanguagesService signLanguagesService;

    @BeforeEach
    void setUp() throws IOException {
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
            TELEPHONE_ARGUMENTS,
            VIDEO_ARGUMENTS,
            PAPER_ARGUMENTS,
            FACE_TO_FACE_ARGUMENTS
        );
    }

    private static Stream<Arguments> nonFaceToFaceNextHearingTypeParameters() {
        return Stream.of(
            TELEPHONE_ARGUMENTS,
            VIDEO_ARGUMENTS,
            PAPER_ARGUMENTS
        );
    }

    private static Stream<Arguments> oralNextHearingTypeParameters() {
        return Stream.of(
            TELEPHONE_ARGUMENTS,
            VIDEO_ARGUMENTS,
            FACE_TO_FACE_ARGUMENTS
        );
    }

    private static Stream<Arguments> faceToFaceNextHearingTypeParameters() {
        return Stream.of(
            FACE_TO_FACE_ARGUMENTS
        );
    }

    private NoticeIssuedTemplateBody verifyTemplateBody(String image, String expectedName, String nextHearingType, boolean isDraft) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        final boolean hasVenue = HearingType.FACE_TO_FACE.getValue().equals(nextHearingType);
        final boolean isOralHearing = HearingType.FACE_TO_FACE.getValue().equals(nextHearingType)
            || HearingType.TELEPHONE.getValue().equals(nextHearingType)
            || HearingType.VIDEO.getValue().equals(nextHearingType);

        NoticeIssuedTemplateBody payload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        assertThat(payload.getImage()).isEqualTo(image);
        if (isDraft) {
            assertThat(payload.getNoticeType()).isEqualTo("DRAFT ADJOURNMENT NOTICE");
        } else {
            assertThat(payload.getNoticeType()).isEqualTo("ADJOURNMENT NOTICE");
        }
        assertThat(payload.getAppellantFullName()).isEqualTo(expectedName);
        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertThat(body).isNotNull();
        if (hasVenue) {
            assertThat(body.getNextHearingVenue()).isNotNull();
        } else {
            assertThat(body.getNextHearingVenue()).isNull();
        }
        if (AdjournCasePreviewService.IN_CHAMBERS.equals(body.getNextHearingVenue())) {
            assertThat(body.isNextHearingAtVenue()).isFalse();
        } else {
            assertThat(body.isNextHearingAtVenue()).isEqualTo(hasVenue);
        }
        assertThat(body.getNextHearingDate()).isNotNull();
        assertThat(body.getNextHearingType()).isNotNull();
        assertThat(body.getNextHearingType()).isEqualTo(nextHearingType);
        if (isOralHearing) {
            assertThat(body.getNextHearingTimeslot()).isNotNull();
        } else {
            assertThat(body.getNextHearingTimeslot()).isNull();
        }
        assertThat(body.getHeldAt()).isNotNull();
        assertThat(body.getHeldBefore()).isNotNull();
        assertThat(body.getHeldOn()).isNotNull();
        return payload;
    }

    private static boolean isOralHearing(String nextHearingType) {
        return HearingType.FACE_TO_FACE.getKey().equals(nextHearingType)
            || HearingType.TELEPHONE.getKey().equals(nextHearingType)
            || HearingType.VIDEO.getKey().equals(nextHearingType);
    }

    private static Hearing createHearingWithDateAndVenueName(String date, String venueName) {
        return Hearing.builder()
            .value(HearingDetails.builder()
                .hearingDate(date)
                .venue(Venue.builder()
                    .name(venueName)
                    .build())
                .build())
            .build();
    }

    private void setAdjournmentCaseTimeWithSessionsAndSpecificTime(List<String> sessions, String specificTime) {
        adjournment.setTime(AdjournCaseTime.builder()
            .adjournCaseNextHearingSpecificTime(specificTime)
            .adjournCaseNextHearingFirstOnSession(sessions)
            .build());
    }

    private void setAdjournmentHearingFirstOnSessionAtSpecificTime(String specificTime) {
        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        setAdjournmentCaseTimeWithSessionsAndSpecificTime(sessions, specificTime);
    }

    private void setAdjournmentHearingFirstOnSessionWithNoSpecificTime() {
        setAdjournmentHearingFirstOnSessionAtSpecificTime(null);
    }

    private static AdjournCaseTemplateBody checkCommonPreviewParams(NoticeIssuedTemplateBody payload) {
        assertThat(payload.getUserName()).isEqualTo("Judge Full Name");
        assertThat(payload.getNoticeType()).isEqualTo("DRAFT ADJOURNMENT NOTICE");

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

        assertThat(body).isNotNull();
        assertThat(body.getAdditionalDirections().get(0)).isEqualTo("Something else.");
        assertThat(body.getHearingType()).isEqualTo("faceToFace");
        assertThat(payload.getDateIssued()).isNull();
        assertThat(payload.getGeneratedDate()).isEqualTo(LocalDate.now());

        return body;
    }

    private static AdjournCaseTemplateBody checkCommonPreviewParamsWithReasons(NoticeIssuedTemplateBody payload) {
        AdjournCaseTemplateBody body = checkCommonPreviewParams(payload);
        assertThat(body.getReasonsForDecision()).isNotNull();
        assertThat(body.getReasonsForDecision()).isNotEmpty();
        assertThat(body.getReasonsForDecision().get(0)).isEqualTo("My reasons for decision");
        return body;
    }

    private static void checkCommonPreviewParamsWithNullReasons(NoticeIssuedTemplateBody payload) {
        AdjournCaseTemplateBody body = checkCommonPreviewParams(payload);
        assertThat(body.getReasonsForDecision()).isNull();
    }

    private void setAdjournmentNextHearingType(String nextHearingType) {
        adjournment.setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
    }

    private static void checkPreviewDocument(PreSubmitCallbackResponse<SscsCaseData> response) {
        assertThat(response.getData().getAdjournment().getPreviewDocument()).isNotNull();
        assertThat(response.getData().getAdjournment().getPreviewDocument()).isEqualTo(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf",
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build());
    }

    private void checkTemplateBodyNextHearingDate(String expected) {
        NoticeIssuedTemplateBody body = verifyTemplateBody(
            NoticeIssuedTemplateBody.ENGLISH_IMAGE,
            "Appellant Lastname",
            "face to face hearing",
            true);
        assertThat(body.getAdjournCaseTemplateBody().getNextHearingDate()).isEqualTo(expected);
    }

    @NotNull
    private AdjournCaseTemplateBody getAdjournCaseTemplateBodyWithHearingTypeText(String nextHearingTypeText) {
        final PreSubmitCallbackResponse<SscsCaseData> response = previewNoticeDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertThat(body).isNotNull();
        return body;
    }

    private PreSubmitCallbackResponse<SscsCaseData> getSscsCaseDataPreSubmitCallbackResponse(boolean showIssueDate) {
        return service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, showIssueDate);
    }
    
    private PreSubmitCallbackResponse<SscsCaseData> previewNoticeShowIssueDate() {
        return getSscsCaseDataPreSubmitCallbackResponse(true);
    }

    private PreSubmitCallbackResponse<SscsCaseData> previewNoticeDoNotShowIssueDate() {
        return getSscsCaseDataPreSubmitCallbackResponse(false);
    }

    private void checkDocumentIsNotCreatedAndReturnsError(String expected) {
        final PreSubmitCallbackResponse<SscsCaseData> response = previewNoticeDoNotShowIssueDate();
        String error = response.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo(expected);
        assertThat(response.getData().getAdjournment().getPreviewDocument()).isNull();
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithNullReasons_WhenReasonsListIsEmpty(String nextHearingType, String nextHearingTypeText) {

        adjournment.setReasons(new ArrayList<>());

        setAdjournmentNextHearingType(nextHearingType);

        final PreSubmitCallbackResponse<SscsCaseData> response = previewNoticeDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        checkCommonPreviewParamsWithNullReasons(payload);
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithNullReasons_WhenReasonsListIsNotEmpty(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        final PreSubmitCallbackResponse<SscsCaseData> response = previewNoticeDoNotShowIssueDate();

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
        adjournment.setInterpreterLanguage(new DynamicList("French"));

        final PreSubmitCallbackResponse<SscsCaseData> response = previewNoticeDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = checkCommonPreviewParamsWithReasons(payload);

        assertThat(body.getInterpreterDescription()).isEqualTo("an interpreter in French");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willNotSetPreviewFileButWillDisplayError_WithInterpreterDescription_WhenInterpreterRequiredAndLanguageIsNotSet(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setInterpreterRequired(YES);

        checkDocumentIsNotCreatedAndReturnsError("An interpreter is required but no language is set");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithoutInterpreterDescription_WhenInterpreterNotRequiredAndLanguageIsSet(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setInterpreterRequired(NO);
        adjournment.setInterpreterLanguage(new DynamicList("French"));

        final PreSubmitCallbackResponse<SscsCaseData> response = previewNoticeDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = checkCommonPreviewParamsWithReasons(payload);

        assertThat(body.getInterpreterDescription()).isNull();
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithoutInterpreterDescription_WhenInterpreterRequiredNotSetAndLanguageIsSet(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        adjournment.setInterpreterLanguage(new DynamicList("French"));

        final PreSubmitCallbackResponse<SscsCaseData> response = previewNoticeDoNotShowIssueDate();

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = checkCommonPreviewParamsWithReasons(payload);

        assertThat(body.getInterpreterDescription()).isNull();
    }


    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        when(userDetailsService.buildLoggedInUserName("Bearer token")).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        checkDocumentIsNotCreatedAndReturnsError("Unable to obtain signed in user details");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenSignedInJudgeUserDetailsNotSet_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        when(userDetailsService.buildLoggedInUserName("Bearer token")).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        checkDocumentIsNotCreatedAndReturnsError("Unable to obtain signed in user details");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenGenerateNoticeNotSet_willNotSetPreviewFile(String nextHearingType) {
        adjournment.setGenerateNotice(null);
        setAdjournmentNextHearingType(nextHearingType);

        checkDocumentIsNotCreatedAndReturnsError("Generate notice has not been set");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithVenues_thenCorrectlySetHeldAtUsingTheFirstHearingInList(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName("2019-01-01", "venue 1 name");

        Hearing hearing2 = createHearingWithDateAndVenueName("2019-01-02", "venue 2 name");

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

        assertThat(body.getHeldAt()).isEqualTo("Gap venue name");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(null);
        Hearing hearing1 = createHearingWithDateAndVenueName("2019-01-01", "venue 1 name");

        Hearing hearing2 = createHearingWithDateAndVenueName("2019-01-02", null);

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        checkDocumentIsNotCreatedAndReturnsError("Unable to determine hearing venue");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstInListWithNoVenue_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName("2019-01-01", "venue 1 name");

        Hearing hearing2 = createHearingWithDateAndVenueName("2019-01-01", null);
        hearing2.getValue().setVenue(null);

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        checkDocumentIsNotCreatedAndReturnsError("Unable to determine hearing venue");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayAnErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName(null, "venue 1 name");

        Hearing hearing2 = null;

        sscsCaseData.setHearings(Arrays.asList(hearing2, hearing1));

        checkDocumentIsNotCreatedAndReturnsError("Unable to determine hearing date or venue");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDetails_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName(null, "venue 1 name");

        Hearing hearing2 = Hearing.builder().build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        checkDocumentIsNotCreatedAndReturnsError("Unable to determine hearing date or venue");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithEmptyHearingsList_thenDefaultHearingData(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        checkDefaultHearingDataForNullOrEmptyHearings(nextHearingTypeText);
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithNullHearingsList_thenDefaultHearingData(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        sscsCaseData.setHearings(null);

        checkDefaultHearingDataForNullOrEmptyHearings(nextHearingTypeText);
    }

    private void checkDefaultHearingDataForNullOrEmptyHearings(String nextHearingTypeText) {
        final PreSubmitCallbackResponse<SscsCaseData> response = previewNoticeDoNotShowIssueDate();

        assertThat(response.getErrors()).isEmpty();
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertThat(body).isNotNull();

        assertThat(body.getHeldOn()).hasToString(LocalDate.now().toString());
        assertThat(body.getHeldAt()).isEqualTo("In chambers");

        assertThat(response.getData().getAdjournment().getPreviewDocument()).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithHearingDates_thenCorrectlySetTheHeldOnUsingTheFirstHearingInList(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName("2019-01-01", "Venue Name");

        Hearing hearing2 = createHearingWithDateAndVenueName("2019-01-02", "Venue Name");

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

        assertThat(body.getHeldOn()).hasToString("2019-01-02");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDate_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName("2019-01-01", "Venue Name");

        Hearing hearing2 = createHearingWithDateAndVenueName(null, "Venue Name");

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);


        checkDocumentIsNotCreatedAndReturnsError("Unable to determine hearing date");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayTwoErrorsAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName("2019-01-01", "Venue Name");

        Hearing hearing2 = null;

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        checkDocumentIsNotCreatedAndReturnsError("Unable to determine hearing date or venue");
    }

    @ParameterizedTest
    @MethodSource("oralNextHearingTypeParameters")
    void givenCaseWithDurationParameterButMissingUnitsWhenOralHearing_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);
        adjournment.setNextHearingListingDuration(2);

        checkDocumentIsNotCreatedAndReturnsError("Timeslot duration units not supplied on case data");
    }

    @Nested
    class Timeslot {

        private void checkOralHearingTimeslot(String nextHearingTypeText, String nextHearingType, String expected) {
            AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

            if (isOralHearing(nextHearingType)) {
                assertThat(body.getNextHearingTimeslot()).isEqualTo(expected);
            } else {
                assertThat(body.getNextHearingTimeslot()).isNull();
            }
        }

        @ParameterizedTest
        @MethodSource(ALL_NEXT_HEARING_TYPE_PARAMETERS)
        void givenCaseWithNoDurationEnumSource_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {
            setAdjournmentNextHearingType(nextHearingType);

            AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

            if (isOralHearing(nextHearingType)) {
                assertThat(body.getNextHearingTimeslot()).isEqualTo("a standard time slot");
            }
        }

        @Test
        void givenCaseWithDurationParameterButMissingUnitsWhenPaperHearing_thenDisplayErrorAndDoNotGenerateDocument() {
            setAdjournmentNextHearingType("paper");
            adjournment.setNextHearingListingDuration(2);

            AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText("decision on the papers");

            assertThat(body.getNextHearingTimeslot()).isNull();
        }

        @ParameterizedTest
        @MethodSource(ALL_NEXT_HEARING_TYPE_PARAMETERS)
        void givenCaseWith120MinutesDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {
            setAdjournmentNextHearingType(nextHearingType);

            adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.MINUTES);
            adjournment.setNextHearingListingDuration(120);

            checkOralHearingTimeslot(nextHearingTypeText, nextHearingType, "120 minutes");
        }

        @ParameterizedTest
        @MethodSource(ALL_NEXT_HEARING_TYPE_PARAMETERS)
        void givenCaseWithOneMinuteDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {
            setAdjournmentNextHearingType(nextHearingType);

            adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.MINUTES);
            adjournment.setNextHearingListingDuration(1);

            checkOralHearingTimeslot(nextHearingTypeText, nextHearingType, "1 minute");
        }

        @ParameterizedTest
        @MethodSource(ALL_NEXT_HEARING_TYPE_PARAMETERS)
        void givenCaseWithTwoSessionDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {
            setAdjournmentNextHearingType(nextHearingType);

            adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS);
            adjournment.setNextHearingListingDuration(2);

            checkOralHearingTimeslot(nextHearingTypeText, nextHearingType, "2 sessions");
        }

        @ParameterizedTest
        @MethodSource(ALL_NEXT_HEARING_TYPE_PARAMETERS)
        void givenCaseWithOneSessionDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {
            setAdjournmentNextHearingType(nextHearingType);

            adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS);
            adjournment.setNextHearingListingDuration(1);

            checkOralHearingTimeslot(nextHearingTypeText, nextHearingType, "1 session");
        }
    }

    @Nested
    class PanelMembers {

        private void checkBodyHeldBefore(String nextHearingTypeText, String expected) {
            AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

            assertThat(body.getHeldBefore()).isEqualTo(expected);
        }

        @ParameterizedTest
        @MethodSource(ALL_NEXT_HEARING_TYPE_PARAMETERS)
        void givenCaseWithThreePanelMembers_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {
            setAdjournmentNextHearingType(nextHearingType);

            adjournment.setDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
            adjournment.setMedicallyQualifiedPanelMemberName("Ms Panel Member 2");
            adjournment.setOtherPanelMemberName("Other Panel Member");

            checkBodyHeldBefore(nextHearingTypeText, "Judge Full Name, Mr Panel Member 1, Ms Panel Member 2 and Other Panel Member");
        }

        @ParameterizedTest
        @MethodSource(ALL_NEXT_HEARING_TYPE_PARAMETERS)
        void givenCaseWithTwoPanelMembers_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {
            setAdjournmentNextHearingType(nextHearingType);

            adjournment.setDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
            adjournment.setMedicallyQualifiedPanelMemberName("Ms Panel Member 2");

            checkBodyHeldBefore(nextHearingTypeText, "Judge Full Name, Mr Panel Member 1 and Ms Panel Member 2");
        }


        @ParameterizedTest
        @MethodSource(ALL_NEXT_HEARING_TYPE_PARAMETERS)
        void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {
            setAdjournmentNextHearingType(nextHearingType);

            adjournment.setDisabilityQualifiedPanelMemberName("Mr Panel Member 1");

            checkBodyHeldBefore(nextHearingTypeText, "Judge Full Name and Mr Panel Member 1");
        }

        @ParameterizedTest
        @MethodSource(ALL_NEXT_HEARING_TYPE_PARAMETERS)
        void givenCaseWithNoPanelMembersWithNullValues_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {
            setAdjournmentNextHearingType(nextHearingType);

            checkBodyHeldBefore(nextHearingTypeText, "Judge Full Name");
        }

        @ParameterizedTest
        @MethodSource(ALL_NEXT_HEARING_TYPE_PARAMETERS)
        void givenCaseWithNoPanelMembersWithEmptyValues_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {
            setAdjournmentNextHearingType(nextHearingType);

            adjournment.setMedicallyQualifiedPanelMemberName("");
            adjournment.setDisabilityQualifiedPanelMemberName("");

            checkBodyHeldBefore(nextHearingTypeText, "Judge Full Name");
        }
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithNoSelectedVenueNotSetForFaceToFace_thenCorrectlySetTheVenueToBeTheExistingVenue(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        previewNoticeShowIssueDate();

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertThat(templateBody.getAdjournCaseTemplateBody().getNextHearingVenue()).isEqualTo("Gap venue name");
    }

    @ParameterizedTest
    @MethodSource("nonFaceToFaceNextHearingTypeParameters")
    void givenCaseWithNoSelectedVenueNotSetForNonFaceToFace_thenCorrectlySetTheVenueToBeNull(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        previewNoticeShowIssueDate();

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        setAdjournmentHearingFirstOnSessionAtSpecificTime("am");

        previewNoticeShowIssueDate();

        checkTemplateBodyNextHearingDate("It will be first in the morning session on the first available date");
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        setAdjournmentHearingFirstOnSessionAtSpecificTime("pm");

        previewNoticeShowIssueDate();

        checkTemplateBodyNextHearingDate("It will be first in the afternoon session on the first available date");
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionNotSelectedAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        setAdjournmentCaseTimeWithSessionsAndSpecificTime(List.of(), null);

        previewNoticeShowIssueDate();

        checkTemplateBodyNextHearingDate("It will be re-scheduled on the first available date");
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionSelectedAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        setAdjournmentHearingFirstOnSessionWithNoSpecificTime();

        previewNoticeShowIssueDate();

        checkTemplateBodyNextHearingDate("It will be first in the session on the first available date");
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
            setAdjournmentHearingFirstOnSessionAtSpecificTime("am");

            previewNoticeShowIssueDate();

            String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            checkTemplateBodyNextHearingDate("It will be first in the morning session on the first available date after " + expectedDate);
        }
    
        @Test
        void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
            setAdjournmentHearingFirstOnSessionAtSpecificTime("pm");

            previewNoticeShowIssueDate();

            String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            checkTemplateBodyNextHearingDate("It will be first in the afternoon session on the first available date after " + expectedDate);
        }
    
        @Test
        void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
            setAdjournmentHearingFirstOnSessionWithNoSpecificTime();

            previewNoticeShowIssueDate();

            String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            checkTemplateBodyNextHearingDate("It will be first in the session on the first available date after " + expectedDate);
        }
    
        @Test
        void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
            adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());

            previewNoticeShowIssueDate();

            String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            checkTemplateBodyNextHearingDate("It will be in the morning session on the first available date after " + expectedDate);
        }
    
        @Test
        void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
            adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());

            previewNoticeShowIssueDate();

            String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            checkTemplateBodyNextHearingDate("It will be in the afternoon session on the first available date after " + expectedDate);
        }
    
        @Test
        void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
            adjournment.setTime(AdjournCaseTime.builder().build());

            previewNoticeShowIssueDate();

            String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            checkTemplateBodyNextHearingDate("It will be re-scheduled on the first available date after " + expectedDate);
        }

        @Test
        void givenCaseWithFirstAvailableDateAfterWithProvidePeriodAndNoPeriodSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {
            adjournment.setNextHearingFirstAvailableDateAfterPeriod(null);

            previewNoticeDoNotShowIssueDate();

            checkDocumentIsNotCreatedAndReturnsError("No value set for adjournCaseNextHearingFirstAvailableDateAfterPeriod in case data");
        }
    }

    @Nested
    class FirstAvailableDateAfterDate {

        @BeforeEach
        void setUp() {
            adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
            adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
            adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));
        }

        @Test
        void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
            setAdjournmentHearingFirstOnSessionAtSpecificTime("am");

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be first in the morning session on the first available date after 01/01/2020");
        }

        @Test
        void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
            setAdjournmentHearingFirstOnSessionAtSpecificTime("pm");

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be first in the afternoon session on the first available date after 01/01/2020");
        }

        @Test
        void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
            setAdjournmentHearingFirstOnSessionWithNoSpecificTime();

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be first in the session on the first available date after 01/01/2020");
        }

        @Test
        void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
            setAdjournmentCaseTimeWithSessionsAndSpecificTime(null, "am");

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be in the morning session on the first available date after 01/01/2020");
        }

        @Test
        void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
            setAdjournmentCaseTimeWithSessionsAndSpecificTime(null, "pm");

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be in the afternoon session on the first available date after 01/01/2020");
        }

        @Test
        void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
            adjournment.setTime(AdjournCaseTime.builder().build());

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be re-scheduled on the first available date after 01/01/2020");
        }
    }

    @Nested
    class DateToBeFixed {

        @BeforeEach
        void setUp() {
            adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);
        }

        @Test
        void givenCaseWithDateToBeFixedAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
            adjournment.setTime(AdjournCaseTime.builder().build());

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be re-scheduled on a date to be fixed");
        }

        @Test
        void givenCaseWithDateToBeFixedAndFirstOnSessionAndMorningSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {
            setAdjournmentHearingFirstOnSessionAtSpecificTime("am");

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be first in the morning session on a date to be fixed");
        }

        @Test
        void givenCaseWithDateToBeFixedAndFirstOnSessionAndAfternoonSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {
            setAdjournmentHearingFirstOnSessionAtSpecificTime("pm");

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be first in the afternoon session on a date to be fixed");
        }

        @Test
        void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndMorningSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {
            adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be in the morning session on a date to be fixed");
        }

        @Test
        void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndAfternoonSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {
            adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be in the afternoon session on a date to be fixed");
        }

        @Test
        void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFixedDate() {
            adjournment.setTime(AdjournCaseTime.builder().build());

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be re-scheduled on a date to be fixed");
        }

        @Test
        void givenCaseWithDateToBeFixedAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
            setAdjournmentHearingFirstOnSessionWithNoSpecificTime();

            previewNoticeShowIssueDate();

            checkTemplateBodyNextHearingDate("It will be first in the session on a date to be fixed");
        }
    }

    @Test
    void givenCaseWithFirstAvailableDateAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDate() {
        adjournment.setTime(AdjournCaseTime.builder().build());

        previewNoticeShowIssueDate();

        checkTemplateBodyNextHearingDate("It will be re-scheduled on the first available date");
    }

    @Test
    void givenCaseWithFirstAvailableDateAndNotAdjournCaseTimeAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDateAfterDate() {
        adjournment.setTime(null);

        previewNoticeShowIssueDate();

        checkTemplateBodyNextHearingDate("It will be re-scheduled on the first available date");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithNoDateOrPeriodIndicator_ThenDisplayErrorAndDoNotDisplayTheDocument() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        checkDocumentIsNotCreatedAndReturnsError("Date or period indicator not available in case data");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithProvideDateAndNoDateSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);

        checkDocumentIsNotCreatedAndReturnsError("No value set for adjournCaseNextHearingFirstAvailableDateAfterDate in case data");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithNeitherProvideDateOrPeriodSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);

        checkDocumentIsNotCreatedAndReturnsError("Date or period indicator not available in case data");
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSameVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeThePreviousVenue(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        adjournment.setNextHearingVenue(SAME_VENUE);
        adjournment.setNextHearingVenueSelected(list);

        previewNoticeShowIssueDate();

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertThat(templateBody.getAdjournCaseTemplateBody().getNextHearingVenue()).isEqualTo("Gap venue name");
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeTheNewVenue(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        previewNoticeShowIssueDate();

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertThat(templateBody.getAdjournCaseTemplateBody().getNextHearingVenue()).isEqualTo("Gap venue name");
    }

    @ParameterizedTest
    @MethodSource("nonFaceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueSetForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        final PreSubmitCallbackResponse<SscsCaseData> response = previewNoticeShowIssueDate();

        String error = previewNoticeDoNotShowIssueDate().getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("adjournCaseNextHearingVenueSelected field should not be set");
        assertThat(response.getData().getAdjournment().getPreviewDocument()).isNull();
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueSetIncorrectlyForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(listItem, List.of(listItem));

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        checkDocumentIsNotCreatedAndReturnsError("Unable to load venue details for id:someUnknownVenueId");
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueMissingListItemForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(null, List.of(listItem));

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        checkDocumentIsNotCreatedAndReturnsError("A next hearing venue of somewhere else has been specified but no venue has been selected");
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueMissingListItemCodeForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(new DynamicListItem(null, ""), List.of(listItem));

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        checkDocumentIsNotCreatedAndReturnsError("A next hearing venue of somewhere else has been specified but no venue has been selected");
    }

    @ParameterizedTest
    @MethodSource("nonFaceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueSetIncorrectlyForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        setAdjournmentNextHearingType(nextHearingType);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(listItem, List.of(listItem));

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        checkDocumentIsNotCreatedAndReturnsError("adjournCaseNextHearingVenueSelected field should not be set");
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void scottishRpcWillShowAScottishImage(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        previewNoticeShowIssueDate();

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

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appointee Surname, appointee for Appellant Lastname", nextHearingTypeText, true);
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenDateIssuedParameterIsTrue_thenShowIssuedDateOnDocument(String nextHearingType, String nextHearingTypeText) {
        setAdjournmentNextHearingType(nextHearingType);

        previewNoticeShowIssueDate();

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertThat(payload.getDateIssued()).isEqualTo(LocalDate.now());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenGeneratedDateIsAlreadySetForGeneratedFlow_thenDoSetNewGeneratedDate(String nextHearingType, String nextHearingTypeText) {
        adjournment.setGeneratedDate(LOCAL_DATE);
        setAdjournmentNextHearingType(nextHearingType);

        previewNoticeShowIssueDate();

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertThat(payload.getGeneratedDate()).hasToString(LocalDate.now().toString());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenGeneratedDateIsAlreadySetNonGeneratedFlow_thenDoSetNewGeneratedDate(String nextHearingType, String nextHearingTypeText) {
        adjournment.setGenerateNotice(NO);
        adjournment.setGeneratedDate(LOCAL_DATE);
        setAdjournmentNextHearingType(nextHearingType);

        previewNoticeShowIssueDate();

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertThat(payload.getGeneratedDate()).hasToString(LocalDate.now().toString());
    }

}
