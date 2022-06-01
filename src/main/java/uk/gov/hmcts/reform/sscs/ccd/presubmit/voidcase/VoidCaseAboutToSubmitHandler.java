package uk.gov.hmcts.reform.sscs.ccd.presubmit.voidcase;

import static java.util.Objects.requireNonNull;

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
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
public class VoidCaseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final ListAssistHearingMessageHelper hearingMessageHelper;
    private boolean isScheduleListingEnabled;

    public VoidCaseAboutToSubmitHandler(ListAssistHearingMessageHelper hearingMessageHelper,
        @Value("${feature.snl.enabled}") boolean isScheduleListingEnabled) {
        this.hearingMessageHelper = hearingMessageHelper;
        this.isScheduleListingEnabled = isScheduleListingEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && (callback.getEvent() == EventType.VOID_CASE
                || callback.getEvent() == EventType.ADMIN_SEND_TO_VOID_STATE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        log.info(String.format("Handling send to void state for case id %s", sscsCaseData.getCcdCaseId()));

        sscsCaseData.setDirectionDueDate(null);
        sscsCaseData.setInterlocReviewState(null);
        cancelHearing(callback);

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        return callbackResponse;
    }

    private void cancelHearing(Callback<SscsCaseData> callback) {
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        log.info("Void case: Cancel hearing conditions ({}) ({}) ({}) for case ({})", isScheduleListingEnabled,
                callback.getCaseDetails().getState(), sscsCaseData.getSchedulingAndListingFields().getHearingRoute(),
                sscsCaseData.getCcdCaseId());
        if (eligibleForHearingsCancel.test(callback)) {
            log.info("Void case: HearingRoute ListAssist Case ({}). Sending cancellation message",
                    sscsCaseData.getCcdCaseId());
            hearingMessageHelper.sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId());
        }
    }

    private final Predicate<Callback<SscsCaseData>> eligibleForHearingsCancel = callback -> isScheduleListingEnabled
            && EventType.VOID_CASE.equals(callback.getEvent())
            && SscsUtil.isValidCaseState(callback.getCaseDetails().getState(), List.of(State.HEARING, State.READY_TO_LIST))
            && SscsUtil.isSAndLCase(callback.getCaseDetails().getCaseData());
}
