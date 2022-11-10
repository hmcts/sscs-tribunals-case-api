package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
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

    private static boolean isOralHearing(AdjournCaseTypeOfHearing nextHearingType) {
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
        assertEquals(FACE_TO_FACE.getDescriptionEn(), body.getHearingType());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void willSetPreviewFileWithNullReasons_WhenReasonsListIsEmpty(AdjournCaseTypeOfHearing nextHearingType) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getAdjournment().setReasons(new ArrayList<>());

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

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
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void willSetPreviewFileWithNullReasons_WhenReasonsListIsNotEmpty(AdjournCaseTypeOfHearing nextHearingType) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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
            NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

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
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void willSetPreviewFileWithInterpreterDescription_WhenInterpreterRequiredAndLanguageIsSet(AdjournCaseTypeOfHearing nextHearingType) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setInterpreterRequired(YES);
        sscsCaseData.getAdjournment().setInterpreterLanguage(new DynamicList(new DynamicListItem("french", "French"), List.of()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

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
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void willNotSetPreviewFileButWillDisplayError_WithInterpreterDescription_WhenInterpreterRequiredAndLanguageIsNotSet(AdjournCaseTypeOfHearing nextHearingType) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setInterpreterRequired(YES);


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void willSetPreviewFileWithoutInterpreterDescription_WhenInterpreterNotRequiredAndLanguageIsSet(AdjournCaseTypeOfHearing nextHearingType) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setInterpreterRequired(NO);
        sscsCaseData.getAdjournment().setInterpreterLanguage(new DynamicList(new DynamicListItem("french", "French"), List.of()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

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
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void willSetPreviewFileWithoutInterpreterDescription_WhenInterpreterRequiredNotSetAndLanguageIsSet(AdjournCaseTypeOfHearing nextHearingType) {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setInterpreterLanguage(new DynamicList(new DynamicListItem("french", "French"), List.of()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

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
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenSignedInJudgeUserDetailsNotSet_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenGenerateNoticeNotSet_willNotSetPreviewFile(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithMultipleHearingsWithVenues_thenCorrectlySetHeldAtUsingTheFirstHearingInList(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("venue 2 name").build()).build()).build();

        List<Hearing> hearings = List.of(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);
        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Gap venue name", body.getHeldAt());

    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(null);;
        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().build()).build()).build();

        List<Hearing> hearings = List.of(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithMultipleHearingsWithFirstInListWithNoVenue_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").build()).build();

        List<Hearing> hearings = List.of(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayAnErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = null;

        sscsCaseData.setHearings(List.of(hearing2, hearing1));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(
            callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDetails_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().build();

        List<Hearing> hearings = List.of(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithEmptyHearingsList_thenDefaultHearingData(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals("In chambers", body.getHeldAt());

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithNullHearingsList_thenDefaultHearingData(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals(LocalDate.now().toString(), body.getHeldOn().toString());
        assertEquals("In chambers", body.getHeldAt());

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithMultipleHearingsWithHearingDates_thenCorrectlySetTheHeldOnUsingTheFirstHearingInList(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("Venue Name").build()).build()).build();

        List<Hearing> hearings = List.of(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournment().getPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournment().getPreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("2019-01-02", body.getHeldOn().toString());

    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDate_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("Venue Name").build())
            .build()).build();

        List<Hearing> hearings = List.of(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayTwoErrorsAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01")
            .venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = null;

        List<Hearing> hearings = List.of(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournment().getPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
        assertNull(response.getData().getAdjournment().getPreviewDocument());

    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithNoDurationEnumSource_thenCorrectlySetTheNextHearingTimeslot(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

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
    @EnumSource(value = AdjournCaseTypeOfHearing.class, names = "PAPER", mode = EXCLUDE)
    void givenCaseWithDurationParameterButMissingUnitsWhenOralHearing_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);
        sscsCaseData.getAdjournment().setNextHearingListingDuration(2);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Timeslot duration units not supplied on case data", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @Test
    void givenCaseWithDurationParameterButMissingUnitsWhenPaperHearing_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(AdjournCaseTypeOfHearing.PAPER);
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", AdjournCaseTypeOfHearing.PAPER.getDescriptionEn(), true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertNull(body.getNextHearingTimeslot());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithTwoHourDuration_thenCorrectlySetTheNextHearingTimeslot(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        if (isOralHearing(nextHearingType)) {
            assertEquals("2 hours", body.getNextHearingTimeslot());
        } else {
            assertNull(body.getNextHearingTimeslot());
        }
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithSixtyMinuteDuration_thenCorrectlySetTheNextHearingTimeslot(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

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
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithTwoSessionDuration_thenCorrectlySetTheNextHearingTimeslot(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

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
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithOneSessionDuration_thenCorrectlySetTheNextHearingTimeslot(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

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
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithThreePanelMembers_thenCorrectlySetTheHeldBefore(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1, Ms Panel Member 2 and Other Panel Member", body.getHeldBefore());

    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithTwoPanelMembers_thenCorrectlySetTheHeldBefore(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1 and Ms Panel Member 2", body.getHeldBefore());

    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name and Mr Panel Member 1", body.getHeldBefore());

    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithNoPanelMembersWithNullValues_thenCorrectlySetTheHeldBefore(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());

    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithNoPanelMembersWithEmptyValues_thenCorrectlySetTheHeldBefore(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class, names = "FACE_TO_FACE")
    void givenCaseWithNoSelectedVenueNotSetForFaceToFace_thenCorrectlySetTheVenueToBeTheExistingVenue(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);
        assertEquals("Gap venue name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class, names = "FACE_TO_FACE", mode = EXCLUDE)
    void givenCaseWithNoSelectedVenueNotSetForNonFaceToFace_thenCorrectlySetTheVenueToBeNull(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);

        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);
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

    @Test
    void givenCaseWithFirstWithInvalidDateTime_thenDisplayAnErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(FACE_TO_FACE);
        sscsCaseData.getAdjournment().setNextHearingDateType(AdjournCaseNextHearingDateType.valueOf("unknownDateType"));

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unknown next hearing date type for:unknownDateType", error);
        assertNull(response.getData().getAdjournment().getPreviewDocument());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class, names = "FACE_TO_FACE")
    void givenCaseWithSameVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeThePreviousVenue(AdjournCaseTypeOfHearing nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        sscsCaseData.getAdjournment().setNextHearingVenue(SAME_VENUE);
        sscsCaseData.getAdjournment().setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
                .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);
        assertEquals("Gap venue name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class, names = "FACE_TO_FACE")
    void givenCaseWithSelectedVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeTheNewVenue(AdjournCaseTypeOfHearing nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        sscsCaseData.getAdjournment().setNextHearingVenue(SOMEWHERE_ELSE);
        sscsCaseData.getAdjournment().setNextHearingVenueSelected(list);

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);
        assertEquals("Gap venue name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class, names = "FACE_TO_FACE", mode = EXCLUDE)
    void givenCaseWithSelectedVenueSetForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(AdjournCaseTypeOfHearing nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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
    @EnumSource(value = AdjournCaseTypeOfHearing.class, names = "FACE_TO_FACE")
    void givenCaseWithSelectedVenueSetIncorrectlyForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(AdjournCaseTypeOfHearing nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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
    @EnumSource(value = AdjournCaseTypeOfHearing.class, names = "FACE_TO_FACE")
    void givenCaseWithSelectedVenueMissingListItemForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(AdjournCaseTypeOfHearing nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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
    @EnumSource(value = AdjournCaseTypeOfHearing.class, names = "FACE_TO_FACE")
    void givenCaseWithSelectedVenueMissingListItemCodeForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(AdjournCaseTypeOfHearing nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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
    @EnumSource(value = AdjournCaseTypeOfHearing.class, names = "FACE_TO_FACE", mode = EXCLUDE)
    void givenCaseWithSelectedVenueSetIncorrectlyForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(AdjournCaseTypeOfHearing nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void scottishRpcWillShowAScottishImage(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        sscsCaseData.setHearings(List.of(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        verifyTemplateBody(NoticeIssuedTemplateBody.SCOTTISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppended(AdjournCaseTypeOfHearing nextHearingType) {

        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
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

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appointee Surname, appointee for Appellant Lastname", nextHearingType.getDescriptionEn(), true);
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenDateIssuedParameterIsTrue_thenShowIssuedDateOnDocument(AdjournCaseTypeOfHearing nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

        assertEquals(LocalDate.now(), payload.getDateIssued());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenGeneratedDateIsAlreadySetForGeneratedFlow_thenDoSetNewGeneratedDate(AdjournCaseTypeOfHearing nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(YES);
        sscsCaseData.getAdjournment().setGeneratedDate(LOCAL_DATE);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

        assertEquals(LocalDate.now().toString(), payload.getGeneratedDate().toString());
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void givenGeneratedDateIsAlreadySetNonGeneratedFlow_thenDoSetNewGeneratedDate(AdjournCaseTypeOfHearing nextHearingType) {
        sscsCaseData.getAdjournment().setGenerateNotice(NO);
        sscsCaseData.getAdjournment().setGeneratedDate(LOCAL_DATE);
        sscsCaseData.getAdjournment().setTypeOfNextHearing(nextHearingType);
        sscsCaseData.getAdjournment().setNextHearingDateType(FIRST_AVAILABLE_DATE);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingType.getDescriptionEn(), true);

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
