package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import junitparams.converters.Nullable;
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
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

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
    private DecisionNoticeQuestionService decisionNoticeQuestionService;


    @Before
    public void setUp() throws IOException {
        initMocks(this);
        this.decisionNoticeOutcomeService = new DecisionNoticeOutcomeService();
        this.decisionNoticeQuestionService = new DecisionNoticeQuestionService();
        handler = new WriteFinalDecisionMidEventHandler(generateFile, idamClient, decisionNoticeOutcomeService,
            decisionNoticeQuestionService, TEMPLATE_ID);

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
    @Parameters({"null", ""})
    public void givenAnFinalDecisionDateIsEmpty_thenIgnoreEndDate(@Nullable String endDate) {
        sscsCaseData.setWriteFinalDecisionDateOfDecision(endDate);

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
    public void willSetPreviewFile_whenAppealAllowedAtFiniteTimeEnhancedRate() {

        sscsCaseData.setWriteFinalDecisionStartDate("2018-10-10");
        sscsCaseData.setWriteFinalDecisionEndDate("2018-11-10");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("enhancedRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(Arrays.asList("movingAround",
            "planningAndFollowing"));
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12f");
        sscsCaseData.setPipWriteFinalDecisionPlanningAndFollowingQuestion("planningAndFollowing11f");
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

        DirectionOrDecisionIssuedTemplateBody body = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10", true);
        assertEquals("2018-10-10",body.getStartDate());
        assertEquals("2018-11-10",body.getEndDate());
        assertEquals(false, body.isIndefinite());
        assertEquals(true, body.isMobilityIsEntited());
        assertEquals(true, body.isMobilityIsSeverelyLimited());
        assertEquals("enhanced rate", body.getMobilityAwardRate());
        Assert.assertNotNull(body.getMobilityDescriptors());
        assertEquals(2, body.getMobilityDescriptors().size());
        Assert.assertNotNull(body.getMobilityDescriptors().get(0));
        assertEquals(12, body.getMobilityDescriptors().get(0).getActivityAnswerPoints());
        assertEquals("f", body.getMobilityDescriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, either aided or unaided,  stand; or (ii) move more than 1 metre.", body.getMobilityDescriptors().get(0).getActivityAnswerValue());
        assertEquals("Moving around", body.getMobilityDescriptors().get(0).getActivityQuestionValue());
        assertEquals("12", body.getMobilityDescriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(body.getMobilityDescriptors().get(1));
        assertEquals(12, body.getMobilityDescriptors().get(1).getActivityAnswerPoints());
        assertEquals("f", body.getMobilityDescriptors().get(1).getActivityAnswerLetter());
        assertEquals("Cannot follow the route of a familiar journey without another person, an assistance dog or an orientation aid.", body.getMobilityDescriptors().get(1).getActivityAnswerValue());
        assertEquals("Planning and following journeys", body.getMobilityDescriptors().get(1).getActivityQuestionValue());
        assertEquals("11", body.getMobilityDescriptors().get(1).getActivityQuestionNumber());
        Assert.assertNotNull(body.getMobilityNumberOfPoints());
        assertEquals(24, body.getMobilityNumberOfPoints().intValue());
        assertEquals(false, body.isDailyLivingIsEntited());
        assertEquals(false, body.isDailyLivingIsSeverelyLimited());
        assertNull(body.getDailyLivingAwardRate());
        assertNull(body.getDailyLivingDescriptors());
    }

    @Test
    public void willSetPreviewFile_whenAppealAllowedAtFiniteTimeStandardRate() {

        sscsCaseData.setWriteFinalDecisionStartDate("2018-10-10");
        sscsCaseData.setWriteFinalDecisionEndDate("2018-11-10");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(Arrays.asList("movingAround"));
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");
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

        DirectionOrDecisionIssuedTemplateBody body = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10", true);
        assertEquals("2018-10-10",body.getStartDate());
        assertEquals("2018-11-10",body.getEndDate());
        assertEquals(false, body.isIndefinite());
        assertEquals(true, body.isMobilityIsEntited());
        assertEquals(false, body.isMobilityIsSeverelyLimited());
        assertEquals("standard rate", body.getMobilityAwardRate());
        Assert.assertNotNull(body.getMobilityDescriptors());
        assertEquals(1, body.getMobilityDescriptors().size());
        Assert.assertNotNull(body.getMobilityDescriptors().get(0));
        assertEquals(10, body.getMobilityDescriptors().get(0).getActivityAnswerPoints());
        assertEquals("d", body.getMobilityDescriptors().get(0).getActivityAnswerLetter());
        assertEquals("Can stand and then move using an aid or appliance more than 20 metres but no more than 50 metres.", body.getMobilityDescriptors().get(0).getActivityAnswerValue());
        assertEquals("Moving around", body.getMobilityDescriptors().get(0).getActivityQuestionValue());
        assertEquals("12", body.getMobilityDescriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(body.getMobilityNumberOfPoints());
        assertEquals(10, body.getMobilityNumberOfPoints().intValue());
        assertEquals(false, body.isDailyLivingIsEntited());
        assertEquals(false, body.isDailyLivingIsSeverelyLimited());
        assertNull(body.getDailyLivingAwardRate());
        assertNull(body.getDailyLivingDescriptors());
    }


    @Test
    public void willSetPreviewFile_whenAppealAllowedAtIndefiniteEnhancedRate() {

        sscsCaseData.setWriteFinalDecisionStartDate("2018-10-10");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("enhancedRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(Arrays.asList("movingAround",
            "planningAndFollowing"));
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12f");
        sscsCaseData.setPipWriteFinalDecisionPlanningAndFollowingQuestion("planningAndFollowing11f");
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

        DirectionOrDecisionIssuedTemplateBody body = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10", true);
        assertEquals("2018-10-10",body.getStartDate());
        assertNull(body.getEndDate());
        assertEquals(true, body.isIndefinite());
        assertEquals(true, body.isMobilityIsEntited());
        assertEquals(true, body.isMobilityIsSeverelyLimited());
        assertEquals("enhanced rate", body.getMobilityAwardRate());
        Assert.assertNotNull(body.getMobilityDescriptors());
        assertEquals(2, body.getMobilityDescriptors().size());
        Assert.assertNotNull(body.getMobilityDescriptors().get(0));
        assertEquals(12, body.getMobilityDescriptors().get(0).getActivityAnswerPoints());
        assertEquals("f", body.getMobilityDescriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, either aided or unaided,  stand; or (ii) move more than 1 metre.", body.getMobilityDescriptors().get(0).getActivityAnswerValue());
        assertEquals("Moving around", body.getMobilityDescriptors().get(0).getActivityQuestionValue());
        assertEquals("12", body.getMobilityDescriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(body.getMobilityDescriptors().get(1));
        assertEquals(12, body.getMobilityDescriptors().get(1).getActivityAnswerPoints());
        assertEquals("f", body.getMobilityDescriptors().get(1).getActivityAnswerLetter());
        assertEquals("Cannot follow the route of a familiar journey without another person, an assistance dog or an orientation aid.", body.getMobilityDescriptors().get(1).getActivityAnswerValue());
        assertEquals("Planning and following journeys", body.getMobilityDescriptors().get(1).getActivityQuestionValue());
        assertEquals("11", body.getMobilityDescriptors().get(1).getActivityQuestionNumber());
        Assert.assertNotNull(body.getMobilityNumberOfPoints());
        assertEquals(24, body.getMobilityNumberOfPoints().intValue());
        assertEquals(false, body.isDailyLivingIsEntited());
        assertEquals(false, body.isDailyLivingIsSeverelyLimited());
        assertNull(body.getDailyLivingAwardRate());
        assertNull(body.getDailyLivingDescriptors());
    }

    @Test
    public void willSetPreviewFile_whenAppealAllowedAtIndefiniteStandardRate() {

        sscsCaseData.setWriteFinalDecisionStartDate("2018-10-10");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(Arrays.asList("movingAround"));
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");
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

        DirectionOrDecisionIssuedTemplateBody body = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10", true);
        assertEquals("2018-10-10",body.getStartDate());
        assertNull(body.getEndDate());
        assertEquals(true, body.isIndefinite());
        assertEquals(true, body.isMobilityIsEntited());
        assertEquals(false, body.isMobilityIsSeverelyLimited());
        assertEquals("standard rate", body.getMobilityAwardRate());
        Assert.assertNotNull(body.getMobilityDescriptors());
        assertEquals(1, body.getMobilityDescriptors().size());
        Assert.assertNotNull(body.getMobilityDescriptors().get(0));
        assertEquals(10, body.getMobilityDescriptors().get(0).getActivityAnswerPoints());
        assertEquals("d", body.getMobilityDescriptors().get(0).getActivityAnswerLetter());
        assertEquals("Can stand and then move using an aid or appliance more than 20 metres but no more than 50 metres.", body.getMobilityDescriptors().get(0).getActivityAnswerValue());
        assertEquals("Moving around", body.getMobilityDescriptors().get(0).getActivityQuestionValue());
        assertEquals("12", body.getMobilityDescriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(body.getMobilityNumberOfPoints());
        assertEquals(10, body.getMobilityNumberOfPoints().intValue());
        assertEquals(false, body.isDailyLivingIsEntited());
        assertEquals(false, body.isDailyLivingIsSeverelyLimited());
        assertNull(body.getDailyLivingAwardRate());
        assertNull(body.getDailyLivingDescriptors());
    }

    @Test
    public void willSetPreviewFile_whenAppealRefused() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("lower");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("lower");
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

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine date of decision", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenSignedInJudgeNameNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        when(userDetails.getFullName()).thenReturn(null);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user name", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenSignedInJudgeUserDetailsNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");
        when(idamClient.getUserDetails("Bearer token")).thenReturn(null);

        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to obtain signed in user details", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenComparedToDwpMobilityQuestionNotSet_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");


        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenComparedToDwpDailyLivingSetIncorrectly_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("someValue");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");


        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenComparedToDwpMobilityQuestionSetIncorrectly_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("someValue");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");


        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build()));


        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenGenerateNoticeSetToNo_willNotSetPreviewFile() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("No");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenGenerateNoticeNotSet_willNotSetPreviewFile() {

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
    }

    @Test
    public void givenCaseWithMultipleHearingsWithVenues_thenCorrectlySetHeldAtUsingTheFirstHearingInList() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("venue 2 name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10", true);

        assertEquals("venue 2 name", payload.getHeldAt());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenueName_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoVenue_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayAnErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = null;

        sscsCaseData.setHearings(Arrays.asList(hearing2, hearing1));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDetails_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("venue 1 name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithEmptyHearingsList_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        List<Hearing> hearings = new ArrayList<>();
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithNullHearingsList_thenDisplayAnErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithHearingDates_thenCorrectlySetTheHeldOnUsingTheFirstHearingInList() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-02").venue(Venue.builder().name("Venue Name").build()).build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertEquals(DocumentLink.builder()
            .documentFilename(String.format("Draft Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
            .documentBinaryUrl(URL + "/binary")
            .documentUrl(URL)
            .build(), response.getData().getWriteFinalDecisionPreviewDocument());

        DirectionOrDecisionIssuedTemplateBody payload = verifyTemplateBody(DirectionOrDecisionIssuedTemplateBody.ENGLISH_IMAGE, "Appellant Lastname", "2018-10-10", true);

        assertEquals("2019-01-02", payload.getHeldOn().toString());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstInListWithNoHearingDate_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01").venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = Hearing.builder().value(HearingDetails.builder()
            .venue(Venue.builder().name("Venue Name").build())
            .build()).build();

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithMultipleHearingsWithFirstHearingInListNull_thenDisplayTwoErrorsAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        Hearing hearing1 = Hearing.builder().value(HearingDetails.builder()
            .hearingDate("2019-01-01")
            .venue(Venue.builder().name("Venue Name").build()).build()).build();

        Hearing hearing2 = null;

        List<Hearing> hearings = Arrays.asList(hearing2, hearing1);
        sscsCaseData.setHearings(hearings);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

    }

    @Test
    public void givenCaseWithNullHearingsList_thenDisplayErrorAndDoNotGenerateDocument() {

        sscsCaseData.setWriteFinalDecisionGenerateNotice("Yes");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        sscsCaseData.setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");
        sscsCaseData.setWriteFinalDecisionDateOfDecision("2018-10-10");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Unable to determine hearing date or venue", error);
        assertNull(response.getData().getWriteFinalDecisionPreviewDocument());

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
