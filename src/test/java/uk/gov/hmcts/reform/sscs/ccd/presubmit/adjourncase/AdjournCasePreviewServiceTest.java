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
import junitparams.Parameters;
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


    @NamedParameters("allNextHearingTypeParameters")
    @SuppressWarnings("unused")
    private Object[] allNextHearingTypeParameters() {
        return new Object[] {
            new String[] {"faceToFace", "face to face"},
            new String[] {"telephone", "telephone"},
            new String[] {"video", "video"},
        };
    }

    @NamedParameters("nonFaceToFaceNextHearingTypeParameters")
    @SuppressWarnings("unused")
    private Object[] nonFaceToFaceNextHearingTypeParameters() {
        return new Object[] {
            new String[] {"telephone", "telephone"},
            new String[] {"video", "video"},
        };
    }

    @NamedParameters("faceToFaceNextHearingTypeParameter")
    @SuppressWarnings("unused")
    private Object[] faceToFaceNextHearingTypeParameter() {
        return new Object[] {
            new String[] {"faceToFace", "face to face"},
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
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
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
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
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
    public void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
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
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenSignedInJudgeUserDetailsNotSet_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
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
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getAdjournCasePreviewDocument());

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        AdjournCaseTemplateBody body = payload.getAdjournCaseTemplateBody();
        assertNotNull(body);

        assertEquals("venue 2 name", body.getHeldAt());

    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenDisplayErrorAndDoNotGenerateDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
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
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
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
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
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
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
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
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
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
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
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
            .documentFilename(String.format("Draft Adjournment Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
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
        assertEquals("Venue Name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
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

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstAvailableDate_thenCorrectlyDisplayTheNextHearingDateString(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("the first available date", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        assertNull(templateBody.getAdjournCaseTemplateBody().getNextHearingTime());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithDateToBeFixed_thenCorrectlyDisplayTheNextHearingDateString(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("dateToBeFixed");
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("a date to be fixed", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        assertNull(templateBody.getAdjournCaseTemplateBody().getNextHearingTime());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstAvailableDateAfterWithNoDateOrPeriodIndicator_ThenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
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
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstAvailableDateAfterWithProvideDateAndNoDateSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
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
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstAvailableDateAfterWithProvideDateAndDateProvided_thenCorrectlyDisplayTheNextHearingDateString(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("provideDate");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterDate("2020-01-01");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("the first available date after 01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        assertNull(templateBody.getAdjournCaseTemplateBody().getNextHearingTime());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstAvailableDateAfterWithProvidePeriodAndNoPeriodSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
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
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstAvailableDateAfterWithNeitherProvideDateOrPeriodSpecified_ThenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Date or period indicator not available in case data", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());

    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstAvailableDateAfterWithProvidePeriodAndPeriodProvided_thenCorrectlyDisplayTheNextHearingDateString(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDateAfter");
        sscsCaseData.setAdjournCaseNextHearingDateOrPeriod("providePeriod");
        sscsCaseData.setAdjournCaseNextHearingFirstAvailableDateAfterPeriod("28");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/YYYY"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("the first available date after " + expectedDate, templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        assertNull(templateBody.getAdjournCaseTemplateBody().getNextHearingTime());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstWithSpecifiedDateAndTime_thenCorrectlyDisplayTheNextHearingDateString(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("specificDateAndTime");
        sscsCaseData.setAdjournCaseNextHearingSpecificDate("2020-01-01");
        sscsCaseData.setAdjournCaseNextHearingSpecificTime("am");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        String expectedDate = LocalDate.now().plusDays(28).format(DateTimeFormatter.ofPattern("dd/MM/YYYY"));

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("01/01/2020", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        assertEquals("am", templateBody.getAdjournCaseTemplateBody().getNextHearingTime());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstWithSpecifiedDateAndTimeWithDateMissing_thenDisplayAnErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
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
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstWithSpecifiedDateAndTimeWithTimeMissing_thenDisplayAnErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
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
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstWithSpecifiedTime_thenCorrectlyDisplayTheNextHearingDateString(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("specificTime");
        sscsCaseData.setAdjournCaseNextHearingSpecificTime("am");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("a date to be decided", templateBody.getAdjournCaseTemplateBody().getNextHearingDate());
        assertEquals("am", templateBody.getAdjournCaseTemplateBody().getNextHearingTime());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstWithSpecifiedTimeWithTimeMissing_thenDisplayAnErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("specificTime");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("adjournCaseNextHearingSpecificTime not available in case data", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenCaseWithFirstWithInvalidDateTime_thenDisplayAnErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {

        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
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
    public void givenCaseWithSelectedVenueSetForFaceToFace_thenCorrectlySetTheVenueToBeTheNewVenue(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseNextHearingVenueSelected("someVenueId");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody templateBody = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);
        assertEquals("New Venue Name", templateBody.getAdjournCaseTemplateBody().getNextHearingVenue());
    }

    @Test
    @Parameters(named = "nonFaceToFaceNextHearingTypeParameters")
    public void givenCaseWithSelectedVenueSetForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseNextHearingVenueSelected("someVenueId");

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
        sscsCaseData.setAdjournCaseNextHearingVenueSelected("someUnknownVenueId");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, false);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to load venue details for id:someUnknownVenueId", error);
        assertNull(response.getData().getAdjournCasePreviewDocument());
    }

    @Test
    @Parameters(named = "nonFaceToFaceNextHearingTypeParameters")
    public void givenCaseWithSelectedVenueSetIncorrectlyForNonFaceToFace_thenDisplayErrorAndDoNotDisplayTheDocument(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");
        sscsCaseData.setAdjournCaseNextHearingVenueSelected("someUnknownVenueId");

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
    public void givenGeneratedDateIsAlreadySetForGeneratedFlow_thenDoNotSetNewGeneratedDate(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("yes");
        sscsCaseData.setAdjournCaseGeneratedDate("2018-10-10");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals("2018-10-10", payload.getGeneratedDate().toString());
    }

    @Test
    @Parameters(named = "allNextHearingTypeParameters")
    public void givenGeneratedDateIsAlreadySetNonGeneratedFlow_thenDoNotSetNewGeneratedDate(String nextHearingType, String nextHearingTypeText) {
        sscsCaseData.setAdjournCaseGenerateNotice("no");
        sscsCaseData.setAdjournCaseGeneratedDate("2018-10-10");
        sscsCaseData.setAdjournCaseTypeOfNextHearing(nextHearingType);
        sscsCaseData.setAdjournCaseNextHearingDateType("firstAvailableDate");

        service.preview(callback, DocumentType.DRAFT_ADJOURNMENT_NOTICE, USER_AUTHORISATION, true);

        NoticeIssuedTemplateBody payload = verifyTemplateBody(NoticeIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", nextHearingTypeText, true);

        assertEquals("2018-10-10", payload.getGeneratedDate().toString());
    }

    private NoticeIssuedTemplateBody verifyTemplateBody(String image, String expectedName, String nextHearingType, boolean isDraft) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());

        final boolean hasVenue = HearingType.FACE_TO_FACE.getValue().equals(nextHearingType);

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
