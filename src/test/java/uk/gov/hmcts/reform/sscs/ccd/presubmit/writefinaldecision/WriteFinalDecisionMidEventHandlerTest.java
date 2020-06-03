package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
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
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;

@RunWith(JUnitParamsRunner.class)
public class WriteFinalDecisionMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String TEMPLATE_ID = "nuts.docx";
    private static final String URL = "http://dm-store/documents/123";
    private WriteFinalDecisionMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private GenerateFile generateFile;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private ArgumentCaptor<GenerateFileParams> capture;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        handler = new WriteFinalDecisionMidEventHandler(generateFile, TEMPLATE_ID);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);


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

    @Test
    public void givenANonWriteFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAnEndDateIsBeforeStartDate_thenDisplayAnError() {
        sscsCaseData.setWriteFinalDecisionStartDate("2020-01-01");
        sscsCaseData.setWriteFinalDecisionEndDate("2019-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice end date must be after decision notice start date", error);
    }

    @Test
    public void givenADecisionDateIsInFuture_thenDisplayAnError() {

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        sscsCaseData.setWriteFinalDecisionDateOfDecision(tomorrow.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice date of decision must not be in the future", error);
    }

    @Test
    public void givenADecisionDateIsToday_thenDoNotDisplayAnError() {

        LocalDate today = LocalDate.now();
        sscsCaseData.setWriteFinalDecisionDateOfDecision(today.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenADecisionDateIsInPast_thenDoNotDisplayAnError() {

        LocalDate yesterday = LocalDate.now().plusDays(-1);
        sscsCaseData.setWriteFinalDecisionDateOfDecision(yesterday.toString());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenAnEndDateIsSameAsStartDate_thenDisplayAnError() {
        sscsCaseData.setWriteFinalDecisionStartDate("2020-01-01");
        sscsCaseData.setWriteFinalDecisionEndDate("2020-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Decision notice end date must be after decision notice start date", error);
    }

    @Test
    public void givenAnEndDateIsAfterStartDate_thenDoNotDisplayAnError() {
        sscsCaseData.setWriteFinalDecisionStartDate("2019-01-01");
        sscsCaseData.setWriteFinalDecisionEndDate("2020-01-01");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }

    @Test
    public void willSetPreviewFile() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");
    }

    @Test
    public void givenGenerateNoticeSetToNo_willNotSetPreviewFile() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("No");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenGenerateNoticeNotSet_willNotSetPreviewFile() {

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithVenues_thenCorrectlySetHeldAtUsingTheLastHearingInList() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 2 name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing1, hearing2);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        assertEquals("venue 2 name", payload.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithSecondWithNoVenueName_thenCorrectlyDoNotSetHeldAt() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing1, hearing2);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        Assert.assertNull(payload.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithSecondWithNoVenue_thenCorrectlyDoNotSetHeldAt() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .build()).build();

        List<Hearing> hearings = Arrays.asList(hearing1, hearing2);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        Assert.assertNull(payload.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithSecondWithNoHearingDetails_thenCorrectlyDoNotSetHeldAt() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().build();

        List<Hearing> hearings = Arrays.asList(hearing1, hearing2);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        Assert.assertNull(payload.getHeldAt());

    }

    @Test
    public void givenCaseWithEmptyHearingsList_thenCorrectlyDoNotSetHeldAt() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        Assert.assertNull(payload.getHeldAt());

    }

    @Test
    public void givenCaseWithNullHearingsList_thenCorrectlyDoNotSetHeldAt() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");


        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        Assert.assertNull(payload.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithHearingDates_thenCorrectlySetTheHeldOnUsingTheLastHearingInList() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").build()).build();

        List<Hearing> hearings = Arrays.asList(hearing1, hearing2);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        assertEquals("2019-01-02", payload.getHeldOn().toString());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithSecondWithNoHearingDate_thenCorrectlyDoNotSetSetTheHeldOn() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .build()).build();

        List<Hearing> hearings = Arrays.asList(hearing1, hearing2);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        Assert.assertNull("2019-01-02", payload.getHeldOn());

    }

    @Test
    public void givenCaseWithEmptyHearingsList_thenCorrectlyDoNotSetSetTheHeldOn() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        Assert.assertNull("2019-01-02", payload.getHeldOn());

    }

    @Test
    public void givenCaseWithNullHearingsList_thenCorrectlyDoNotSetSetTheHeldOn() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");


        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        Assert.assertNull("2019-01-02", payload.getHeldOn());

    }

    @Test
    public void givenCaseWithTwoPanelMembers_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        sscsCaseData.setWriteFinalDecisionDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        sscsCaseData.setWriteFinalDecisionMedicallyQualifiedPanelMemberName("Ms Panel Member 2");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        assertEquals("Judge Name Placeholder, Mr Panel Member 1, Ms Panel Member 2", payload.getHeldBefore());

    }

    @Test
    public void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        sscsCaseData.setWriteFinalDecisionDisabilityQualifiedPanelMemberName("Mr Panel Member 1");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        assertEquals("Judge Name Placeholder, Mr Panel Member 1", payload.getHeldBefore());

    }

    @Test
    public void givenCaseWithNoPanelMembers_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname");

        assertEquals("Judge Name Placeholder", payload.getHeldBefore());

    }

    @Test
    public void scottishRpcWillShowAScottishImage() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.SCOTTISH_IMAGE, "Appellant Lastname");
    }

    @Test
    public void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppended() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.getAppeal().getAppellant().setIsAppointee("Yes");
        sscsCaseData.getAppeal().getAppellant().setAppointee(Appointee.builder()
            .name(Name.builder().firstName("APPOINTEE")
                .lastName("SurNamE")
                .build())
            .identity(Identity.builder().build())
            .build());

        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appointee Surname, appointee for Appellant Lastname");
    }

    private DirectionOrDecisionIssuedTemplateBody verifyTemplateBody(String image, String expectedName) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        DirectionOrDecisionIssuedTemplateBody payload = (DirectionOrDecisionIssuedTemplateBody) capture.getValue().getFormPayload();
        assertEquals(image, payload.getImage());
        assertEquals("DECISION NOTICE", payload.getNoticeType());
        assertEquals(expectedName, payload.getAppellantFullName());
        return payload;
    }
}
