package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.pip;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.IssueFinalDecisionAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeQuestionService;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;

@RunWith(JUnitParamsRunner.class)
public class PipIssueFinalDecisionAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueFinalDecisionAboutToSubmitHandler handler;

    private PipDecisionNoticeOutcomeService pipDecisionNoticeOutcomeService;

    private DecisionNoticeService decisionNoticeService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    @Mock
    private ListAssistHearingMessageHelper hearingMessageHelper;

    private SscsCaseData sscsCaseData;

    private SscsDocument document;

    protected static Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        pipDecisionNoticeOutcomeService = new PipDecisionNoticeOutcomeService(new PipDecisionNoticeQuestionService());

        decisionNoticeService = new DecisionNoticeService(new ArrayList<>(), Arrays.asList(pipDecisionNoticeOutcomeService), new ArrayList<>());

        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, validator,
                hearingMessageHelper);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(DRAFT_DECISION_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));


        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .state(State.HEARING)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .sscsDocument(documentList)
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionGenerateNotice("")
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
                .writeFinalDecisionDetailsOfDecision("")
            .build())
            .pipSscsCaseData(SscsPipCaseData.builder()
                    .pipWriteFinalDecisionDailyLivingQuestion("")
                    .pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("")
                    .pipWriteFinalDecisionMobilityQuestion("")
                    .pipWriteFinalDecisionComparedToDwpMobilityQuestion("")
                    .pipWriteFinalDecisionDailyLivingActivitiesQuestion(new ArrayList<>())
                    .pipWriteFinalDecisionMobilityActivitiesQuestion(new ArrayList<>())
                    .pipWriteFinalDecisionPreparingFoodQuestion("")
                    .pipWriteFinalDecisionTakingNutritionQuestion("")
                    .pipWriteFinalDecisionManagingTherapyQuestion("")
                    .pipWriteFinalDecisionWashAndBatheQuestion("")
                    .pipWriteFinalDecisionManagingToiletNeedsQuestion("")
                    .pipWriteFinalDecisionDressingAndUndressingQuestion("")
                    .pipWriteFinalDecisionCommunicatingQuestion("")
                    .pipWriteFinalDecisionReadingUnderstandingQuestion("")
                    .pipWriteFinalDecisionEngagingWithOthersQuestion("")
                    .pipWriteFinalDecisionBudgetingDecisionsQuestion("")
                    .pipWriteFinalDecisionPlanningAndFollowingQuestion("")
                    .pipWriteFinalDecisionMovingAroundQuestion("")
                    .build())
            .wcaAppeal(null)
            .sscsEsaCaseData(SscsEsaCaseData.builder().showRegulation29Page(YesNo.YES)
                .showSchedule3ActivitiesPage(YesNo.YES).doesRegulation29Apply(YesNo.YES)
                .doesRegulation35Apply(YesNo.YES).build())
            .dwpReassessTheAward("")
            .showFinalDecisionNoticeSummaryOfOutcomePage(YesNo.YES)
            .supportGroupOnlyAppeal("")
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                    .hearingRoute(HearingRoute.LIST_ASSIST)
                    .build())

            .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonIssueFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnIssueFinalDecisionEventForYesYesFlowWhenComparedToDwpQuestionComparedRatesAreNull_ThenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(null);

        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);

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
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionCommunicatingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMovingAroundQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getShowRegulation29Page());
        assertNotNull(sscsCaseData.getSscsEsaCaseData().getShowSchedule3ActivitiesPage());
        assertNotNull(sscsCaseData.getShowFinalDecisionNoticeSummaryOfOutcomePage());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDetailsOfDecision());
    }

    @Test
    public void givenAnIssueFinalDecisionEventForNoYesFlowWhenComparedToDwpQuestionComparedRatesAreNull_ThenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("no");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(null);

        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);

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
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionCommunicatingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMovingAroundQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
    }

    @Test
    public void givenAnIssueFinalDecisionEventForNoYesFlowWhenComparedToDwpQuestionComparedRatesAreNullButApprovalStatusSet_ThenDoNotDisplayAnError() {
        ReflectionTestUtils.setField(handler, "scheduleListingEnabled", false);
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("no");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null), eq(null));
        verifyNoInteractions(hearingMessageHelper);
        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());

        assertEquals("decisionInFavourOfAppellant", response.getData().getOutcome());

        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionCommunicatingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMovingAroundQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAnythingElse());
    }

    @Test
    public void givenAnIssueFinalDecisionEventForYesNoFlowWhenComparedToDwpQuestionComparedRatesAreNotNullButApprovalNotSet_ThenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(null);

        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("no");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);

        verifyNoInteractions(footerService);
        assertNull(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());
        assertEquals(1, (int) response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).count());

        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionCommunicatingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNotNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMovingAroundQuestion());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNotNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
    }

    @Test
    public void givenAnIssueFinalDecisionEventForYesNoFlowWhenComparedToDwpQuestionComparedRatesAreNullButApprovalStatusSet_ThenDoNotDisplayAnError() {
        ReflectionTestUtils.setField(handler, "scheduleListingEnabled", true);
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("no");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null), eq(null));
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()));
        verifyNoMoreInteractions(hearingMessageHelper);
        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());

        assertEquals("decisionInFavourOfAppellant", response.getData().getOutcome());

        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionCommunicatingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMovingAroundQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
    }


    @Test
    @Parameters({
        "higher, higher, decisionInFavourOfAppellant",
        "higher, same, decisionInFavourOfAppellant",
        "higher, lower, decisionInFavourOfAppellant",
        "same, higher, decisionInFavourOfAppellant",
        "same, same, decisionUpheld",
        "same, lower, decisionUpheld",
        "lower, higher, decisionInFavourOfAppellant",
        "lower, same, decisionUpheld",
        "lower, lower, decisionUpheld"})
    public void givenAnIssueFinalDecisionEvent_thenCreateDecisionWithFooterAndClearTransientFields(String comparedRateDailyLiving, String comparedRateMobility, String expectedOutcome) {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving);
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null), eq(null));
        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());

        assertEquals(expectedOutcome, response.getData().getOutcome());

        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionCommunicatingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNull(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMovingAroundQuestion());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAnythingElse());
        assertNull(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDetailsOfDecision());
    }

    @Test
    public void givenIssueFinalDecisionEventWithWelshAppeal_thenTranslationIsRequired() {
        sscsCaseData.setLanguagePreferenceWelsh(YesNo.YES.getValue());
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("same");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("same");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null), eq(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED));

        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getData().getTranslationWorkOutstanding(), is(YesNo.YES.getValue()));
        assertThat(response.getData().getInterlocReviewState(), is(InterlocReviewState.WELSH_TRANSLATION.getId()));
    }

    @Test
    public void givenAnIssueFinalDecisionEventAndNoDraftDecisionOnCase_thenDisplayAnError() {
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(null);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no Preview Draft Decision Notice on the case so decision cannot be issued", error);
    }

    @Test
    public void givenANonPdfDecisionNotice_thenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice("yes");
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
}
