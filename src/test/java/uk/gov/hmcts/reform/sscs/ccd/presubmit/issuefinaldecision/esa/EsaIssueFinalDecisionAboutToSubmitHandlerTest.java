package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.esa;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.validation.Validation;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.IssueFinalDecisionAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.service.*;

@RunWith(JUnitParamsRunner.class)
public class EsaIssueFinalDecisionAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    
    private IssueFinalDecisionAboutToSubmitHandler handler;

    private EsaDecisionNoticeOutcomeService esaecisionNoticeOutcomeService;

    private DecisionNoticeService decisionNoticeService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private ListAssistHearingMessageHelper hearingMessageHelper;

    @Mock
    private VenueDataLoader venueDataLoader;

    private SscsCaseData sscsCaseData;

    private DocumentLink documentLink;

    protected static Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        esaecisionNoticeOutcomeService = new EsaDecisionNoticeOutcomeService(new EsaDecisionNoticeQuestionService());

        decisionNoticeService = new DecisionNoticeService(new ArrayList<>(), Arrays.asList(esaecisionNoticeOutcomeService), new ArrayList<>());

        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, userDetailsService,
                validator, hearingMessageHelper, venueDataLoader, false);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetails));

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(DRAFT_DECISION_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));
        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .state(State.HEARING)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("ESA").build()).build())
            .sscsDocument(documentList)
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionTypeOfHearing("")
                .writeFinalDecisionPresentingOfficerAttendedQuestion("")
                .writeFinalDecisionAppellantAttendedQuestion("")
                .writeFinalDecisionDisabilityQualifiedPanelMemberName("")
                .writeFinalDecisionMedicallyQualifiedPanelMemberName("")
                .writeFinalDecisionStartDate("")
                .writeFinalDecisionEndDateType("")
                .writeFinalDecisionEndDate("")
                .writeFinalDecisionDateOfDecision("")
                .writeFinalDecisionReasons(Arrays.asList(new CollectionItem(null, "")))
                .writeFinalDecisionPageSectionReference("")
                .writeFinalDecisionAnythingElse("something else")
                .writeFinalDecisionPreviewDocument(DocumentLink.builder().build())
                .build())
            .dwpReassessTheAward("")
            .sscsEsaCaseData(SscsEsaCaseData.builder()
                .esaWriteFinalDecisionSchedule3ActivitiesQuestion(Arrays.asList())
                .esaWriteFinalDecisionSchedule3ActivitiesApply("")
                .esaWriteFinalDecisionAppropriatenessOfBehaviourQuestion("")
                .esaWriteFinalDecisionAwarenessOfHazardsQuestion("")
                .esaWriteFinalDecisionCommunicationQuestion("")
                .esaWriteFinalDecisionConsciousnessQuestion("")
                .esaWriteFinalDecisionCopingWithChangeQuestion("")
                .esaWriteFinalDecisionGettingAboutQuestion("")
                .esaWriteFinalDecisionLearningTasksQuestion("")
                .esaWriteFinalDecisionLossOfControlQuestion("")
                .esaWriteFinalDecisionMakingSelfUnderstoodQuestion("")
                .esaWriteFinalDecisionManualDexterityQuestion("")
                .esaWriteFinalDecisionMentalAssessmentQuestion(Arrays.asList())
                .esaWriteFinalDecisionMobilisingUnaidedQuestion("")
                .esaWriteFinalDecisionNavigationQuestion("")
                .esaWriteFinalDecisionPersonalActionQuestion("")
                .esaWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList())
                .esaWriteFinalDecisionPickingUpQuestion("")
                .esaWriteFinalDecisionReachingQuestion("")
                .esaWriteFinalDecisionSocialEngagementQuestion("")
                .esaWriteFinalDecisionStandingAndSittingQuestion("")
                .showRegulation29Page(YES)
                .showSchedule3ActivitiesPage(YES)
                .doesRegulation35Apply(YES)
                .doesRegulation29Apply(YES)
                .build())
            .showFinalDecisionNoticeSummaryOfOutcomePage(YES)
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                    .hearingRoute(HearingRoute.LIST_ASSIST)
                    .build())
            .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(State.HEARING);

        String filename = String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")));
        documentLink = DocumentLink.builder()
            .documentUrl("bla.com")
            .documentFilename(filename)
            .build();
    }

    @Test
    public void givenANonIssueFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnIssueFinalDecisionEventRemoveDraftDecisionNotice() {
        SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(documentLink);
        sscsFinalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsFinalDecisionCaseData.setWriteFinalDecisionGenerateNotice(NO);

        SscsDocument document1 = buildSscsDocumentWithDocumentType(DRAFT_DECISION_NOTICE.getValue());
        SscsDocument document2 = buildSscsDocumentWithDocumentType(FINAL_DECISION_NOTICE.getValue());

        List<SscsDocument> documentList = new ArrayList<>(List.of(document1, document2));
        callback.getCaseDetails().getCaseData().setSscsDocument(documentList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(1, response.getData().getSscsDocument().size());
    }

    @Test
    public void givenAnIssueFinalDecisionEventWhenDocumentTypeIsNullThenRemoveDraftDecisionNotice() {
        SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(documentLink);
        sscsFinalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        sscsFinalDecisionCaseData.setWriteFinalDecisionGenerateNotice(NO);

        SscsDocument document1 = buildSscsDocumentWithDocumentType(FINAL_DECISION_NOTICE.getValue());
        SscsDocument document3 = buildSscsDocumentWithDocumentType(DRAFT_DECISION_NOTICE.getValue());

        List<SscsDocument> documentList = new ArrayList<>(List.of(document1, document3));
        callback.getCaseDetails().getCaseData().setSscsDocument(documentList);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(1, response.getData().getSscsDocument().size());
    }

    @Test
    public void givenAnIssueFinalDecisionEventForGenerateNoticeFlowWhenAllowedOrRefusedIsNull_ThenDisplayAnError() {
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(documentLink);
        callback.getCaseDetails().getCaseData().setWcaAppeal(YES);
        callback.getCaseDetails().getCaseData().setSupportGroupOnlyAppeal("Yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(null);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);

        verifyNoInteractions(footerService);
        assertNull(response.getData().getDwpState());
        assertEquals(1, (int) response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).count());

        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
        assertNotNull(sscsCaseData.getWcaAppeal());
        assertNotNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionAppropriatenessOfBehaviourQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionAwarenessOfHazardsQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionCommunicationQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionConsciousnessQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionCopingWithChangeQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionGettingAboutQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionLearningTasksQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionLossOfControlQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMakingSelfUnderstoodQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionManualDexterityQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMentalAssessmentQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMobilisingUnaidedQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionNavigationQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPersonalActionQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPickingUpQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionReachingQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesApply());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSchedule3ActivitiesQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionSocialEngagementQuestion());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionStandingAndSittingQuestion());
        assertNotNull(sscsCaseData.getDwpReassessTheAward());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNotNull(sscsCaseData.getWcaAppeal());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getShowRegulation29Page());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getShowSchedule3ActivitiesPage());
        assertNotNull(sscsCaseData.getShowFinalDecisionNoticeSummaryOfOutcomePage());
        assertNotNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getDoesRegulation35Apply());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getDoesRegulation29Apply());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
    }

    @Test
    public void givenAnIssueFinalDecisionEventAndNoDraftDecisionOnCase_thenDisplayAnError() {
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(null);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no Preview Draft Decision Notice on the case so decision cannot be issued", error);
    }

    @Test
    public void givenANonPdfDecisionNotice_thenDisplayAnError() {
        documentLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(documentLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You need to upload PDF documents only", error);
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenAnIssueFinalDecisionEvent_shouldUpdateIssueFinalDecisionDateToToday() {
        ReflectionTestUtils.setField(handler, "isAdjournmentEnabled", true);

        SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(documentLink);
        sscsFinalDecisionCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsFinalDecisionCaseData.setWriteFinalDecisionGenerateNotice(YES);
        sscsFinalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setState(State.VOID_STATE);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getIssueFinalDecisionDate(), is(LocalDate.now()));
    }

    @Test
    public void givenAnIssueFinalDecisionEvent_shouldUpdateStateAndDwpStateWhenStateIsNotReadyToListOrWithFta() {
        SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(documentLink);
        sscsFinalDecisionCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsFinalDecisionCaseData.setWriteFinalDecisionGenerateNotice(YES);
        sscsFinalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setState(State.VOID_STATE);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.DORMANT_APPEAL_STATE));
        assertThat(response.getData().getDwpState(), is(FINAL_DECISION_ISSUED));
    }

    @Test
    public void givenAnIssueFinalDecisionEventWithPostHearingsTrueAndCorrectionNotInProgress_shouldUpdateStateAndDwpStateWhenStateIsNotReadyToListOrWithFta() {
        ReflectionTestUtils.setField(handler, "isPostHearingsEnabled", true);
        SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(documentLink);
        sscsFinalDecisionCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsFinalDecisionCaseData.setWriteFinalDecisionGenerateNotice(YES);
        sscsFinalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setState(State.VOID_STATE);
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn("judge name");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.DORMANT_APPEAL_STATE));
        assertThat(response.getData().getDwpState(), is(FINAL_DECISION_ISSUED));
    }

    @Test
    public void givenAnIssueFinalDecisionEventWithPostHearingsTrueAndCorrectionInProgress_dontUpdateState() {
        ReflectionTestUtils.setField(handler, "isPostHearingsEnabled", true);
        sscsCaseData.getPostHearing().getCorrection().setIsCorrectionFinalDecisionInProgress(YES);
        SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(documentLink);
        sscsFinalDecisionCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsFinalDecisionCaseData.setWriteFinalDecisionGenerateNotice(YES);
        sscsFinalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setState(State.VOID_STATE);
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn(" judge name");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.VOID_STATE));
    }

    @Test
    public void givenAnIssueFinalDecisionEvent_shouldNotUpdateStateAndDwpStateWhenStateIsReadyToList() {
        SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(documentLink);
        sscsFinalDecisionCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsFinalDecisionCaseData.setWriteFinalDecisionGenerateNotice(YES);
        sscsFinalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setState(State.READY_TO_LIST);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.DIRECTION_RESPONDED);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.READY_TO_LIST));
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_RESPONDED));
    }

    @Test
    public void givenAnIssueFinalDecisionEvent_shouldNotUpdateStateAndDwpStateWhenStateIsWithFta() {
        SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(documentLink);
        sscsFinalDecisionCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsFinalDecisionCaseData.setWriteFinalDecisionGenerateNotice(YES);
        sscsFinalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setState(State.WITH_DWP);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.DIRECTION_RESPONDED);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.WITH_DWP));
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_RESPONDED));
    }

    @Test
    public void givenWriteFinalDecisionPostHearingsEnabledAndNoNullIssueFinalDate_shouldNotUpdateFinalCaseData() {
        ReflectionTestUtils.setField(handler, "isPostHearingsEnabled", true);
        SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(documentLink);
        sscsFinalDecisionCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsFinalDecisionCaseData.setWriteFinalDecisionGenerateNotice(YES);
        sscsFinalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setState(State.WITH_DWP);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.DIRECTION_RESPONDED);
        sscsCaseData.getSscsFinalDecisionCaseData().setFinalDecisionIssuedDate(LocalDate.now());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        Assertions.assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getFinalDecisionIdamSurname());
        Assertions.assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getFinalDecisionGeneratedDate());
    }

    @Test
    public void givenWriteFinalDecisionPostHearingsEnabledAndNoIssueFinalDate_shouldUpdateFinalCaseData() {
        ReflectionTestUtils.setField(handler, "isPostHearingsEnabled", true);
        SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(documentLink);
        sscsFinalDecisionCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsFinalDecisionCaseData.setWriteFinalDecisionGenerateNotice(YES);
        sscsFinalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setState(State.WITH_DWP);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.DIRECTION_RESPONDED);
        sscsCaseData.getSscsFinalDecisionCaseData().setFinalDecisionIssuedDate(null);
        when(userDetailsService.buildLoggedInUserName(USER_AUTHORISATION)).thenReturn("judge name");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals(sscsCaseData.getSscsFinalDecisionCaseData().getFinalDecisionIssuedDate(), LocalDate.now());
    }

    private SscsDocument buildSscsDocumentWithDocumentType(String documentType) {
        SscsDocumentDetails sscsDocumentDetails = SscsDocumentDetails.builder().documentType(documentType).build();
        return SscsDocument.builder().value(sscsDocumentDetails).build();
    }
}
