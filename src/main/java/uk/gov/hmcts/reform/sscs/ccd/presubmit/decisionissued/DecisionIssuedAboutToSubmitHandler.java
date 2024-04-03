package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DecisionType.STRIKE_OUT;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.isFileAPdf;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.clearPostponementTransientFields;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
public class DecisionIssuedAboutToSubmitHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;
    private final ListAssistHearingMessageHelper hearingMessageHelper;
    private boolean isScheduleListingEnabled;
    private boolean isPostHearingsEnabled;

    public DecisionIssuedAboutToSubmitHandler(FooterService footerService, ListAssistHearingMessageHelper
            hearingMessageHelper, @Value("${feature.snl.enabled}") boolean isScheduleListingEnabled,
            @Value("${feature.postHearings.enabled}") boolean isPostHearingsEnabled) {
        this.footerService = footerService;
        this.hearingMessageHelper = hearingMessageHelper;
        this.isScheduleListingEnabled = isScheduleListingEnabled;
        this.isPostHearingsEnabled = isPostHearingsEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
                && (callback.getEvent() == EventType.DECISION_ISSUED
                || callback.getEvent() == EventType.DECISION_ISSUED_WELSH)
                && nonNull(callback.getCaseDetails())
                && nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if (isPostHearingsEnabled) {
            clearInterlocReferralReason(caseData);
        }

        final PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        DocumentLink url = null;
        if (nonNull(callback.getCaseDetails().getCaseData().getDocumentStaging().getPreviewDocument()) && callback.getEvent() == EventType.DECISION_ISSUED) {
            url = caseData.getDocumentStaging().getPreviewDocument();
        } else if (caseData.getSscsInterlocDecisionDocument() != null && callback.getEvent() == EventType.DECISION_ISSUED) {
            url = caseData.getSscsInterlocDecisionDocument().getDocumentLink();
            caseData.getDocumentStaging().setDateAdded(caseData.getSscsInterlocDecisionDocument().getDocumentDateAdded());
            if (!isFileAPdf(caseData.getSscsInterlocDecisionDocument().getDocumentLink())) {
                sscsCaseDataPreSubmitCallbackResponse.addError("You need to upload PDF documents only");
                return sscsCaseDataPreSubmitCallbackResponse;
            }
        }

        if (isNull(url) && callback.getEvent() != EventType.DECISION_ISSUED_WELSH) {
            sscsCaseDataPreSubmitCallbackResponse.addError("You need to upload a PDF document");
            return sscsCaseDataPreSubmitCallbackResponse;
        }

        SscsDocumentTranslationStatus documentTranslationStatus = caseData.isLanguagePreferenceWelsh() && callback.getEvent() == EventType.DECISION_ISSUED ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null;

        if (callback.getEvent() == EventType.DECISION_ISSUED) {
            footerService.createFooterAndAddDocToCase(url, caseData, DocumentType.DECISION_NOTICE,
                Optional.ofNullable(caseData.getDocumentStaging().getDateAdded()).orElse(LocalDate.now())
                    .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                caseData.getDocumentStaging().getDateAdded(), null, documentTranslationStatus);
        }

        if (!SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(documentTranslationStatus)) {
            State beforeState = callback.getCaseDetailsBefore().map(CaseDetails::getState).orElse(null);
            clearTransientFields(caseData);
            caseData.setDwpState(DwpState.STRUCK_OUT);
            caseData.setDirectionDueDate(null);

            if (STRIKE_OUT.getValue().equals(caseData.getDecisionType())) {
                if (State.INTERLOCUTORY_REVIEW_STATE.equals(beforeState)) {
                    caseData.setOutcome("nonCompliantAppealStruckout");
                } else {
                    caseData.setOutcome("struckOut");
                }
                caseData.setInterlocReviewState(null);
            }
            caseData.setState(State.DORMANT_APPEAL_STATE);
        } else {
            log.info("Case is a Welsh case so Decsion Notice requires translation for case id : {}", caseData.getCcdCaseId());
            clearBasicTransientFields(caseData);
            caseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION);
            log.info("Set the InterlocReviewState to {},  for case id : {}", caseData.getInterlocReviewState(), caseData.getCcdCaseId());
            caseData.setTranslationWorkOutstanding("Yes");

        }

        log.info("Saved the new interloc decision document for case id: " + caseData.getCcdCaseId());
        clearPostponementTransientFields(caseData);
        cancelHearing(callback);
        return sscsCaseDataPreSubmitCallbackResponse;
    }

    private void cancelHearing(Callback<SscsCaseData> callback) {
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (eligibleForHearingsCancel.test(callback)) {
            log.info("Issue interlocutory decision: HearingRoute ListAssist Case ({}). Sending cancellation message",
                    sscsCaseData.getCcdCaseId());
            hearingMessageHelper.sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(),
                    CancellationReason.STRUCK_OUT);
        }
    }

    private final Predicate<Callback<SscsCaseData>> eligibleForHearingsCancel = callback -> isScheduleListingEnabled
            && EventType.DECISION_ISSUED.equals(callback.getEvent())
            && SscsUtil.isValidCaseState(callback.getCaseDetailsBefore().map(CaseDetails::getState)
            .       orElse(State.UNKNOWN), List.of(State.HEARING, State.READY_TO_LIST))
            && SscsUtil.isSAndLCase(callback.getCaseDetails().getCaseData());

    // SSCS-11486 AC4
    private void clearInterlocReferralReason(SscsCaseData caseData) {
        caseData.setInterlocReferralReason(null);
    }
}
