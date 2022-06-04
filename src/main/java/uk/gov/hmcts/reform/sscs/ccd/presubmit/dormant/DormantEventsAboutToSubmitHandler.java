package uk.gov.hmcts.reform.sscs.ccd.presubmit.dormant;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIRM_LAPSED;

import java.util.List;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
public class DormantEventsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    protected final ListAssistHearingMessageHelper hearingMessageHelper;
    protected boolean isScheduleListingEnabled;

    public DormantEventsAboutToSubmitHandler(
            ListAssistHearingMessageHelper hearingMessageHelper,
            @Value("${feature.snl.enabled}") boolean isScheduleListingEnabled) {
        this.hearingMessageHelper = hearingMessageHelper;
        this.isScheduleListingEnabled = isScheduleListingEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && (callback.getEvent() == HMCTS_LAPSE_CASE
                || callback.getEvent() == CONFIRM_LAPSED
                || callback.getEvent() == WITHDRAWN
                || callback.getEvent() == LAPSED_REVISED
                || callback.getEvent() == DORMANT
                || callback.getEvent() == ADMIN_SEND_TO_DORMANT_APPEAL_STATE
                || callback.getEvent() == ADMIN_APPEAL_WITHDRAWN
                || callback.getEvent() == ISSUE_FINAL_DECISION
            );
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        log.info("Handling {} event for case id {}", callback.getEvent(), callback.getCaseDetails().getId());

        caseData.setInterlocReviewState(null);
        caseData.setDirectionDueDate(null);
        callback.getCaseDetailsBefore().ifPresent(f -> caseData.setPreviousState(f.getState()));

        String caseID = callback.getCaseDetails().getCaseData().getCcdCaseId();
        if (eligibleForHearingsCancel.test(callback)) {

            CancellationReason cancellationReason = null;
            switch (callback.getEvent()) {
                case CONFIRM_LAPSED:
                case HMCTS_LAPSE_CASE:
                case ADMIN_SEND_TO_DORMANT_APPEAL_STATE:
                case LAPSED_REVISED:
                    cancellationReason = CancellationReason.LAPSED;
                    break;
                case WITHDRAWN:
                    cancellationReason = CancellationReason.WITHDRAWN;
                    break;
                default: log.info("CaseID: {} - Event: {} is not handled for cancellation reason",
                        caseID, callback.getEvent());
                    break;
            }

            log.info("CaseID: {} - Event: {}. HearingRoute is List Assist - Sending cancellation message",
                    caseID, callback.getEvent());
            hearingMessageHelper.sendListAssistCancelHearingMessage(caseID, cancellationReason);
        }

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private final Predicate<Callback<SscsCaseData>> eligibleForHearingsCancel = callback ->
            SscsUtil.isValidCaseState(callback.getCaseDetailsBefore().map(CaseDetails::getState)
                    .orElse(State.UNKNOWN), List.of(State.HEARING, State.READY_TO_LIST))
            && SscsUtil.isSAndLCase(callback.getCaseDetails().getCaseData())
            && isValidCaseEventForCancellation(callback.getEvent())
            && isScheduleListingEnabled;

    private boolean isValidCaseEventForCancellation(EventType event) {
        return List.of(
                CONFIRM_LAPSED,
                HMCTS_LAPSE_CASE,
                ADMIN_SEND_TO_DORMANT_APPEAL_STATE,
                LAPSED_REVISED,
                WITHDRAWN).contains(event);
    }
}
