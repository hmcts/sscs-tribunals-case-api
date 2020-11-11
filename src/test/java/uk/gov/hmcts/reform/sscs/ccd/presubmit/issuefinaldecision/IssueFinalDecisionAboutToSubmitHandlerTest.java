package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.validation.Validation;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeOutcomeService;

@RunWith(JUnitParamsRunner.class)
public class IssueFinalDecisionAboutToSubmitHandlerTest {

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

    private SscsCaseData sscsCaseData;

    private SscsDocument document;

    protected static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        pipDecisionNoticeOutcomeService = new PipDecisionNoticeOutcomeService();

        decisionNoticeService = new DecisionNoticeService(new ArrayList<>(), Arrays.asList(pipDecisionNoticeOutcomeService));

        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, validator);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(DRAFT_DECISION_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .sscsDocument(documentList)
            .writeFinalDecisionGenerateNotice("")
            .writeFinalDecisionTypeOfHearing("")
            .writeFinalDecisionPresentingOfficerAttendedQuestion("")
            .writeFinalDecisionAppellantAttendedQuestion("")
            .writeFinalDecisionDisabilityQualifiedPanelMemberName("")
            .writeFinalDecisionMedicallyQualifiedPanelMemberName("")
            .pipWriteFinalDecisionDailyLivingQuestion("")
            .pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("")
            .pipWriteFinalDecisionMobilityQuestion("")
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion("")
            .writeFinalDecisionStartDate("")
            .writeFinalDecisionEndDateType("")
            .writeFinalDecisionEndDate("")
            .writeFinalDecisionDateOfDecision("")
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
            .writeFinalDecisionReasons(Arrays.asList(new CollectionItem(null, "")))
            .writeFinalDecisionPageSectionReference("")
            .writeFinalDecisionAnythingElse("something else")
            .writeFinalDecisionPreviewDocument(DocumentLink.builder().build())
            .esaWriteFinalDecisionPhysicalDisabilitiesQuestion(new ArrayList<>())
            .esaWriteFinalDecisionMentalAssessmentQuestion(new ArrayList<>())
            .esaWriteFinalDecisionMobilisingUnaidedQuestion("")
            .esaWriteFinalDecisionStandingAndSittingQuestion("")
            .esaWriteFinalDecisionReachingQuestion("")
            .esaWriteFinalDecisionPickingUpQuestion("")
            .esaWriteFinalDecisionManualDexterityQuestion("")
            .esaWriteFinalDecisionMakingSelfUnderstoodQuestion("")
            .esaWriteFinalDecisionCommunicationQuestion("")
            .esaWriteFinalDecisionNavigationQuestion("")
            .esaWriteFinalDecisionLossOfControlQuestion("")
            .esaWriteFinalDecisionConsciousnessQuestion("")
            .esaWriteFinalDecisionLearningTasksQuestion("")
            .esaWriteFinalDecisionAwarenessOfHazardsQuestion("")
            .esaWriteFinalDecisionPersonalActionQuestion("")
            .esaWriteFinalDecisionCopingWithChangeQuestion("")
            .esaWriteFinalDecisionGettingAboutQuestion("")
            .esaWriteFinalDecisionSocialEngagementQuestion("")
            .esaWriteFinalDecisionAppropriatenessOfBehaviourQuestion("")
            .esaWriteFinalDecisionSchedule3ActivitiesApply("")
            .esaWriteFinalDecisionSchedule3ActivitiesQuestion(new ArrayList<>())
            .showRegulation29Page(YesNo.YES)
            .showSchedule3ActivitiesPage(YesNo.YES)
            .showFinalDecisionNoticeSummaryOfOutcomePage(YesNo.YES)
            .writeFinalDecisionDetailsOfDecision("")
            .wcaAppeal("")
            .supportGroupOnlyAppeal("")
            .doesRegulation29Apply(YesNo.YES)
            .doesRegulation35Apply(YesNo.YES)
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
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionAllowedOrRefused(null);

        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);

        verifyNoInteractions(footerService);
        assertNull(response.getData().getDwpState());
        assertEquals(1, (int) response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).count());

        assertNotNull(sscsCaseData.getWriteFinalDecisionTypeOfHearing());
        assertNotNull(sscsCaseData.getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionAppellantAttendedQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionStartDate());
        assertNotNull(sscsCaseData.getWriteFinalDecisionEndDateType());
        assertNotNull(sscsCaseData.getWriteFinalDecisionEndDate());
        assertNotNull(sscsCaseData.getWriteFinalDecisionDateOfDecision());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionCommunicatingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionMovingAroundQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionReasons());
        assertNotNull(sscsCaseData.getWriteFinalDecisionPageSectionReference());
        assertNotNull(sscsCaseData.getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getWriteFinalDecisionGeneratedDate());
        assertNotNull(sscsCaseData.getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());

        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionMentalAssessmentQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionMobilisingUnaidedQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionStandingAndSittingQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionReachingQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionPickingUpQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionManualDexterityQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionMakingSelfUnderstoodQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionCommunicationQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionNavigationQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionLossOfControlQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionConsciousnessQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionLearningTasksQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionAwarenessOfHazardsQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionPersonalActionQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionCopingWithChangeQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionGettingAboutQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionSocialEngagementQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionAppropriatenessOfBehaviourQuestion());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionSchedule3ActivitiesApply());
        assertNotNull(sscsCaseData.getEsaWriteFinalDecisionSchedule3ActivitiesQuestion());
        assertNotNull(sscsCaseData.getShowRegulation29Page());
        assertNotNull(sscsCaseData.getShowSchedule3ActivitiesPage());
        assertNotNull(sscsCaseData.getShowFinalDecisionNoticeSummaryOfOutcomePage());
        assertNotNull(sscsCaseData.getWriteFinalDecisionDetailsOfDecision());
        assertNotNull(sscsCaseData.getWcaAppeal());
        assertNotNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNotNull(sscsCaseData.getDoesRegulation29Apply());
        assertNotNull(sscsCaseData.getDoesRegulation35Apply());
    }

    @Test
    public void givenAnIssueFinalDecisionEventForNoYesFlowWhenComparedToDwpQuestionComparedRatesAreNull_ThenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionIsDescriptorFlow("no");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionAllowedOrRefused(null);

        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);

        verifyNoInteractions(footerService);
        assertNull(response.getData().getDwpState());
        assertEquals(1, (int) response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).count());

        assertNotNull(sscsCaseData.getWriteFinalDecisionTypeOfHearing());
        assertNotNull(sscsCaseData.getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionAppellantAttendedQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionStartDate());
        assertNotNull(sscsCaseData.getWriteFinalDecisionEndDateType());
        assertNotNull(sscsCaseData.getWriteFinalDecisionEndDate());
        assertNotNull(sscsCaseData.getWriteFinalDecisionDateOfDecision());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionCommunicatingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionMovingAroundQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionReasons());
        assertNotNull(sscsCaseData.getWriteFinalDecisionPageSectionReference());
        assertNotNull(sscsCaseData.getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getWriteFinalDecisionGeneratedDate());
        assertNotNull(sscsCaseData.getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());
    }

    @Test
    public void givenAnIssueFinalDecisionEventForNoYesFlowWhenComparedToDwpQuestionComparedRatesAreNullButApprovalStatusSet_ThenDoNotDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionIsDescriptorFlow("no");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null));
        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());

        assertEquals("decisionInFavourOfAppellant", response.getData().getOutcome());

        assertNull(sscsCaseData.getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionCommunicatingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMovingAroundQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionReasons());
        assertNull(sscsCaseData.getWriteFinalDecisionPageSectionReference());
        assertNull(sscsCaseData.getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getWriteFinalDecisionGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());
        assertNull(sscsCaseData.getWriteFinalDecisionAnythingElse());
    }

    @Test
    public void givenAnIssueFinalDecisionEventForYesNoFlowWhenComparedToDwpQuestionComparedRatesAreNotNullButApprovalNotSet_ThenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionAllowedOrRefused(null);

        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("no");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);

        verifyNoInteractions(footerService);
        assertNull(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());
        assertEquals(1, (int) response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).count());

        assertNotNull(sscsCaseData.getWriteFinalDecisionTypeOfHearing());
        assertNotNull(sscsCaseData.getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionAppellantAttendedQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionStartDate());
        assertNotNull(sscsCaseData.getWriteFinalDecisionEndDateType());
        assertNotNull(sscsCaseData.getWriteFinalDecisionEndDate());
        assertNotNull(sscsCaseData.getWriteFinalDecisionDateOfDecision());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionCommunicatingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNotNull(sscsCaseData.getPipWriteFinalDecisionMovingAroundQuestion());
        assertNotNull(sscsCaseData.getWriteFinalDecisionReasons());
        assertNotNull(sscsCaseData.getWriteFinalDecisionPageSectionReference());
        assertNotNull(sscsCaseData.getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getWriteFinalDecisionGeneratedDate());
        assertNotNull(sscsCaseData.getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());
    }

    @Test
    public void givenAnIssueFinalDecisionEventForYesNoFlowWhenComparedToDwpQuestionComparedRatesAreNullButApprovalStatusSet_ThenDoNotDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("no");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null));
        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());

        assertEquals("decisionInFavourOfAppellant", response.getData().getOutcome());

        assertNull(sscsCaseData.getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionCommunicatingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMovingAroundQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionReasons());
        assertNull(sscsCaseData.getWriteFinalDecisionPageSectionReference());
        assertNull(sscsCaseData.getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getWriteFinalDecisionGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());
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
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving);
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null));
        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());

        assertEquals(expectedOutcome, response.getData().getOutcome());

        assertNull(sscsCaseData.getWriteFinalDecisionTypeOfHearing());
        assertNull(sscsCaseData.getWriteFinalDecisionPresentingOfficerAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionAppellantAttendedQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionDisabilityQualifiedPanelMemberName());
        assertNull(sscsCaseData.getWriteFinalDecisionMedicallyQualifiedPanelMemberName());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionStartDate());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDateType());
        assertNull(sscsCaseData.getWriteFinalDecisionEndDate());
        assertNull(sscsCaseData.getWriteFinalDecisionDateOfDecision());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDailyLivingActivitiesQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMobilityActivitiesQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionPreparingFoodQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionTakingNutritionQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionManagingTherapyQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionWashAndBatheQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionManagingToiletNeedsQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionDressingAndUndressingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionCommunicatingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionReadingUnderstandingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionEngagingWithOthersQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionBudgetingDecisionsQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionPlanningAndFollowingQuestion());
        assertNull(sscsCaseData.getPipWriteFinalDecisionMovingAroundQuestion());
        assertNull(sscsCaseData.getWriteFinalDecisionReasons());
        assertNull(sscsCaseData.getWriteFinalDecisionPageSectionReference());
        assertNull(sscsCaseData.getWriteFinalDecisionPreviewDocument());
        assertNull(sscsCaseData.getWriteFinalDecisionGeneratedDate());
        assertNull(sscsCaseData.getWriteFinalDecisionIsDescriptorFlow());
        assertNull(sscsCaseData.getWriteFinalDecisionAllowedOrRefused());
        assertNull(sscsCaseData.getWriteFinalDecisionAnythingElse());

        assertNull(sscsCaseData.getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionMentalAssessmentQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionMobilisingUnaidedQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionStandingAndSittingQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionReachingQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionPickingUpQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionManualDexterityQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionMakingSelfUnderstoodQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionCommunicationQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionNavigationQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionLossOfControlQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionConsciousnessQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionLearningTasksQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionAwarenessOfHazardsQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionPersonalActionQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionCopingWithChangeQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionGettingAboutQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionSocialEngagementQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionAppropriatenessOfBehaviourQuestion());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionSchedule3ActivitiesApply());
        assertNull(sscsCaseData.getEsaWriteFinalDecisionSchedule3ActivitiesQuestion());
        assertNull(sscsCaseData.getShowRegulation29Page());
        assertNull(sscsCaseData.getShowSchedule3ActivitiesPage());
        assertNull(sscsCaseData.getShowFinalDecisionNoticeSummaryOfOutcomePage());
        assertNull(sscsCaseData.getWriteFinalDecisionDetailsOfDecision());
        assertNull(sscsCaseData.getWcaAppeal());
        assertNull(sscsCaseData.getSupportGroupOnlyAppeal());
        assertNull(sscsCaseData.getDoesRegulation29Apply());
        assertNull(sscsCaseData.getDoesRegulation35Apply());
    }

    @Test
    public void givenAnIssueFinalDecisionEventAndNoDraftDecisionOnCase_thenDisplayAnError() {
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionPreviewDocument(null);
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("There is no Preview Draft Decision Notice on the case so decision cannot be issued", error);
    }

    @Test
    public void givenANonPdfDecisionNotice_thenDisplayAnError() {
        DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionGenerateNotice("yes");
        callback.getCaseDetails().getCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

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
