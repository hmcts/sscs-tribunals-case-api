package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.util.FinalDecisionUtil;
import uk.gov.hmcts.reform.sscs.util.FinalDecisionUtil.FinalDecisionType;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
public class IssueFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;
    private final DecisionNoticeService decisionNoticeService;
    private final Validator validator;
    private final ListAssistHearingMessageHelper hearingMessageHelper;
    private boolean isScheduleListingEnabled;
    @Value("${feature.snl.adjournment.enabled}")
    private boolean isAdjournmentEnabled;
    @Value("${feature.postHearings.enabled}")
    private boolean isPostHearingsEnabled;

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

        FinalDecisionUtil.issueFinalDecisionNoticeFromPreviewDraft(preSubmitCallbackResponse, FinalDecisionType.INITIAL, footerService);

        if (isPostHearingsEnabled) {
            String generateNotice = sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice();
            log.info("Saving finalDecisionNoticeGenerated for case {} as {}", sscsCaseData.getCcdCaseId(), generateNotice);
            sscsCaseData.setFinalDecisionNoticeGenerated(YesNo.valueOf(generateNotice.toUpperCase()));
        }

        clearDraftDecisionNotice(preSubmitCallbackResponse);

        if (!(State.READY_TO_LIST.equals(sscsCaseData.getState())
            || State.WITH_DWP.equals(sscsCaseData.getState()))) {
            sscsCaseData.setDwpState(FINAL_DECISION_ISSUED);
            sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        }
        if (eligibleForHearingsCancel.test(callback)) {
            log.info("Issue Final Decision: HearingRoute ListAssist Case ({}). Sending cancellation message",
                    sscsCaseData.getCcdCaseId());
            hearingMessageHelper.sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(),
                    CancellationReason.OTHER);
        }

        if (isAdjournmentEnabled) {
            sscsCaseData.setIssueFinalDecisionDate(LocalDate.now());
        }
        return preSubmitCallbackResponse;
    }

    private static void clearDraftDecisionNotice(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        preSubmitCallbackResponse.getData().getSscsDocument()
            .removeIf(doc -> doc.getValue().getDocumentType().equals(DRAFT_DECISION_NOTICE.getValue()));
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

        String benefitType = FinalDecisionUtil.getBenefitType(sscsCaseData);

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

}
