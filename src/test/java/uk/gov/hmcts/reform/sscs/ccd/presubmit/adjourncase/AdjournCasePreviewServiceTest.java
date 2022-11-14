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
    public static final LocalDate LOCAL_DATE = LocalDate.parse("2018-10-10");
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
                .build()).build();

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
        return HearingType.FACE_TO_FACE.getKey().equals(nextHearingType.toString())
            || HearingType.TELEPHONE.getKey().equals(nextHearingType.toString())
            || HearingType.VIDEO.getKey().equals(nextHearingType.toString());
    }

    private void setCommonPreviewParams(SscsCaseData sscsCaseData, String endDate) {
        sscsCaseData.getAdjournment().setReasons(List.of(new CollectionItem<>(null, "My reasons for decision")));
        sscsCaseData.getAdjournment().setAdditionalDirections(List.of(new CollectionItem<>(null, "Something else.")));
        sscsCaseData.getAdjournment().setTypeOfHearing(FACE_TO_FACE);
        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
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
        assertEquals(FACE_TO_FACE.getDescriptionEn().toLowerCase(), body.getHearingType());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithNullReasons_WhenReasonsListIsEmpty(String nextHearingType, String nextHearingTypeText) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getAdjournment().setReasons(new ArrayList<>());

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

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

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithNullReasons_WhenReasonsListIsNotEmpty(String nextHearingType, String nextHearingTypeText) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(
            callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format(
                "Draft Adjournment Notice generated on %s.pdf",
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(
            NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals("Judge Full Name", payload.getUserName());
        assertEquals("DRAFT ADJOURNMENT NOTICE", payload.getNoticeType());

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

        assertNotNull(body);

        // Common assertions
        assertCommonPreviewParams(body, endDate, false);

        assertNull(payload.getDateIssued());
        assertEquals(LocalDate.now(), payload.getGeneratedDate());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithInterpreterDescription_WhenInterpreterRequiredAndLanguageIsSet(String nextHearingType, String nextHearingTypeText) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setInterpreterRequired(YES);
        sscsCaseData.getAdjournment().setInterpreterLanguage("french");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

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

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willNotSetPreviewFileButWillDisplayError_WithInterpreterDescription_WhenInterpreterRequiredAndLanguageIsNotSet(String nextHearingType) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setInterpreterRequired(YES);


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithoutInterpreterDescription_WhenInterpreterNotRequiredAndLanguageIsSet(String nextHearingType, String nextHearingTypeText) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setInterpreterRequired(NO);
        sscsCaseData.getAdjournment().setInterpreterLanguage("french");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

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

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void willSetPreviewFileWithoutInterpreterDescription_WhenInterpreterRequiredNotSetAndLanguageIsSet(String nextHearingType, String nextHearingTypeText) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setInterpreterLanguage("french");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

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


    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        when(userDetailsService.buildLoggedInUserName("Bearer token")).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenSignedInJudgeUserDetailsNotSet_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        when(userDetailsService.buildLoggedInUserName("Bearer token")).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenGenerateNoticeNotSet_willNotSetPreviewFile(String nextHearingType) {

        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithVenues_thenCorrectlySetHeldAtUsingTheFirstHearingInList(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("venue 2 name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Gap venue name", body.getHeldAt());

    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
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
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstInListWithNoVenue_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayAnErrorAndDoNotGenerateDocument(String nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = null;

        sscsCaseData.setHearings(Arrays.asList(hearing2, hearing1));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(
            callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDetails_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithEmptyHearingsList_thenDefaultHearingData(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

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

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

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

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("Venue Name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("2019-01-02", body.getHeldOn().toString());

    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDate_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

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
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayTwoErrorsAndDoNotGenerateDocument(String nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01")
            .venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = null;

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournment().getPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithNoDurationEnumSource_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        final boolean isOralHearing = HearingType.FACE_TO_FACE.getKey().equals(nextHearingType.toString())
            || HearingType.TELEPHONE.getKey().equals(nextHearingType.toString())
            || HearingType.VIDEO.getKey().equals(nextHearingType.toString());

        if (isOralHearing) {
            assertEquals("a standard time slot", body.getNextHearingTimeslot());
        }
    }

    @ParameterizedTest
    @MethodSource("oralNextHearingTypeParameters")
    void givenCaseWithDurationParameterButMissingUnitsWhenOralHearing_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setNextHearingListingDuration(2);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Timeslot duration units not supplied on case data", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("paperNextHearingTypeParameters")
    void givenCaseWithDurationParameterButMissingUnitsWhenPaperHearing_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setNextHearingListingDuration(2);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertNull(body.getNextHearingTimeslot());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithTwoHourDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.getAdjournment().setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.MINUTES);
        sscsCaseData.getAdjournment().setNextHearingListingDuration(120);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

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

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.getAdjournment().setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.MINUTES);
        sscsCaseData.getAdjournment().setNextHearingListingDuration(60);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        final boolean isOralHearing = isOralHearing(nextHearingType);

        if (isOralHearing) {
            assertEquals("60 minutes", body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
    }



    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithTwoSessionDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.getAdjournment().setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS);
        sscsCaseData.getAdjournment().setNextHearingListingDuration(2);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        final boolean isOralHearing = isOralHearing(nextHearingType);

        if (isOralHearing) {
            assertEquals("2 sessions", body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithOneSessionDuration_thenCorrectlySetTheNextHearingTimeslot(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.getAdjournment().setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS);
        sscsCaseData.getAdjournment().setNextHearingListingDuration(1);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        final boolean isOralHearing = isOralHearing(nextHearingType);

        if (isOralHearing) {
            assertEquals("1 session", body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithThreePanelMembers_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.getAdjournment().setDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        sscsCaseData.getAdjournment().setMedicallyQualifiedPanelMemberName("Ms Panel Member 2");
        sscsCaseData.getAdjournment().setOtherPanelMemberName("Other Panel Member");

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1, Ms Panel Member 2 and Other Panel Member", body.getHeldBefore());

    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithTwoPanelMembers_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.getAdjournment().setDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        sscsCaseData.getAdjournment().setMedicallyQualifiedPanelMemberName("Ms Panel Member 2");

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1 and Ms Panel Member 2", body.getHeldBefore());

    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.getAdjournment().setDisabilityQualifiedPanelMemberName("Mr Panel Member 1");

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name and Mr Panel Member 1", body.getHeldBefore());

    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithNoPanelMembersWithNullValues_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setGenerateNotice(YES);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());

    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithNoPanelMembersWithEmptyValues_thenCorrectlySetTheHeldBefore(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setMedicallyQualifiedPanelMemberName("");
        sscsCaseData.getAdjournment().setDisabilityQualifiedPanelMemberName("");

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithNoSelectedVenueNotSetForFaceToFace_thenCorrectlySetTheVenueToBeTheExistingVenue(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("Gap venue name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @ParameterizedTest
    @MethodSource("nonFaceToFaceNextHearingTypeParameters")
    void givenCaseWithNoSelectedVenueNotSetForNonFaceToFace_thenCorrectlySetTheVenueToBeNull(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));

        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
    }

    // Scenarios for next hearing date
    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the morning session on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the afternoon session on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionNotSelectedAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        List<String> sessions = new ArrayList<>();
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionSelectedAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the session on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the morning session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the afternoon session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the morning session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the afternoon session on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the morning session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the afternoon session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);

        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the morning session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);

        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the afternoon session on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);

        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(DATE_TO_BE_FIXED);

        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndFirstOnSessionAndMorningSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(DATE_TO_BE_FIXED);

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the morning session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndFirstOnSessionAndAfternoonSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(DATE_TO_BE_FIXED);

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the afternoon session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndMorningSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(DATE_TO_BE_FIXED);

        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the morning session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndAfternoonSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(DATE_TO_BE_FIXED);

        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be in the afternoon session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDateAfterDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFixedDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(DATE_TO_BE_FIXED);
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAndNotAdjournCaseTimeAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDateAfterDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setTime(null);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be re-scheduled on the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithDateToBeFixedAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(DATE_TO_BE_FIXED);

        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        sscsCaseData.getAdjournment().setTime(AdjournCaseTime.builder().adjournCaseNextHearingFirstOnSession(sessions).build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face hearing", true);
        assertEquals("It will be first in the session on a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithNoDateOrPeriodIndicator_ThenDisplayErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Date or period indicator not available in case data", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithProvideDateAndNoDateSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_DATE);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("No value set for adjournCaseNextHearingFirstAvailableDateAfterDate in case data", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithProvidePeriodAndNoPeriodSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        sscsCaseData.getAdjournment().setNextHearingDateOrPeriod(PROVIDE_PERIOD);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("No value set for adjournCaseNextHearingFirstAvailableDateAfterPeriod in case data", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithNeitherProvideDateOrPeriodSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Date or period indicator not available in case data", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSameVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeThePreviousVenue(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        sscsCaseData.getAdjournment().setNextHearingVenue(SAME_VENUE);
        sscsCaseData.getAdjournment().setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("Gap venue name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeTheNewVenue(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        sscsCaseData.getAdjournment().setNextHearingVenue(SOMEWHERE_ELSE);
        sscsCaseData.getAdjournment().setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("Gap venue name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @ParameterizedTest
    @MethodSource("nonFaceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueSetForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        sscsCaseData.getAdjournment().setNextHearingVenue(SOMEWHERE_ELSE);
        sscsCaseData.getAdjournment().setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("adjournCaseNextHearingVenueSelected field should not be set", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueSetIncorrectlyForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(listItem, List.of(listItem));

        sscsCaseData.getAdjournment().setNextHearingVenue(SOMEWHERE_ELSE);
        sscsCaseData.getAdjournment().setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to load venue details for id:someUnknownVenueId", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueMissingListItemForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(null, List.of(listItem));

        sscsCaseData.getAdjournment().setNextHearingVenue(SOMEWHERE_ELSE);
        sscsCaseData.getAdjournment().setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("A next hearing venue of somewhere else has been specified but no venue has been selected", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("faceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueMissingListItemCodeForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(new DynamicListItem(null, ""), List.of(listItem));

        sscsCaseData.getAdjournment().setNextHearingVenue(SOMEWHERE_ELSE);
        sscsCaseData.getAdjournment().setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("A next hearing venue of somewhere else has been specified but no venue has been selected", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("nonFaceToFaceNextHearingTypeParameters")
    void givenCaseWithSelectedVenueSetIncorrectlyForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(listItem, List.of(listItem));

        sscsCaseData.getAdjournment().setNextHearingVenue(SOMEWHERE_ELSE);
        sscsCaseData.getAdjournment().setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("adjournCaseNextHearingVenueSelected field should not be set", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void scottishRpcWillShowAScottishImage(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        verifyTemplateBody(NoticeIssuedTemplateBody.SCOTTISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppended(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
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
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals(LocalDate.now(), payload.getDateIssued());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenGeneratedDateIsAlreadySetForGeneratedFlow_thenDoSetNewGeneratedDate(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setGeneratedDate(LOCAL_DATE);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void givenGeneratedDateIsAlreadySetNonGeneratedFlow_thenDoSetNewGeneratedDate(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.getAdjournment().setGenerateNotice(NO);
        sscsCaseData.getAdjournment().setGeneratedDate(LOCAL_DATE);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.getTypeOfHearingByCcdDefinition(nextHearingType));
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

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
