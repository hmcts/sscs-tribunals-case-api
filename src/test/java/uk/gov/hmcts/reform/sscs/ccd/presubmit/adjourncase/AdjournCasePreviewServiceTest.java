package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class AdjournCasePreviewServiceTest {

    private static final String ADDITIONAL_DIRECTIONS = "Something else.";
    private static final String APPELLANT_FULL_NAME = "Appellant Lastname";
    private static final String GAP_VENUE_NAME = "Gap venue name";
    private static final String HEARING_DATE = "2019-01-01";
    private static final String JUDGE_FULL_NAME = "Judge Full Name";
    private static final String REASONS = "My reasons for decision";
    private static final String TEMPLATE_ID = "nuts.docx";
    private static final String URL = "http://dm-store/documents/123";
    private static final String USER_AUTHORISATION = "Bearer token";
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

    Map<String, VenueDetails> venueDetailsMap;

    @Mock
    private SignLanguagesService signLanguagesService;

    @BeforeEach
    void setUp() throws IOException {
        service = new AdjournCasePreviewService(generateFile, userDetailsService,
            venueDataLoader, new LanguageService(), TEMPLATE_ID, signLanguagesService);

        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        venueDetailsMap = new HashMap<>();
        VenueDetails venueDetails = VenueDetails.builder().venName("Venue Name").gapsVenName(GAP_VENUE_NAME).build();
        venueDetailsMap.put("someVenueId", venueDetails);

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
                .reasons(List.of(new CollectionItem<>(null, REASONS)))
                .additionalDirections(List.of(new CollectionItem<>(null, ADDITIONAL_DIRECTIONS)))
                .typeOfHearing(FACE_TO_FACE)
                .generateNotice(YES)
                .typeOfNextHearing(FACE_TO_FACE)
                .nextHearingDateType(FIRST_AVAILABLE_DATE)
                .build())
            .hearings(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingDate(HEARING_DATE)
                    .venue(Venue.builder()
                        .name("Venue Name")
                        .build())
                    .venueId("someVenueId").build())
                .build()))
            .build();
        
        adjournment = sscsCaseData.getAdjournment();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        capture = ArgumentCaptor.forClass(GenerateFileParams.class);
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

    private static boolean isOralHearing(AdjournCaseTypeOfHearing nextHearingType) {
        return HearingType.FACE_TO_FACE.getKey().equals(nextHearingType.getCcdDefinition())
            || HearingType.TELEPHONE.getKey().equals(nextHearingType.getCcdDefinition())
            || HearingType.VIDEO.getKey().equals(nextHearingType.getCcdDefinition());
    }

    private void checkOralHearingTimeslot(String nextHearingTypeText, AdjournCaseTypeOfHearing nextHearingType, String expected) {
        AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

        if (isOralHearing(nextHearingType)) {
            assertThat(body.getNextHearingTimeslot()).isEqualTo(expected);
        } else {
            assertThat(body.getNextHearingTimeslot()).isNull();
        }
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

    private void setAdjournmentHearingFirstOnSessionAtSpecificTime(String specificTime) {
        List<String> sessions = new ArrayList<>();
        sessions.add("firstOnSession");
        adjournment.setTime(AdjournCaseTime.builder()
            .adjournCaseNextHearingSpecificTime(specificTime)
            .adjournCaseNextHearingFirstOnSession(sessions)
            .build());
    }

    private static AdjournCaseTemplateBody checkCommonPreviewParams(NoticeIssuedTemplateBody payload) {
        assertThat(payload.getUserName()).isEqualTo(JUDGE_FULL_NAME);
        assertThat(payload.getNoticeType()).isEqualTo("DRAFT ADJOURNMENT NOTICE");

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();

        assertThat(body).isNotNull();
        assertThat(body.getAdditionalDirections().get(0)).isEqualTo(ADDITIONAL_DIRECTIONS);
        assertThat(body.getHearingType()).isEqualTo("faceToFace");
        assertThat(payload.getDateIssued()).isNull();
        assertThat(payload.getGeneratedDate()).isEqualTo(LocalDate.now());

        return body;
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
            APPELLANT_FULL_NAME,
            "face to face hearing",
            true);
        assertThat(body.getAdjournCaseTemplateBody().getNextHearingDate()).isEqualTo(expected);
    }

    @NotNull
    private AdjournCaseTemplateBody getAdjournCaseTemplateBodyWithHearingTypeText(String nextHearingTypeText) {
        final PreSubmitCallbackResponse<SscsCaseData> response =
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertThat(body).isNotNull();
        return body;
    }

    private void checkDocumentIsNotCreatedAndReturnsError(String expected) {
        final PreSubmitCallbackResponse<SscsCaseData> response =
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);
        String error = response.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo(expected);
        assertThat(response.getData().getAdjournment().getPreviewDocument()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = AdjournCaseTypeOfHearing.class)
    void willSetPreviewFileWithNullReasons_WhenReasonsListIsEmpty(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setReasons(new ArrayList<>());

        adjournment.setTypeOfNextHearing(nextHearingType);

        final PreSubmitCallbackResponse<SscsCaseData> response =
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        checkPreviewDocument(response);

        HearingType hearingType = HearingType.getByKey(nextHearingType.getCcdDefinition());
        String nextHearingTypeText = hearingType.getValue();
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);

        AdjournCaseTemplateBody body = checkCommonPreviewParams(payload);
        assertThat(body.getReasonsForDecision()).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void willSetPreviewFileWithNullReasons_WhenReasonsListIsNotEmpty(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        final PreSubmitCallbackResponse<SscsCaseData> response =
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(
            NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);

        AdjournCaseTemplateBody body = checkCommonPreviewParams(payload);
        assertThat(body.getReasonsForDecision()).isNotNull();
        assertThat(body.getReasonsForDecision()).isNotEmpty();
        assertThat(body.getReasonsForDecision().get(0)).isEqualTo(REASONS);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void willSetPreviewFileWithInterpreterDescription_WhenInterpreterRequiredAndLanguageIsSet(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setInterpreterRequired(YES);
        adjournment.setInterpreterLanguage(new DynamicList("French"));

        final PreSubmitCallbackResponse<SscsCaseData> response =
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);

        AdjournCaseTemplateBody body = checkCommonPreviewParams(payload);
        assertThat(body.getReasonsForDecision()).isNotNull();
        assertThat(body.getReasonsForDecision()).isNotEmpty();
        assertThat(body.getReasonsForDecision().get(0)).isEqualTo(REASONS);

        assertThat(body.getInterpreterDescription()).isEqualTo("an interpreter in French");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void willNotSetPreviewFileButWillDisplayError_WithInterpreterDescription_WhenInterpreterRequiredAndLanguageIsNotSet(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setInterpreterRequired(YES);

        checkDocumentIsNotCreatedAndReturnsError("An interpreter is required but no language is set");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void willSetPreviewFileWithoutInterpreterDescription_WhenInterpreterNotRequiredAndLanguageIsSet(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setInterpreterRequired(NO);
        adjournment.setInterpreterLanguage(new DynamicList("French"));

        final PreSubmitCallbackResponse<SscsCaseData> response =
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);

        AdjournCaseTemplateBody body = checkCommonPreviewParams(payload);
        assertThat(body.getReasonsForDecision()).isNotNull();
        assertThat(body.getReasonsForDecision()).isNotEmpty();
        assertThat(body.getReasonsForDecision().get(0)).isEqualTo(REASONS);

        assertThat(body.getInterpreterDescription()).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void willSetPreviewFileWithoutInterpreterDescription_WhenInterpreterRequiredNotSetAndLanguageIsSet(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setInterpreterLanguage(new DynamicList("French"));

        final PreSubmitCallbackResponse<SscsCaseData> response =
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        checkPreviewDocument(response);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);

        AdjournCaseTemplateBody body = checkCommonPreviewParams(payload);
        assertThat(body.getReasonsForDecision()).isNotNull();
        assertThat(body.getReasonsForDecision()).isNotEmpty();
        assertThat(body.getReasonsForDecision().get(0)).isEqualTo(REASONS);

        assertThat(body.getInterpreterDescription()).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        checkDocumentIsNotCreatedAndReturnsError("Unable to obtain signed in user details");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenSignedInJudgeUserDetailsNotSet_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenThrow(new IllegalStateException("Unable to obtain signed in user details"));

        checkDocumentIsNotCreatedAndReturnsError("Unable to obtain signed in user details");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenGenerateNoticeNotSet_willNotSetPreviewFile(AdjournCaseTypeOfHearing nextHearingType) {
        adjournment.setGenerateNotice(null);
        adjournment.setTypeOfNextHearing(nextHearingType);

        checkDocumentIsNotCreatedAndReturnsError("Generate notice has not been set");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithMultipleHearingsWithVenues_thenCorrectlySetHeldAtUsingTheFirstHearingInList(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName(HEARING_DATE, "venue 1 name");

        Hearing hearing2 = createHearingWithDateAndVenueName("2019-01-02", "venue 2 name");

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

        assertThat(body.getHeldAt()).isEqualTo(GAP_VENUE_NAME);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(null);
        Hearing hearing1 = createHearingWithDateAndVenueName(HEARING_DATE, "venue 1 name");

        Hearing hearing2 = createHearingWithDateAndVenueName("2019-01-02", null);

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        checkDocumentIsNotCreatedAndReturnsError("Unable to determine hearing venue");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithMultipleHearingsWithFirstInListWithNoVenue_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName(HEARING_DATE, "venue 1 name");

        Hearing hearing2 = createHearingWithDateAndVenueName(HEARING_DATE, null);
        hearing2.getValue().setVenue(null);

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        checkDocumentIsNotCreatedAndReturnsError("Unable to determine hearing venue");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayAnErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName(null, "venue 1 name");

        Hearing hearing2 = null;

        sscsCaseData.setHearings(Arrays.asList(hearing2, hearing1));

        checkDocumentIsNotCreatedAndReturnsError("Unable to determine hearing date or venue");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDetails_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName(null, "venue 1 name");

        Hearing hearing2 = Hearing.builder().build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        checkDocumentIsNotCreatedAndReturnsError("Unable to determine hearing date or venue");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithEmptyHearingsList_thenDefaultHearingData(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        checkDefaultHearingDataForNullOrEmptyHearings(nextHearingTypeText);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithNullHearingsList_thenDefaultHearingData(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        sscsCaseData.setHearings(null);

        checkDefaultHearingDataForNullOrEmptyHearings(nextHearingTypeText);
    }

    private void checkDefaultHearingDataForNullOrEmptyHearings(String nextHearingTypeText) {
        final PreSubmitCallbackResponse<SscsCaseData> response =
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertThat(response.getErrors()).isEmpty();
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertThat(body).isNotNull();

        assertThat(body.getHeldOn()).hasToString(LocalDate.now().toString());
        assertThat(body.getHeldAt()).isEqualTo("In chambers");

        assertThat(response.getData().getAdjournment().getPreviewDocument()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithMultipleHearingsWithHearingDates_thenCorrectlySetTheHeldOnUsingTheFirstHearingInList(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName(HEARING_DATE, "Venue Name");

        Hearing hearing2 = createHearingWithDateAndVenueName("2019-01-02", "Venue Name");

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

        assertThat(body.getHeldOn()).hasToString("2019-01-02");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDate_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName(HEARING_DATE, "Venue Name");

        Hearing hearing2 = createHearingWithDateAndVenueName(null, "Venue Name");

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        checkDocumentIsNotCreatedAndReturnsError("Unable to determine hearing date");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayTwoErrorsAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        Hearing hearing1 = createHearingWithDateAndVenueName(HEARING_DATE, "Venue Name");

        Hearing hearing2 = null;

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        checkDocumentIsNotCreatedAndReturnsError("Unable to determine hearing date or venue");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithDurationParameterButMissingUnitsWhenOralHearing_thenDisplayErrorAndDoNotGenerateDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setNextHearingListingDuration(2);

        checkDocumentIsNotCreatedAndReturnsError("Timeslot duration units not supplied on case data");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithNoDurationEnumSource_thenCorrectlySetTheNextHearingTimeslot(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

        if (isOralHearing(nextHearingType)) {
            assertThat(body.getNextHearingTimeslot()).isEqualTo("a standard time slot");
        }
    }

    @Test
    void givenCaseWithDurationParameterButMissingUnitsWhenPaperHearing_thenDisplayErrorAndDoNotGenerateDocument() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(AdjournCaseTypeOfHearing.PAPER);
        adjournment.setNextHearingListingDuration(2);

        AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText("decision on the papers");

        assertThat(body.getNextHearingTimeslot()).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWith120MinutesDuration_thenCorrectlySetTheNextHearingTimeslot(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.MINUTES);
        adjournment.setNextHearingListingDuration(120);

        checkOralHearingTimeslot(nextHearingTypeText, nextHearingType, "120 minutes");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithOneMinuteDuration_thenCorrectlySetTheNextHearingTimeslot(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.MINUTES);
        adjournment.setNextHearingListingDuration(1);

        checkOralHearingTimeslot(nextHearingTypeText, nextHearingType, "1 minute");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithTwoSessionDuration_thenCorrectlySetTheNextHearingTimeslot(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS);
        adjournment.setNextHearingListingDuration(2);

        checkOralHearingTimeslot(nextHearingTypeText, nextHearingType, "2 sessions");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithOneSessionDuration_thenCorrectlySetTheNextHearingTimeslot(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setNextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS);
        adjournment.setNextHearingListingDuration(1);

        checkOralHearingTimeslot(nextHearingTypeText, nextHearingType, "1 session");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithThreePanelMembers_thenCorrectlySetTheHeldBefore(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        adjournment.setMedicallyQualifiedPanelMemberName("Ms Panel Member 2");
        adjournment.setOtherPanelMemberName("Other Panel Member");

        AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

        assertThat(body.getHeldBefore()).isEqualTo("Judge Full Name, Mr Panel Member 1, Ms Panel Member 2 and Other Panel Member");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithTwoPanelMembers_thenCorrectlySetTheHeldBefore(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        adjournment.setMedicallyQualifiedPanelMemberName("Ms Panel Member 2");

        AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

        assertThat(body.getHeldBefore()).isEqualTo("Judge Full Name, Mr Panel Member 1 and Ms Panel Member 2");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setDisabilityQualifiedPanelMemberName("Mr Panel Member 1");

        AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

        assertThat(body.getHeldBefore()).isEqualTo("Judge Full Name and Mr Panel Member 1");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithNoPanelMembersWithNullValues_thenCorrectlySetTheHeldBefore(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

        assertThat(body.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithNoPanelMembersWithEmptyValues_thenCorrectlySetTheHeldBefore(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);
        adjournment.setMedicallyQualifiedPanelMemberName("");
        adjournment.setDisabilityQualifiedPanelMemberName("");

        AdjournCaseTemplateBody body = getAdjournCaseTemplateBodyWithHearingTypeText(nextHearingTypeText);

        assertThat(body.getHeldBefore()).isEqualTo(JUDGE_FULL_NAME);
    }

    @ParameterizedTest
    @CsvSource(value = {"FACE_TO_FACE,face to face hearing"})
    void givenCaseWithNoSelectedVenueNotSetForFaceToFace_thenCorrectlySetTheVenueToBeTheExistingVenue(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);
        assertThat(templateBody.getAdjournCaseTemplateBody().getNextHearingVenue()).isEqualTo(GAP_VENUE_NAME);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers"
    })
    void givenCaseWithNoSelectedVenueNotSetForNonFaceToFace_thenCorrectlySetTheVenueToBeNull(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        setAdjournmentHearingFirstOnSessionAtSpecificTime("am");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be first in the morning session on the first available date");
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        setAdjournmentHearingFirstOnSessionAtSpecificTime("pm");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be first in the afternoon session on the first available date");
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionNotSelectedAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(AdjournCaseTime.builder()
            .adjournCaseNextHearingSpecificTime(null)
            .adjournCaseNextHearingFirstOnSession(List.of())
            .build());

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be re-scheduled on the first available date");
    }

    @Test
    void givenCaseWithFirstAvailableDateAndFirstOnSessionSelectedAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        setAdjournmentHearingFirstOnSessionAtSpecificTime(null);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be first in the session on the first available date");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        setAdjournmentHearingFirstOnSessionAtSpecificTime("am");
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        adjournment.setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        checkTemplateBodyNextHearingDate("It will be first in the morning session on the first available date after " + expectedDate);
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        setAdjournmentHearingFirstOnSessionAtSpecificTime("pm");
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        adjournment.setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        checkTemplateBodyNextHearingDate("It will be first in the afternoon session on the first available date after " + expectedDate);
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterPeriodAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        setAdjournmentHearingFirstOnSessionAtSpecificTime(null);
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        adjournment.setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        checkTemplateBodyNextHearingDate("It will be first in the session on the first available date after " + expectedDate);
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        adjournment.setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        checkTemplateBodyNextHearingDate("It will be in the morning session on the first available date after " + expectedDate);
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        adjournment.setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        checkTemplateBodyNextHearingDate("It will be in the afternoon session on the first available date after " + expectedDate);
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterPeriodAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(AdjournCaseTime.builder().build());
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        adjournment.setNextHearingFirstAvailableDateAfterPeriod(TWENTY_EIGHT_DAYS);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        checkTemplateBodyNextHearingDate("It will be re-scheduled on the first available date after " + expectedDate);
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithProvidePeriodAndNoPeriodSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_PERIOD);
        adjournment.setNextHearingFirstAvailableDateAfterPeriod(null);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        checkDocumentIsNotCreatedAndReturnsError("No value set for adjournCaseNextHearingFirstAvailableDateAfterPeriod in case data");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        setAdjournmentHearingFirstOnSessionAtSpecificTime("am");
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be first in the morning session on the first available date after 01/01/2020");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        setAdjournmentHearingFirstOnSessionAtSpecificTime("pm");
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be first in the afternoon session on the first available date after 01/01/2020");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        setAdjournmentHearingFirstOnSessionAtSpecificTime(null);
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be first in the session on the first available date after 01/01/2020");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndMorningSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(AdjournCaseTime.builder()
            .adjournCaseNextHearingSpecificTime("am")
            .adjournCaseNextHearingFirstOnSession(null)
            .build());
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be in the morning session on the first available date after 01/01/2020");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndAfternoonSessionSelected_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(AdjournCaseTime.builder()
            .adjournCaseNextHearingSpecificTime("pm")
            .adjournCaseNextHearingFirstOnSession(null)
            .build());
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be in the afternoon session on the first available date after 01/01/2020");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(AdjournCaseTime.builder().build());
        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be re-scheduled on the first available date after 01/01/2020");
    }

    @Test
    void givenCaseWithDateToBeFixedAndNoFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(AdjournCaseTime.builder().build());
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be re-scheduled on a date to be fixed");
    }

    @Test
    void givenCaseWithDateToBeFixedAndFirstOnSessionAndMorningSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        setAdjournmentHearingFirstOnSessionAtSpecificTime("am");
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be first in the morning session on a date to be fixed");
    }

    @Test
    void givenCaseWithDateToBeFixedAndFirstOnSessionAndAfternoonSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        setAdjournmentHearingFirstOnSessionAtSpecificTime("pm");
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be first in the afternoon session on a date to be fixed");
    }

    @Test
    void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndMorningSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("am").build());
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be in the morning session on a date to be fixed");
    }

    @Test
    void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndAfternoonSessionProvided_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(AdjournCaseTime.builder().adjournCaseNextHearingSpecificTime("pm").build());
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be in the afternoon session on a date to be fixed");
    }

    @Test
    void givenCaseWithDateToBeFixedAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFixedDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(AdjournCaseTime.builder().build());
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be re-scheduled on a date to be fixed");
    }

    @Test
    void givenCaseWithDateToBeFixedAndFirstOnSessionAndNoTimeProvided_thenCorrectlyDisplayTheNextHearingDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        setAdjournmentHearingFirstOnSessionAtSpecificTime(null);
        adjournment.setNextHearingDateType(DATE_TO_BE_FIXED);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be first in the session on a date to be fixed");
    }

    @Test
    void givenCaseWithFirstAvailableDateAndNotFirstOnSessionAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(AdjournCaseTime.builder().build());

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be re-scheduled on the first available date");
    }

    @Test
    void givenCaseWithFirstAvailableDateAndNotAdjournCaseTimeAndNoAfternoonSessionProvided_thenCorrectlyDisplayTheFirstAvailableDateAfterDate() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTime(null);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        checkTemplateBodyNextHearingDate("It will be re-scheduled on the first available date");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithNoDateOrPeriodIndicator_ThenDisplayErrorAndDoNotDisplayTheDocument() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingFirstAvailableDateAfterDate(LocalDate.parse("2020-01-01"));

        checkDocumentIsNotCreatedAndReturnsError("Date or period indicator not available in case data");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithProvideDateAndNoDateSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);
        adjournment.setNextHearingDateOrPeriod(PROVIDE_DATE);

        checkDocumentIsNotCreatedAndReturnsError("No value set for adjournCaseNextHearingFirstAvailableDateAfterDate in case data");
    }

    @Test
    void givenCaseWithFirstAvailableDateAfterWithNeitherProvideDateOrPeriodSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument() {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setNextHearingDateType(FIRST_AVAILABLE_DATE_AFTER);

        checkDocumentIsNotCreatedAndReturnsError("Date or period indicator not available in case data");
    }

    @ParameterizedTest
    @CsvSource(value = {"FACE_TO_FACE,face to face hearing"})
    void givenCaseWithSameVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeThePreviousVenue(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        adjournment.setNextHearingVenue(SAME_VENUE);
        adjournment.setNextHearingVenueSelected(list);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);
        assertThat(templateBody.getAdjournCaseTemplateBody().getNextHearingVenue()).isEqualTo(GAP_VENUE_NAME);
    }

    @ParameterizedTest
    @CsvSource(value = {"FACE_TO_FACE,face to face hearing"})
    void givenCaseWithSelectedVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeTheNewVenue(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getVenueDetailsMap()).thenReturn(venueDetailsMap);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);
        assertThat(templateBody.getAdjournCaseTemplateBody().getNextHearingVenue()).isEqualTo(GAP_VENUE_NAME);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers"
    })
    void givenCaseWithSelectedVenueSetForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        DynamicListItem item = new DynamicListItem("someVenueId", "");
        DynamicList list = new DynamicList(item, List.of());

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        final PreSubmitCallbackResponse<SscsCaseData> response =
            service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String error = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false).getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("adjournCaseNextHearingVenueSelected field should not be set");
        assertThat(response.getData().getAdjournment().getPreviewDocument()).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {"FACE_TO_FACE,face to face hearing"})
    void givenCaseWithSelectedVenueSetIncorrectlyForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(listItem, List.of(listItem));

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        checkDocumentIsNotCreatedAndReturnsError("Unable to load venue details for id:someUnknownVenueId");
    }

    @ParameterizedTest
    @CsvSource(value = {"FACE_TO_FACE,face to face hearing"})
    void givenCaseWithSelectedVenueMissingListItemForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(null, List.of(listItem));

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        checkDocumentIsNotCreatedAndReturnsError("A next hearing venue of somewhere else has been specified but no venue has been selected");
    }

    @ParameterizedTest
    @CsvSource(value = {"FACE_TO_FACE,face to face hearing"})
    void givenCaseWithSelectedVenueMissingListItemCodeForFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(new DynamicListItem(null, ""), List.of(listItem));

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        checkDocumentIsNotCreatedAndReturnsError("A next hearing venue of somewhere else has been specified but no venue has been selected");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers"
    })
    void givenCaseWithSelectedVenueSetIncorrectlyForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(AdjournCaseTypeOfHearing nextHearingType) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);

        adjournment.setTypeOfNextHearing(nextHearingType);

        DynamicListItem listItem = new DynamicListItem("someUnknownVenueId", "");
        DynamicList list = new DynamicList(listItem, List.of(listItem));

        adjournment.setNextHearingVenue(SOMEWHERE_ELSE);
        adjournment.setNextHearingVenueSelected(list);

        checkDocumentIsNotCreatedAndReturnsError("adjournCaseNextHearingVenueSelected field should not be set");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void scottishRpcWillShowAScottishImage(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        verifyTemplateBody(NoticeIssuedTemplateBody.SCOTTISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppended(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

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
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenDateIssuedParameterIsTrue_thenShowIssuedDateOnDocument(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setTypeOfNextHearing(nextHearingType);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);

        assertThat(payload.getDateIssued()).isEqualTo(LocalDate.now());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenGeneratedDateIsAlreadySetForGeneratedFlow_thenDoSetNewGeneratedDate(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setGeneratedDate(LOCAL_DATE);
        adjournment.setTypeOfNextHearing(nextHearingType);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);

        assertThat(payload.getGeneratedDate()).hasToString(LocalDate.now().toString());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "TELEPHONE,telephone hearing",
        "VIDEO,video hearing",
        "PAPER,decision on the papers",
        "FACE_TO_FACE,face to face hearing"
    })
    void givenGeneratedDateIsAlreadySetNonGeneratedFlow_thenDoSetNewGeneratedDate(AdjournCaseTypeOfHearing nextHearingType, String nextHearingTypeText) {
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(JUDGE_FULL_NAME);
        when(venueDataLoader.getGapVenueName(any(), any())).thenReturn(GAP_VENUE_NAME);
        when(generateFile.assemble(any())).thenReturn(URL);

        adjournment.setGenerateNotice(NO);
        adjournment.setGeneratedDate(LOCAL_DATE);
        adjournment.setTypeOfNextHearing(nextHearingType);

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, APPELLANT_FULL_NAME, nextHearingTypeText, true);

        assertThat(payload.getGeneratedDate()).hasToString(LocalDate.now().toString());
    }

}
