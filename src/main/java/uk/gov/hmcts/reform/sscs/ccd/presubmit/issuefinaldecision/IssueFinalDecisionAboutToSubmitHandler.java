package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_CORRECTED_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.hasHearingScheduledInTheFuture;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.clearPostponementTransientFields;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionBenefitTypeHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.*;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
public class IssueFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final FooterService footerService;
    private final DecisionNoticeService decisionNoticeService;
    private final UserDetailsService userDetailsService;
    private final Validator validator;
    private final ListAssistHearingMessageHelper hearingMessageHelper;
    private final VenueDataLoader venueDataLoader;
    private boolean isScheduleListingEnabled;
    @Value("${feature.snl.adjournment.enabled}")
    private boolean isAdjournmentEnabled;
    @Value("${feature.postHearings.enabled}")
    private boolean isPostHearingsEnabled;

    public IssueFinalDecisionAboutToSubmitHandler(FooterService footerService,
                                                  DecisionNoticeService decisionNoticeService,
                                                  UserDetailsService userDetailsService,
                                                  Validator validator,
                                                  ListAssistHearingMessageHelper hearingMessageHelper,
                                                  VenueDataLoader venueDataLoader,
                                                  @Value("${feature.snl.enabled}") boolean isScheduleListingEnabled) {
        this.footerService = footerService;
        this.decisionNoticeService = decisionNoticeService;
        this.userDetailsService = userDetailsService;
        this.validator = validator;
        this.hearingMessageHelper = hearingMessageHelper;
        this.venueDataLoader = venueDataLoader;
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

        sscsCaseData.clearPoDetails();

        Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
        for (ConstraintViolation<SscsCaseData> violation : violations) {
            preSubmitCallbackResponse.addError(violation.getMessage());
        }

        calculateOutcomeCode(sscsCaseData, preSubmitCallbackResponse);
        verifyPreviewDocument(sscsCaseData, preSubmitCallbackResponse);

        if (!preSubmitCallbackResponse.getErrors().isEmpty()) {
            return preSubmitCallbackResponse;
        }

        if (isPostHearingsEnabled) {
            SscsFinalDecisionCaseData finalDecisionCaseData = sscsCaseData.getSscsFinalDecisionCaseData();

            if (isNull(finalDecisionCaseData.getFinalDecisionIssuedDate())) {
                finalDecisionCaseData.setFinalDecisionIssuedDate(LocalDate.now());
                finalDecisionCaseData.setFinalDecisionJudge(userDetailsService.buildLoggedInUserName(userAuthorisation));
                finalDecisionCaseData.setFinalDecisionHeldAt(SscsUtil.buildWriteFinalDecisionHeldAt(sscsCaseData, venueDataLoader));
            }
        }

        SscsUtil.createFinalDecisionNoticeFromPreviewDraft(callback, footerService, isPostHearingsEnabled);
        clearTransientFields(sscsCaseData);

        if ((!(State.READY_TO_LIST.equals(sscsCaseData.getState())
            || State.WITH_DWP.equals(sscsCaseData.getState())))
            && !SscsUtil.isCorrectionInProgress(sscsCaseData, isPostHearingsEnabled)) {
            sscsCaseData.setDwpState(FINAL_DECISION_ISSUED);
            sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        }

        clearPostponementTransientFields(sscsCaseData);

        if (eligibleForHearingsCancel.test(callback) && hasHearingScheduledInTheFuture(sscsCaseData)) {
            log.info("Issue Final Decision: HearingRoute ListAssist Case ({}). Sending cancellation message",
                    sscsCaseData.getCcdCaseId());
            hearingMessageHelper.sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(),
                    CancellationReason.OTHER);
        }

        if (isAdjournmentEnabled) {
            sscsCaseData.setIssueFinalDecisionDate(LocalDate.now());
        }

        sscsCaseData.getSscsFinalDecisionCaseData().setFinalDecisionWasOriginalDecisionUploaded(isYes(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice()) ? NO : YES);

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
            && SscsUtil.isValidCaseState(callback.getCaseDetailsBefore().map(CaseDetails::getState)
                    .orElse(State.UNKNOWN), List.of(State.HEARING, State.READY_TO_LIST))
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

    private void clearTransientFields(SscsCaseData sscsCaseData) {
        sscsCaseData.getSscsDocument()
                .removeIf(doc -> doc.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue()));
        sscsCaseData.getSscsDocument()
                .removeIf(doc -> doc.getValue().getDocumentType().equals(DRAFT_CORRECTED_NOTICE.getValue()));
    }
}
