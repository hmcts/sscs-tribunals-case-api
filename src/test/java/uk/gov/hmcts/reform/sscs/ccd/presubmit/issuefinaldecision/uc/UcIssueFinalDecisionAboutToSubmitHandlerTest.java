package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.uc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.IssueFinalDecisionAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.model.PoDetails;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.*;

@RunWith(JUnitParamsRunner.class)
public class UcIssueFinalDecisionAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueFinalDecisionAboutToSubmitHandler handler;

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

    protected static Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        UcDecisionNoticeOutcomeService ucDecisionNoticeOutcomeService = new UcDecisionNoticeOutcomeService(new UcDecisionNoticeQuestionService());

        decisionNoticeService = new DecisionNoticeService(new ArrayList<>(), List.of(ucDecisionNoticeOutcomeService), new ArrayList<>());

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
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build())
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
            .sscsUcCaseData(SscsUcCaseData.builder()
                .ucWriteFinalDecisionSchedule7ActivitiesQuestion(Arrays.asList())
                .ucWriteFinalDecisionSchedule7ActivitiesApply("")
                .ucWriteFinalDecisionAppropriatenessOfBehaviourQuestion("")
                .ucWriteFinalDecisionAwarenessOfHazardsQuestion("")
                .ucWriteFinalDecisionCommunicationQuestion("")
                .ucWriteFinalDecisionConsciousnessQuestion("")
                .ucWriteFinalDecisionCopingWithChangeQuestion("")
                .ucWriteFinalDecisionGettingAboutQuestion("")
                .ucWriteFinalDecisionLearningTasksQuestion("")
                .ucWriteFinalDecisionLossOfControlQuestion("")
                .ucWriteFinalDecisionMakingSelfUnderstoodQuestion("")
                .ucWriteFinalDecisionManualDexterityQuestion("")
                .ucWriteFinalDecisionMentalAssessmentQuestion(Arrays.asList())
                .ucWriteFinalDecisionMobilisingUnaidedQuestion("")
                .ucWriteFinalDecisionNavigationQuestion("")
                .ucWriteFinalDecisionPersonalActionQuestion("")
                .ucWriteFinalDecisionPhysicalDisabilitiesQuestion(Arrays.asList())
                .ucWriteFinalDecisionPickingUpQuestion("")
                .ucWriteFinalDecisionReachingQuestion("")
                .ucWriteFinalDecisionSocialEngagementQuestion("")
                .ucWriteFinalDecisionStandingAndSittingQuestion("")
                .showSchedule8Paragraph4Page(YES)
                .showSchedule7ActivitiesPage(YES)
                .doesSchedule9Paragraph4Apply(YES)
                .doesSchedule8Paragraph4Apply(YES)
                .build())
            .showFinalDecisionNoticeSummaryOfOutcomePage(YES)
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                    .hearingRoute(HearingRoute.LIST_ASSIST)
                    .build())
            .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(State.HEARING);
    }

    @Test
    public void givenANonIssueFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnIssueFinalDecisionEventRemoveDraftDecisionNotice() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(docLink);
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
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(docLink);
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
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
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
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionAppropriatenessOfBehaviourQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionAwarenessOfHazardsQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionCommunicationQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionConsciousnessQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionCopingWithChangeQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionGettingAboutQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionLearningTasksQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionLossOfControlQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMakingSelfUnderstoodQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionManualDexterityQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMentalAssessmentQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPhysicalDisabilitiesQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMobilisingUnaidedQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionNavigationQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPersonalActionQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPickingUpQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionReachingQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSocialEngagementQuestion());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionStandingAndSittingQuestion());
        assertNotNull(sscsCaseData.getDwpReassessTheAward());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNotNull(sscsCaseData.getWcaAppeal());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getShowSchedule8Paragraph4Page());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getShowSchedule7ActivitiesPage());
        assertNotNull(sscsCaseData.getShowFinalDecisionNoticeSummaryOfOutcomePage());
        assertNotNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getDoesSchedule9Paragraph4Apply());
        assertNotNull(sscsCaseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply());
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
        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
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
    public void givenAnIssueFinalDecisionEventIfHearingsIsNull_ThenDoNotSendHearingCancellationRequest() {
        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, userDetailsService,
                validator, hearingMessageHelper, venueDataLoader, true);

        DocumentLink docLink = DocumentLink.builder()
                .documentUrl("bla.com")
                .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setHearings(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        verify(hearingMessageHelper, times(0)).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()), eq(CancellationReason.OTHER));
    }

    @Test
    public void givenAnIssueFinalDecisionEventIfHearingsIsInThePastOnly_ThenDoNotSendHearingCancellationRequest() {
        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, userDetailsService,
                validator, hearingMessageHelper, venueDataLoader, true);

        DocumentLink docLink = DocumentLink.builder()
                .documentUrl("bla.com")
                .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        HearingDetails hearingDetails1 = HearingDetails.builder()
                .hearingDate(LocalDate.now().minusDays(10).toString())
                .start(LocalDateTime.now().minusDays(10))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        Hearing hearing1 = Hearing.builder().value(hearingDetails1).build();

        HearingDetails hearingDetails2 = HearingDetails.builder()
                .hearingDate(LocalDate.now().minusDays(5).toString())
                .start(LocalDateTime.now().minusDays(5))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        Hearing hearing2 = Hearing.builder().value(hearingDetails2).build();

        callback.getCaseDetails().getCaseData().setHearings(List.of(hearing1, hearing2));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        verify(hearingMessageHelper, times(0)).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()), eq(CancellationReason.OTHER));
    }

    @Test
    public void givenAnIssueFinalDecisionEventIfHearingsIsInThePastAndInTheFuture_ThenSendHearingCancellationRequest() {
        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, userDetailsService,
                validator, hearingMessageHelper, venueDataLoader, true);

        DocumentLink docLink = DocumentLink.builder()
                .documentUrl("bla.com")
                .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        HearingDetails hearingDetails1 = HearingDetails.builder()
                .hearingDate(LocalDate.now().minusDays(10).toString())
                .start(LocalDateTime.now().minusDays(10))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        Hearing hearing1 = Hearing.builder().value(hearingDetails1).build();

        HearingDetails hearingDetails2 = HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(5).toString())
                .start(LocalDateTime.now().plusDays(5))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        Hearing hearing2 = Hearing.builder().value(hearingDetails2).build();

        callback.getCaseDetails().getCaseData().setHearings(List.of(hearing1, hearing2));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()), eq(CancellationReason.OTHER));
    }

    @Test
    public void givenAnIssueFinalDecisionEventIfHearingsIsInTheFutureOnly_ThenSendHearingCancellationRequest() {
        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, userDetailsService,
                validator, hearingMessageHelper, venueDataLoader, true);

        DocumentLink docLink = DocumentLink.builder()
                .documentUrl("bla.com")
                .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        HearingDetails hearingDetails = HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(5).toString())
                .start(LocalDateTime.now().plusDays(5))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        Hearing hearing = Hearing.builder().value(hearingDetails).build();

        callback.getCaseDetails().getCaseData().setHearings(List.of(hearing));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()), eq(CancellationReason.OTHER));
    }

    @Test
    public void givenFinalDecisionIsIssued_thenClearPoFields() {
        sscsCaseData.setPoAttendanceConfirmed(YES);
        sscsCaseData.setPresentingOfficersDetails(PoDetails.builder().name(Name.builder().build()).build());
        sscsCaseData.setPresentingOfficersHearingLink("link");

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getPoAttendanceConfirmed()).isEqualTo(NO);
        assertThat(sscsCaseData.getPresentingOfficersDetails()).isEqualTo(PoDetails.builder().build());
        assertThat(sscsCaseData.getPresentingOfficersHearingLink()).isNull();
    }

    private SscsDocument buildSscsDocumentWithDocumentType(String documentType) {
        SscsDocumentDetails sscsDocumentDetails = SscsDocumentDetails.builder().documentType(documentType).build();
        return SscsDocument.builder().value(sscsDocumentDetails).build();
    }
}
