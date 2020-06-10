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
import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
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
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;

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

    @Mock
    private IdamClient idamClient;

    @Mock
    private UserDetails userDetails;

    private ArgumentCaptor<GenerateFileParams> capture;

    private SscsCaseData sscsCaseData;

    private DecisionNoticeOutcomeService decisionNoticeOutcomeService;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        this.decisionNoticeOutcomeService = new DecisionNoticeOutcomeService();
        handler = new WriteFinalDecisionMidEventHandler(generateFile, idamClient, decisionNoticeOutcomeService, TEMPLATE_ID);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer token")).thenReturn(userDetails);

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
    public void willSetPreviewFile_whenAppealAllowed() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10", true);
    }

    @Test
    public void willSetPreviewFile_whenAppealRefused() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("lower");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("lower");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10", false);
    }

    @Test
    public void givenDateOfDecisionNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine date of decision", error);
        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        when(userDetails.getFullName()).thenReturn(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user name", error);
        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenSignedInJudgeUserDetailsNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        when(idamClient.getUserDetails("Bearer token")).thenReturn(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenComparedToDwpMobilityQuestionNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");


        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenComparedToDwpDailyLivingSetIncorrectly_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("someValue");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenComparedToDwpMobilityQuestionSetIncorrectly_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("someValue");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
        Assert.assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
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
    public void givenCaseWithTwoPanelMembers_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        sscsCaseData.setWriteFinalDecisionDisabilityQualifiedPanelMemberName("Mr Panel Member 1");
        sscsCaseData.setWriteFinalDecisionMedicallyQualifiedPanelMemberName("Ms Panel Member 2");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10", true);

        assertEquals("Judge Full Name, Mr Panel Member 1 and Ms Panel Member 2", payload.getHeldBefore());

    }

    @Test
    public void givenCaseWithOnePanelMember_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        sscsCaseData.setWriteFinalDecisionDisabilityQualifiedPanelMemberName("Mr Panel Member 1");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10", true);

        assertEquals("Judge Full Name and Mr Panel Member 1", payload.getHeldBefore());

    }

    @Test
    public void givenCaseWithNoPanelMembers_thenCorrectlySetTheHeldBefore() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10", true);

        assertEquals("Judge Full Name", payload.getHeldBefore());

    }

    @Test
    public void scottishRpcWillShowAScottishImage() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        sscsCaseData.setRegionalProcessingCenter(RegionalProcessingCenter.builder().name("Glasgow").build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));

        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.SCOTTISH_IMAGE, "Appellant Lastname", "2018-10-10", true);
    }

    @Test
    public void givenCaseWithAppointee_thenCorrectlySetTheNoticeNameWithAppellantAndAppointeeAppended() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        sscsCaseData.getAppeal().getAppellant().setIsAppointee("Yes");
        sscsCaseData.getAppeal().getAppellant().setAppointee(Appointee.builder()
            .name(Name.builder().firstName("APPOINTEE")
                .lastName("SurNamE")
                .build())
            .identity(Identity.builder().build())
            .build());

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appointee Surname, appointee for Appellant Lastname", "2018-10-10", true);
    }

    private DirectionOrDecisionIssuedTemplateBody verifyTemplateBody(String image, String expectedName, String dateOfDecision, boolean allowed) {
        verify(generateFile, atLeastOnce()).assemble(capture.capture());
        DirectionOrDecisionIssuedTemplateBody payload = (DirectionOrDecisionIssuedTemplateBody) capture.getValue().getFormPayload();
        assertEquals(image, payload.getImage());
        assertEquals("DRAFT DECISION NOTICE", payload.getNoticeType());
        assertEquals(expectedName, payload.getAppellantFullName());
        assertEquals(dateOfDecision, payload.getDateOfDecision());
        assertEquals(allowed, payload.isAllowed());
        assertEquals(allowed, payload.isSetAside());

        return payload;
    }
}
