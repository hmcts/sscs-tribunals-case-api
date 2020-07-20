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
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
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

    @Mock
    private VenueDataLoader venueDataLoader;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        service = new AdjournCasePreviewService(generateFile, idamClient,
            venueDataLoader, TEMPLATE_ID);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer token")).thenReturn(userDetails);

        Map<String, VenueDetails> venueDetailsMap = new HashMap<>();
        VenueDetails venueDetails = VenueDetails.builder().venName("New Venue Name").build();
        venueDetailsMap.put("someVenueId", venueDetails);

        when(venueDataLoader.getVenueDetailsMap()).thenReturn(venueDetailsMap);

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
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

    }

    private void assertCommonPreviewParams(AdjournCaseTemplateBody body, String endDate, boolean nullReasonsExpected) {

        if (nullReasonsExpected) {
            assertNull(body.getReasonsForDecision());
        } else {
            assertNotNull(body.getReasonsForDecision());
            assertFalse(body.getReasonsForDecision().isEmpty());
            assertEquals("My reasons for decision", body.getReasonsForDecision().get(0));
        }
        assertEquals("Something else.", body.getAnythingElse());
        assertEquals("faceToFace", body.getHearingType());
    }

    @Test
    public void willSetPreviewFileWithNullReasons_WhenReasonsListIsEmpty() {

        final String endDate = "10-10-2020";
        
        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.setAdjournCaseReasons(new ArrayList<>());

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

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
    public void willSetPreviewFileWithNullReasons_WhenReasonsListIsNotEmpty() {

        final String endDate = "10-10-2020";

        setCommonPreviewParams(sscsCaseData, endDate);
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

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
    public void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

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

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        when(idamClient.getUserDetails("Bearer token")).thenReturn(null);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void givenGenerateNoticeNotSet_willNotSetPreviewFile() {

        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");


        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithVenues_thenCorrectlySetHeldAtUsingTheFirstHearingInList() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
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
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);
        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("venue 2 name", body.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

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
    public void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayAnErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
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
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDetails_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
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
    public void givenCaseWithEmptyHearingsList_thenDefaultHearingData() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

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
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertTrue(response.getErrors().isEmpty());
        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

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
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("2019-01-02", body.getHeldOn().toString());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDate_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
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
    public void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayTwoErrorsAndDoNotGenerateDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
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
    public void givenCaseWithThreePanelMembers_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setAdjournCaseDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        sscsCaseData.setAdjournCaseMedicallyQualifiedPanelMemberName("Ms Panel Member 2");
        sscsCaseData.setAdjournCaseOtherPanelMemberName("Other Panel Member");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        assertNotNull(response.getData().getAdjournCasePreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1, Ms Panel Member 2 and Other Panel Member", body.getHeldBefore());

    }

    @Test
    public void givenCaseWithTwoPanelMembers_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");


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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name, Mr Panel Member 1 and Ms Panel Member 2", body.getHeldBefore());

    }

    @Test
    public void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name and Mr Panel Member 1", body.getHeldBefore());

    }

    @Test
    public void givenCaseWithNoPanelMembersWithNullValues_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());

    }

    @Test
    public void givenCaseWithNoPanelMembersWithEmptyValues_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
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

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("Judge Full Name", body.getHeldBefore());
    }

    @Test
    public void givenCaseWithNoSelectedVenueNotSet_thenCorrectlySetTheVenueToBeTheExistingVenue() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);
        assertEquals("Venue Name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @Test
    public void givenCaseWithFirstAvailableDate_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);
        assertEquals("the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        assertNull(templateBody.getAdjournCaseTemplateBody().getNextHearingTime());
    }

    @Test
    public void givenCaseWithDateToBeFixed_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("dateToBeFixed");
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);
        assertEquals("a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        assertNull(templateBody.getAdjournCaseTemplateBody().getNextHearingTime());
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
    public void givenCaseWithFirstAvailableDateAfterWithProvideDateAndDateProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate("2020-01-01");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);
        assertEquals("the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        assertNull(templateBody.getAdjournCaseTemplateBody().getNextHearingTime());
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
    public void givenCaseWithFirstAvailableDateAfterWithProvidePeriodAndPeriodProvided_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("providePeriod");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterPeriod("28");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/YYYY"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);
        assertEquals("the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        assertNull(templateBody.getAdjournCaseTemplateBody().getNextHearingTime());
    }

    @Test
    public void givenCaseWithFirstWithSpecifiedDateAndTime_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("specificDateAndTime");
        sscsCaseData.setAdjournCaseNextHearingSpecificDate("2020-01-01");
        sscsCaseData.setAdjournCaseNextHearingSpecificTime("am");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/YYYY"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);
        assertEquals("01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        assertEquals("am", templateBody.getAdjournCaseTemplateBody().getNextHearingTime());
    }

    @Test
    public void givenCaseWithFirstWithSpecifiedDateAndTimeWithDateMissing_thenDisplayAnErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("specificDateAndTime");
        sscsCaseData.setAdjournCaseNextHearingSpecificTime("am");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("adjournCaseNextHearingSpecificDate not available in case data", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }


    @Test
    public void givenCaseWithFirstWithSpecifiedDateAndTimeWithTimeMissing_thenDisplayAnErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("specificDateAndTime");
        sscsCaseData.setAdjournCaseNextHearingSpecificDate("2020-01-01");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("adjournCaseNextHearingSpecificTime not available in case data", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void givenCaseWithFirstWithSpecifiedTime_thenCorrectlyDisplayTheNextHearingDateString() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("specificTime");
        sscsCaseData.setAdjournCaseNextHearingSpecificTime("am");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);
        assertEquals("a date to be decided", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        assertEquals("am", templateBody.getAdjournCaseTemplateBody().getNextHearingTime());
    }

    @Test
    public void givenCaseWithFirstWithSpecifiedTimeWithTimeMissing_thenDisplayAnErrorAndDoNotDisplayTheDocument() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("specificTime");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("adjournCaseNextHearingSpecificTime not available in case data", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void givenCaseWithFirstWithInvaidDateTime_thenDisplayAnErrorAndDoNotDisplayTheDocument() {

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
    public void givenCaseWithSelectedVenueSet_thenCorrectlySetTheVenueToBeTheNewVenue() {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseNextHearingVenueSelected("someVenueId");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);
        assertEquals("New Venue Name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @Test
    public void givenCaseWithSelectedVenueSetIncorrectly_thenDisplayErrorAndDoNotDisplayTheDocument() {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseNextHearingVenueSelected("someUnknownVenueId");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to load venue details for id:someUnknownVenueId", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    public void scottishRpcWillShowAScottishImage() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        verifyTemplateBody(NoticeIssuedTemplateBody.SCOTTISH_IMAGE, "Appellant Lastname", "face to face", true);
    }

    @Test
    public void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppended() {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
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

        verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appointee Surname, appointee for Appellant Lastname", "face to face", true);
    }

    @Test
    public void givenDateIssuedParameterIsTrue_thenShowIssuedDateOnDocument() {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

        assertEquals(LocalDate.now(), payload.getDateIssued());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetForGeneratedFlow_thenDoNotSetNewGeneratedDate() {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseGeneratedDate("2018-10-10");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

        assertEquals("2018-10-10", payload.getGeneratedDate().toString());
    }

    @Test
    public void givenGeneratedDateIsAlreadySetNonGeneratedFlow_thenDoNotSetNewGeneratedDate() {
        sscsCaseData.setAdjournCaseGenerateNotice("no");
        sscsCaseData.setAdjournCaseGeneratedDate("2018-10-10");
        sscsCaseData.setAdjournCaseTypeOfNextHearing("faceToFace");
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "face to face", true);

        assertEquals("2018-10-10", payload.getGeneratedDate().toString());
    }

    private NoticeIssuedTemplateBody verifyTemplateBody(String image, String expectedName,  String nextHearingType, boolean isDraft) {
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
        assertNotNull(body.getNextHearingVenue());
        assertNotNull(body.getNextHearingDate());
        assertNotNull(body.getNextHearingType());
        assertEquals(nextHearingType, body.getNextHearingType());
        assertNotNull(body.getNextHearingTimeslot());
        assertNotNull(body.getHeldAt());
        assertNotNull(body.getHeldBefore());
        assertNotNull(body.getHeldOn());
        return payload;
    }
}
