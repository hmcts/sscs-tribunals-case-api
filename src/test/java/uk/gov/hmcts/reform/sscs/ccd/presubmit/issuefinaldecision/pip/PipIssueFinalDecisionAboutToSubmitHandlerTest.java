package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.pip;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision.IssueFinalDecisionAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;

class PipIssueFinalDecisionAboutToSubmitHandlerTest {

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
    private UserDetailsService userDetailsService;

    @Mock
    private ListAssistHearingMessageHelper hearingMessageHelper;

    @Mock
    private VenueDataLoader venueDataLoader;

    private SscsCaseData sscsCaseData;

    private SscsDocument document;

    protected static Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @BeforeEach
    void setUp() throws IOException {
        openMocks(this);
        pipDecisionNoticeOutcomeService = new PipDecisionNoticeOutcomeService(new PipDecisionNoticeQuestionService());

        decisionNoticeService = new DecisionNoticeService(new ArrayList<>(), Arrays.asList(pipDecisionNoticeOutcomeService), new ArrayList<>());

        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, userDetailsService,
                validator, hearingMessageHelper, venueDataLoader, false);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetails));

        final List<SscsDocument> documentList = new ArrayList<>();

        final SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(DRAFT_DECISION_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));


        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .state(State.HEARING)
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build()).build())
            .sscsDocument(documentList)
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionGenerateNotice(null)
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
            .sscsEsaCaseData(SscsEsaCaseData.builder().showRegulation29Page(YES)
                .showSchedule3ActivitiesPage(YES).doesRegulation29Apply(YES)
                .doesRegulation35Apply(YES).build())
            .dwpReassessTheAward("")
            .showFinalDecisionNoticeSummaryOfOutcomePage(YES)
            .supportGroupOnlyAppeal("")
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                    .hearingRoute(HearingRoute.LIST_ASSIST)
                    .build())

            .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(State.HEARING);
    }

    @Test
    void givenANonIssueFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenAnIssueFinalDecisionEventRemoveDraftDecisionNotice() {
        final DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        final SscsFinalDecisionCaseData finalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        finalDecisionCaseData.setWriteFinalDecisionPreviewDocument(docLink);
        finalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        finalDecisionCaseData.setWriteFinalDecisionGenerateNotice(NO);
        finalDecisionCaseData.setWriteFinalDecisionIsDescriptorFlow("no");

        final SscsDocument document1 = buildSscsDocumentWithDocumentType(DRAFT_DECISION_NOTICE.getValue());
        final SscsDocument document2 = buildSscsDocumentWithDocumentType(FINAL_DECISION_NOTICE.getValue());

        final List<SscsDocument> documentList = new ArrayList<>(List.of(document1, document2));
        callback.getCaseDetails().getCaseData().setInternalCaseDocumentData(InternalCaseDocumentData.builder().sscsInternalDocument(documentList).build());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getInternalCaseDocumentData().getSscsInternalDocument()).hasSize(1);
    }

    @Test
    void givenAnIssueFinalDecisionEventWhenDocumentTypeIsNullThenRemoveDraftDecisionNotice() {
        final DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        final SscsFinalDecisionCaseData finalDecisionCaseData = callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData();
        finalDecisionCaseData.setWriteFinalDecisionPreviewDocument(docLink);
        finalDecisionCaseData.setWriteFinalDecisionAllowedOrRefused("allowed");
        finalDecisionCaseData.setWriteFinalDecisionGenerateNotice(NO);
        finalDecisionCaseData.setWriteFinalDecisionIsDescriptorFlow("no");

        final SscsDocument document1 = buildSscsDocumentWithDocumentType(FINAL_DECISION_NOTICE.getValue());
        final SscsDocument document3 = buildSscsDocumentWithDocumentType(DRAFT_DECISION_NOTICE.getValue());

        final List<SscsDocument> documentList = new ArrayList<>(List.of(document1, document3));
        callback.getCaseDetails().getCaseData().setInternalCaseDocumentData(InternalCaseDocumentData.builder().sscsInternalDocument(documentList).build());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getInternalCaseDocumentData().getSscsInternalDocument()).hasSize(1);
    }

    @Test
    void givenAnIssueFinalDecisionEventForYesYesFlowWhenComparedToDwpQuestionComparedRatesAreNull_ThenDisplayAnError() {
        final DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(null);

        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        final String error = response.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("Outcome cannot be empty. Please check case data. If problem continues please contact support");

        verifyNoInteractions(footerService);
        assertThat(response.getData().getDwpState()).isNull();
        assertThat(response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).count()).isEqualTo(1);

        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion()).isNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion()).isNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingActivitiesQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityActivitiesQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPreparingFoodQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionTakingNutritionQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingTherapyQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionWashAndBatheQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingToiletNeedsQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDressingAndUndressingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionCommunicatingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionReadingUnderstandingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionEngagingWithOthersQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionBudgetingDecisionsQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPlanningAndFollowingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMovingAroundQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate()).isNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused()).isNull();
        assertThat(sscsCaseData.getSscsEsaCaseData().getShowRegulation29Page()).isNotNull();
        assertThat(sscsCaseData.getSscsEsaCaseData().getShowSchedule3ActivitiesPage()).isNotNull();
        assertThat(sscsCaseData.getShowFinalDecisionNoticeSummaryOfOutcomePage()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDetailsOfDecision()).isNotNull();
    }

    @Test
    void givenAnIssueFinalDecisionEventForNoYesFlowWhenComparedToDwpQuestionComparedRatesAreNull_ThenDisplayAnError() {
        final DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("no");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(null);

        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        final String error = response.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("Outcome cannot be empty. Please check case data. If problem continues please contact support");

        verifyNoInteractions(footerService);
        assertThat(response.getData().getDwpState()).isNull();
        assertThat(response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).count()).isEqualTo(1);

        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion()).isNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion()).isNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingActivitiesQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityActivitiesQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPreparingFoodQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionTakingNutritionQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingTherapyQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionWashAndBatheQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingToiletNeedsQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDressingAndUndressingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionCommunicatingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionReadingUnderstandingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionEngagingWithOthersQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionBudgetingDecisionsQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPlanningAndFollowingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMovingAroundQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate()).isNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused()).isNull();
    }

    @Test
    void givenAnIssueFinalDecisionEventForYesNoFlowWhenComparedToDwpQuestionComparedRatesAreNotNullButApprovalNotSet_ThenDisplayAnError() {
        final DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(null);

        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(NO);
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        final String error = response.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("Outcome cannot be empty. Please check case data. If problem continues please contact support");

        verifyNoInteractions(footerService);
        assertThat(response.getData().getDwpState()).as(FINAL_DECISION_ISSUED.getCcdDefinition()).isNull();
        assertThat(response.getData().getSscsDocument().stream().filter(f -> f.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue())).count()).isEqualTo(1);

        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionTypeOfHearing()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPresentingOfficerAttendedQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAppellantAttendedQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDisabilityQualifiedPanelMemberName()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionMedicallyQualifiedPanelMemberName()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionStartDate()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDate()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionDateOfDecision()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingActivitiesQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityActivitiesQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPreparingFoodQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionTakingNutritionQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingTherapyQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionWashAndBatheQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionManagingToiletNeedsQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionDressingAndUndressingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionCommunicatingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionReadingUnderstandingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionEngagingWithOthersQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionBudgetingDecisionsQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionPlanningAndFollowingQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsPipCaseData().getPipWriteFinalDecisionMovingAroundQuestion()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionReasons()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPageSectionReference()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGeneratedDate()).isNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow()).isNotNull();
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused()).isNull();
    }

    @Test
    void givenIssueFinalDecisionEventWithWelshAppeal_thenTranslationIsRequired() {
        sscsCaseData.setLanguagePreferenceWelsh(YES.getValue());
        final DocumentLink docLink = DocumentLink.builder().documentUrl("bla.com").documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))).build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("same");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("same");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(docLink), any(), eq(FINAL_DECISION_NOTICE), any(), eq(null), eq(null), eq(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED));

        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getTranslationWorkOutstanding()).isEqualTo(YES.getValue());
        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.WELSH_TRANSLATION);
    }

    @Test
    void givenAnIssueFinalDecisionEventAndNoDraftDecisionOnCase_thenDisplayAnError() {
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(null);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        final String error = response.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("There is no Preview Draft Decision Notice on the case so decision cannot be issued");
    }

    @Test
    void givenANonPdfDecisionNotice_thenDisplayAnError() {
        final DocumentLink docLink = DocumentLink.builder().documentUrl("test.doc").build();
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher");
        callback.getCaseDetails().getCaseData().getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion("higher");

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        final String error = response.getErrors().stream().findFirst().orElse("");
        assertThat(error).isEqualTo("You need to upload PDF documents only");
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(final CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenAnIssueFinalDecisionEventIfHearingsIsNull_ThenDoNotSendHearingCancellationRequest() {
        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, userDetailsService,
                validator, hearingMessageHelper, venueDataLoader, true);
        final DocumentLink docLink = DocumentLink.builder()
                .documentUrl("bla.com")
                .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");
        callback.getCaseDetails().getCaseData().setHearings(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        verify(hearingMessageHelper, times(0)).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()), eq(CancellationReason.OTHER));
    }

    @Test
    void givenAnIssueFinalDecisionEventIfHearingsIsInThePastOnly_ThenDoNotSendHearingCancellationRequest() {
        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, userDetailsService,
                validator, hearingMessageHelper, venueDataLoader, true);
        final DocumentLink docLink = DocumentLink.builder()
                .documentUrl("bla.com")
                .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        final HearingDetails hearingDetails1 = HearingDetails.builder()
                .hearingDate(LocalDate.now().minusDays(10).toString())
                .start(LocalDateTime.now().minusDays(10))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        final Hearing hearing1 = Hearing.builder().value(hearingDetails1).build();

        final HearingDetails hearingDetails2 = HearingDetails.builder()
                .hearingDate(LocalDate.now().minusDays(5).toString())
                .start(LocalDateTime.now().minusDays(5))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        final Hearing hearing2 = Hearing.builder().value(hearingDetails2).build();

        callback.getCaseDetails().getCaseData().setHearings(List.of(hearing1, hearing2));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        verify(hearingMessageHelper, times(0)).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()), eq(CancellationReason.OTHER));
    }

    @Test
    void givenAnIssueFinalDecisionEventIfHearingsIsInThePastAndInTheFuture_ThenSendHearingCancellationRequest() {
        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, userDetailsService,
                validator, hearingMessageHelper, venueDataLoader, true);
        final DocumentLink docLink = DocumentLink.builder()
                .documentUrl("bla.com")
                .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        final HearingDetails hearingDetails1 = HearingDetails.builder()
                .hearingDate(LocalDate.now().minusDays(10).toString())
                .start(LocalDateTime.now().minusDays(10))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        final Hearing hearing1 = Hearing.builder().value(hearingDetails1).build();

        final HearingDetails hearingDetails2 = HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(5).toString())
                .start(LocalDateTime.now().plusDays(5))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        final Hearing hearing2 = Hearing.builder().value(hearingDetails2).build();

        callback.getCaseDetails().getCaseData().setHearings(List.of(hearing1, hearing2));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()), eq(CancellationReason.OTHER));
    }

    @Test
    void givenAnIssueFinalDecisionEventIfHearingsIsInTheFutureOnly_ThenSendHearingCancellationRequest() {
        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, userDetailsService,
                validator, hearingMessageHelper, venueDataLoader, true);
        final DocumentLink docLink = DocumentLink.builder()
                .documentUrl("bla.com")
                .documentFilename(String.format("Decision Notice issued on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                .build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        final HearingDetails hearingDetails = HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(5).toString())
                .start(LocalDateTime.now().plusDays(5))
                .hearingId(String.valueOf(1))
                .venue(Venue.builder().name("Venue 1").build())
                .time("12:00")
                .build();
        final Hearing hearing = Hearing.builder().value(hearingDetails).build();

        callback.getCaseDetails().getCaseData().setHearings(List.of(hearing));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()), eq(CancellationReason.OTHER));
    }

    @ParameterizedTest
    @MethodSource("judgeOnlyCompositions")
    void givenJudgeOnlyReadyToListCaseAwaitingListing_whenIssueFinalDecision_thenSendHearingCancellationRequest(
        final PanelMemberComposition panelMemberComposition) {
        prepareCaseInStateBeforeIssuingFinalDecision(State.READY_TO_LIST, panelMemberComposition);
        sscsCaseData.setHearings(List.of(awaitingListingHearing(HearingStatus.AWAITING_LISTING)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getState()).isEqualTo(State.READY_TO_LIST);
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(), CancellationReason.OTHER);
    }

    @Test
    void givenJudgeOnlyReadyToListCaseWithPastAndAwaitingListingHearings_whenIssueFinalDecision_thenSendHearingCancellationRequest() {
        prepareCaseInStateBeforeIssuingFinalDecision(State.READY_TO_LIST, judgeOnlyComposition());
        sscsCaseData.setHearings(List.of(pastHearing(), awaitingListingHearing(HearingStatus.AWAITING_LISTING)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(), CancellationReason.OTHER);
    }

    @Test
    void givenJudgeOnlyReadyToListCaseWithNoHearings_whenIssueFinalDecision_thenDoNotSendHearingCancellationRequest() {
        prepareCaseInStateBeforeIssuingFinalDecision(State.READY_TO_LIST, judgeOnlyComposition());
        sscsCaseData.setHearings(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenJudgeOnlyReadyToListCaseWithPastHearingOnly_whenIssueFinalDecision_thenDoNotSendHearingCancellationRequest() {
        prepareCaseInStateBeforeIssuingFinalDecision(State.READY_TO_LIST, judgeOnlyComposition());
        sscsCaseData.setHearings(List.of(pastHearing()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenJudgeOnlyReadyToListCaseWithCancelledListingRequest_whenIssueFinalDecision_thenDoNotSendHearingCancellationRequest() {
        prepareCaseInStateBeforeIssuingFinalDecision(State.READY_TO_LIST, judgeOnlyComposition());
        sscsCaseData.setHearings(List.of(awaitingListingHearing(HearingStatus.CANCELLED)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verifyNoInteractions(hearingMessageHelper);
    }

    @ParameterizedTest
    @MethodSource("nonJudgeOnlyCompositions")
    void givenNonJudgeOnlyReadyToListCaseAwaitingListing_whenIssueFinalDecision_thenDoNotSendHearingCancellationRequest(
        final PanelMemberComposition panelMemberComposition) {
        prepareCaseInStateBeforeIssuingFinalDecision(State.READY_TO_LIST, panelMemberComposition);
        sscsCaseData.setHearings(List.of(awaitingListingHearing(HearingStatus.AWAITING_LISTING)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenReadyToListCaseAwaitingListingWithNoPanelComposition_whenIssueFinalDecision_thenDoNotSendHearingCancellationRequest() {
        prepareCaseInStateBeforeIssuingFinalDecision(State.READY_TO_LIST, null);
        sscsCaseData.setHearings(List.of(awaitingListingHearing(HearingStatus.AWAITING_LISTING)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verifyNoInteractions(hearingMessageHelper);
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names = {"HEARING", "WITH_DWP", "RESPONSE_RECEIVED", "UNKNOWN"})
    void givenJudgeOnlyCaseAwaitingListingNotInReadyToList_whenIssueFinalDecision_thenDoNotSendHearingCancellationRequest(
        final State stateBefore) {
        prepareCaseInStateBeforeIssuingFinalDecision(stateBefore, judgeOnlyComposition());
        sscsCaseData.setHearings(List.of(awaitingListingHearing(HearingStatus.AWAITING_LISTING)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenJudgeOnlyCaseAwaitingListingWithNoCaseDetailsBefore_whenIssueFinalDecision_thenDoNotSendHearingCancellationRequest() {
        prepareCaseInStateBeforeIssuingFinalDecision(State.READY_TO_LIST, judgeOnlyComposition());
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.empty());
        sscsCaseData.setHearings(List.of(awaitingListingHearing(HearingStatus.AWAITING_LISTING)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenJudgeOnlyReadyToListCaseAwaitingListingAndScheduleListingDisabled_whenIssueFinalDecision_thenDoNotSendHearingCancellationRequest() {
        prepareCaseInStateBeforeIssuingFinalDecision(State.READY_TO_LIST, judgeOnlyComposition());
        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, userDetailsService,
            validator, hearingMessageHelper, venueDataLoader, false);
        sscsCaseData.setHearings(List.of(awaitingListingHearing(HearingStatus.AWAITING_LISTING)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenJudgeOnlyReadyToListCaseAwaitingListingOnGapsRoute_whenIssueFinalDecision_thenDoNotSendHearingCancellationRequest() {
        prepareCaseInStateBeforeIssuingFinalDecision(State.READY_TO_LIST, judgeOnlyComposition());
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.GAPS);
        sscsCaseData.setHearings(List.of(awaitingListingHearing(HearingStatus.AWAITING_LISTING)));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    void givenNonJudgeOnlyReadyToListCaseWithFutureHearing_whenIssueFinalDecision_thenSendHearingCancellationRequest() {
        prepareCaseInStateBeforeIssuingFinalDecision(State.READY_TO_LIST, judgeAndMedicalMemberComposition());
        final HearingDetails hearingDetails = HearingDetails.builder()
                                                            .hearingDate(LocalDate.now().plusDays(5).toString())
                                                            .start(LocalDateTime.now().plusDays(5))
                                                            .hearingId("1")
                                                            .venue(Venue.builder().name("Venue 1").build())
                                                            .time("12:00")
                                                            .hearingStatus(HearingStatus.LISTED)
                                                            .build();
        sscsCaseData.setHearings(List.of(Hearing.builder().value(hearingDetails).build()));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(), CancellationReason.OTHER);
    }

    @Test
    void givenWriteFinalDecisionPostHearingsEnabledAndNoIssueFinalDate_shouldUpdateFinalCaseData() {
        final String filename = String.format("Decision Notice issued on %s.pdf",
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")));
        final DocumentLink docLink = DocumentLink.builder()
                                                 .documentUrl("bla.com")
                                                 .documentFilename(filename)
                                                 .build();
        ReflectionTestUtils.setField(handler, "isPostHearingsEnabled", true);
        final SscsFinalDecisionCaseData sscsFinalDecisionCaseData = callback
            .getCaseDetails()
            .getCaseData()
            .getSscsFinalDecisionCaseData();
        sscsFinalDecisionCaseData.setWriteFinalDecisionPreviewDocument(docLink);
        sscsFinalDecisionCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsFinalDecisionCaseData.setWriteFinalDecisionGenerateNotice(YES);
        callback
            .getCaseDetails()
            .getCaseData()
            .getSscsPipCaseData()
            .setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion("same");
        callback
            .getCaseDetails()
            .getCaseData()
            .getSscsPipCaseData()
            .setPipWriteFinalDecisionComparedToDwpMobilityQuestion("same");
        sscsCaseData.getSscsFinalDecisionCaseData().setFinalDecisionIssuedDate(null);
        when(userDetailsService.buildLoggedInUserSurname(USER_AUTHORISATION)).thenReturn("judge name");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getFinalDecisionIssuedDate()).isEqualTo(LocalDate.now());
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getFinalDecisionJudge()).isEqualTo("judge name");
        assertThat(sscsCaseData.getSscsFinalDecisionCaseData().getFinalDecisionHeldAt()).isEqualTo("In chambers");
    }

    private static PanelMemberComposition judgeOnlyComposition() {
        return PanelMemberComposition.builder().panelCompositionJudge("84").build();
    }

    private static PanelMemberComposition judgeAndMedicalMemberComposition() {
        return PanelMemberComposition.builder().panelCompositionJudge("84").panelCompositionMemberMedical1("58").build();
    }

    private static Stream<PanelMemberComposition> judgeOnlyCompositions() {
        return Stream.of(
            judgeOnlyComposition(),
            PanelMemberComposition.builder().districtTribunalJudge("74").build());
    }

    private static Stream<PanelMemberComposition> nonJudgeOnlyCompositions() {
        return Stream.of(
            judgeAndMedicalMemberComposition(),
            PanelMemberComposition.builder().panelCompositionJudge("84").panelCompositionMemberMedical2("58").build(),
            PanelMemberComposition.builder().panelCompositionJudge("84")
                                  .panelCompositionDisabilityAndFqMember(List.of("44")).build(),
            PanelMemberComposition.builder().build());
    }

    private static Hearing awaitingListingHearing(final HearingStatus hearingStatus) {
        return Hearing.builder()
                      .value(HearingDetails.builder().hearingId("2").hearingStatus(hearingStatus).build())
                      .build();
    }

    private static Hearing pastHearing() {
        return Hearing.builder()
                      .value(HearingDetails.builder()
                                           .hearingDate(LocalDate.now().minusDays(5).toString())
                                           .start(LocalDateTime.now().minusDays(5))
                                           .hearingId("1")
                                           .venue(Venue.builder().name("Venue 1").build())
                                           .time("12:00")
                                           .hearingStatus(HearingStatus.LISTED)
                                           .build())
                      .build();
    }

    private void prepareCaseInStateBeforeIssuingFinalDecision(final State stateBefore,
        final PanelMemberComposition panelMemberComposition) {
        handler = new IssueFinalDecisionAboutToSubmitHandler(footerService, decisionNoticeService, userDetailsService,
            validator, hearingMessageHelper, venueDataLoader, true);
        final DocumentLink docLink = DocumentLink.builder()
                                                 .documentUrl("bla.com")
                                                 .documentFilename(String.format("Decision Notice issued on %s.pdf",
                                                     LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))))
                                                 .build();
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(docLink);
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        callback.getCaseDetails().getCaseData().getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused("allowed");

        final CaseDetails<SscsCaseData> caseDetailsBefore = mock(CaseDetails.class);
        when(caseDetailsBefore.getState()).thenReturn(stateBefore);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetails.getState()).thenReturn(stateBefore);
        sscsCaseData.setState(stateBefore);
        sscsCaseData.setPanelMemberComposition(panelMemberComposition);
    }

    private SscsDocument buildSscsDocumentWithDocumentType(final String documentType) {
        final SscsDocumentDetails sscsDocumentDetails = SscsDocumentDetails.builder().documentType(documentType).build();
        return SscsDocument.builder().value(sscsDocumentDetails).build();
    }
}
