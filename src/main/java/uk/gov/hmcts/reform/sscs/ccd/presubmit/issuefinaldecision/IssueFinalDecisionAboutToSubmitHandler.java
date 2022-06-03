package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionBenefitTypeHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
public class IssueFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;
    private final DecisionNoticeService decisionNoticeService;
    private final Validator validator;
    private final ListAssistHearingMessageHelper hearingMessageHelper;
    private boolean isScheduleListingEnabled;

    public IssueFinalDecisionAboutToSubmitHandler(FooterService footerService,
        DecisionNoticeService decisionNoticeService, Validator validator,
            ListAssistHearingMessageHelper hearingMessageHelper,
                @Value("${feature.snl.enabled}") boolean isScheduleListingEnabled) {
        this.footerService = footerService;
        this.decisionNoticeService = decisionNoticeService;
        this.validator = validator;
        this.hearingMessageHelper = hearingMessageHelper;
        this.isScheduleListingEnabled = isScheduleListingEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.ISSUE_FINAL_DECISION
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
        for (ConstraintViolation<SscsCaseData> violation : violations) {
            preSubmitCallbackResponse.addError(violation.getMessage());
        }

        calculateOutcomeCode(sscsCaseData, preSubmitCallbackResponse);
        verifyPreviewDocument(sscsCaseData, preSubmitCallbackResponse);

        if (!preSubmitCallbackResponse.getErrors().isEmpty()) {
            return preSubmitCallbackResponse;
        }

        sscsCaseData.setDwpState(FINAL_DECISION_ISSUED.getId());
        createFinalDecisionNoticeFromPreviewDraft(preSubmitCallbackResponse);
        clearTransientFields(preSubmitCallbackResponse);
        log.info("Issue final decision request: Check eligible hearing request for case {}", sscsCaseData.getCcdCaseId());
        log.info("Issue final decision request: Feature flag {} for case {}", isScheduleListingEnabled,
                sscsCaseData.getCcdCaseId());
        log.info("Issue final decision request: SnL case determinant {} for case {}", sscsCaseData
                .getSchedulingAndListingFields().getHearingRoute(), sscsCaseData.getCcdCaseId());
        log.info("Issue final decision request: Sscscasedata case state {} for case {}", sscsCaseData.getState(),
                sscsCaseData.getCcdCaseId());
        log.info("Issue final decision request: Overall case state {} for case {}", callback.getCaseDetails()
                        .getState(), sscsCaseData.getCcdCaseId());
        log.info("Issue final decision request: Overall case state before {} for case {}",
                callback.getCaseDetailsBefore().map(CaseDetails::getState).orElse(null),
                sscsCaseData.getCcdCaseId());
        log.info("Issue final decision request: condition outcome 1 ({})", isScheduleListingEnabled);
        log.info("Issue final decision request: condition outcome 2 ({})", SscsUtil
                .isValidCaseState(callback.getCaseDetails().getState(), List.of(State.HEARING, State.READY_TO_LIST)));
        log.info("Issue final decision request: condition outcome 3 ({})", SscsUtil.isSAndLCase(sscsCaseData));
        log.info("Issue final decision request: consolidated condition ({})", eligibleForHearingsCancel
                .test(callback));
        if (eligibleForHearingsCancel.test(callback)) {
            log.info("Issue Final Decision: HearingRoute ListAssist Case ({}). Sending cancellation message",
                    sscsCaseData.getCcdCaseId());
            hearingMessageHelper.sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(),
                    CancellationReason.OTHER);
        }

        return preSubmitCallbackResponse;
    }

    private void verifyPreviewDocument(SscsCaseData sscsCaseData,
                                       PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument() == null) {
            preSubmitCallbackResponse
                    .addError("There is no Preview Draft Decision Notice on the case so decision cannot be issued");
        }
    }

    private final Predicate<Callback<SscsCaseData>> eligibleForHearingsCancel = callback -> isScheduleListingEnabled
            && SscsUtil.isValidCaseState(callback.getCaseDetails().getState(),
                List.of(State.HEARING, State.READY_TO_LIST))
            && SscsUtil.isSAndLCase(callback.getCaseDetails().getCaseData());

    private void calculateOutcomeCode(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        String benefitType = WriteFinalDecisionBenefitTypeHelper.getBenefitType(sscsCaseData);

        if (benefitType == null) {
            throw new IllegalStateException("Unable to determine benefit type");
        }

        DecisionNoticeOutcomeService decisionNoticeOutcomeService = decisionNoticeService.getOutcomeService(benefitType);

        Outcome outcome = decisionNoticeOutcomeService.determineOutcome(sscsCaseData);

        if (outcome != null) {
            sscsCaseData.setOutcome(outcome.getId());
        } else {
            log.error("Outcome cannot be empty when generating final decision. Something has gone wrong for caseId: {} ", sscsCaseData.getCcdCaseId());
            preSubmitCallbackResponse.addError("Outcome cannot be empty. Please check case data. If problem continues please contact support");
        }

    }

    private void createFinalDecisionNoticeFromPreviewDraft(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {

        SscsCaseData sscsCaseData = preSubmitCallbackResponse.getData();

        DocumentLink docLink = sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument();

        DocumentLink documentLink = DocumentLink.builder()
            .documentUrl(docLink.getDocumentUrl())
            .documentFilename(docLink.getDocumentFilename())
            .documentBinaryUrl(docLink.getDocumentBinaryUrl())
            .build();


        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        final SscsDocumentTranslationStatus documentTranslationStatus = sscsCaseData.isLanguagePreferenceWelsh() ? TRANSLATION_REQUIRED : null;
        footerService.createFooterAndAddDocToCase(documentLink, sscsCaseData, DocumentType.FINAL_DECISION_NOTICE, now,
                null, null, documentTranslationStatus);
        if (documentTranslationStatus != null) {
            sscsCaseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION.getId());
            log.info("Set the InterlocReviewState to {},  for case id : {}", sscsCaseData.getInterlocReviewState(), sscsCaseData.getCcdCaseId());
            sscsCaseData.setTranslationWorkOutstanding(YES.getValue());
        }
    }

    private void clearTransientFields(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        SscsCaseData sscsCaseData = preSubmitCallbackResponse.getData();

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionTypeOfHearing(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPresentingOfficerAttendedQuestion(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAppellantAttendedQuestion(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDisabilityQualifiedPanelMemberName(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionMedicallyQualifiedPanelMemberName(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionStartDate(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDate(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDateOfDecision(null);
        sscsCaseData.setWcaAppeal(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setOtherPartyAttendedQuestions(Lists.newArrayList());

        //PIP
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionTakingNutritionQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionManagingTherapyQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionWashAndBatheQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionManagingToiletNeedsQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDressingAndUndressingQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionCommunicatingQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionReadingUnderstandingQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionEngagingWithOthersQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionBudgetingDecisionsQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPlanningAndFollowingQuestion(null);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionReasons(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPageSectionReference(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionPreviewDocument(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGeneratedDate(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAllowedOrRefused(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionAnythingElse(null);

        //ESA
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMentalAssessmentQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMobilisingUnaidedQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionStandingAndSittingQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionReachingQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPickingUpQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionManualDexterityQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionMakingSelfUnderstoodQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionCommunicationQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionNavigationQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionLossOfControlQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionConsciousnessQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionLearningTasksQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionAwarenessOfHazardsQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionPersonalActionQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionCopingWithChangeQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionGettingAboutQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSocialEngagementQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionAppropriatenessOfBehaviourQuestion(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesApply(null);
        sscsCaseData.getSscsEsaCaseData().setEsaWriteFinalDecisionSchedule3ActivitiesQuestion(null);
        sscsCaseData.setDwpReassessTheAward(null);
        sscsCaseData.getSscsEsaCaseData().setShowRegulation29Page(null);
        sscsCaseData.getSscsEsaCaseData().setShowSchedule3ActivitiesPage(null);
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDetailsOfDecision(null);
        sscsCaseData.setSupportGroupOnlyAppeal(null);
        sscsCaseData.getSscsEsaCaseData().setDoesRegulation29Apply(null);
        sscsCaseData.getSscsEsaCaseData().setDoesRegulation35Apply(null);

        //UC
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMentalAssessmentQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionStandingAndSittingQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionReachingQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPickingUpQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionManualDexterityQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMakingSelfUnderstoodQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionCommunicationQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionNavigationQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionLossOfControlQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionConsciousnessQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionLearningTasksQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionAwarenessOfHazardsQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPersonalActionQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionCopingWithChangeQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionGettingAboutQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSocialEngagementQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionAppropriatenessOfBehaviourQuestion(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply(null);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesQuestion(null);
        sscsCaseData.setDwpReassessTheAward(null);
        sscsCaseData.getSscsUcCaseData().setShowSchedule8Paragraph4Page(null);
        sscsCaseData.getSscsUcCaseData().setShowSchedule7ActivitiesPage(null);
        sscsCaseData.setShowFinalDecisionNoticeSummaryOfOutcomePage(null);
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionDetailsOfDecision(null);
        sscsCaseData.setSupportGroupOnlyAppeal(null);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(null);
        sscsCaseData.getSscsUcCaseData().setDoesSchedule9Paragraph4Apply(null);

        preSubmitCallbackResponse.getData().getSscsDocument()
                .removeIf(doc -> doc.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue()));
    }

}
