package uk.gov.hmcts.reform.sscs.ccd.presubmit.notlistable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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

@Component
@Slf4j
public class NotListableAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final ListAssistHearingMessageHelper hearingMessageHelper;
    private boolean isScheduleListingEnabled;

    public NotListableAboutToSubmitHandler(
            ListAssistHearingMessageHelper hearingMessageHelper,
            @Value("${feature.snl.enabled}") boolean isScheduleListingEnabled) {
        this.hearingMessageHelper = hearingMessageHelper;
        this.isScheduleListingEnabled = isScheduleListingEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.NOT_LISTABLE
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (eligibleForHearingsCancel.test(callback)) {
            log.info("CaseID: {} - Event: {}. HearingRoute is List Assist - Sending cancellation message", sscsCaseData.getCcdCaseId(), callback.getEvent());
            hearingMessageHelper.sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(), CancellationReason.OTHER);
        }

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        sscsCaseData.setDirectionDueDate(sscsCaseData.getNotListableDueDate());
        clearTransientFields(sscsCaseData);

        return preSubmitCallbackResponse;
    }

    private void clearTransientFields(SscsCaseData sscsCaseData) {
        sscsCaseData.setNotListableDueDate(null);
    }

    private final Predicate<Callback<SscsCaseData>> eligibleForHearingsCancel = callback -> isScheduleListingEnabled
            && SscsUtil.isValidCaseState(
                    callback.getCaseDetailsBefore()
                            .map(CaseDetails::getState)
                            .orElse(State.UNKNOWN), List.of(State.HEARING, State.READY_TO_LIST))
            && SscsUtil.isSAndLCase(callback.getCaseDetails().getCaseData());
}
