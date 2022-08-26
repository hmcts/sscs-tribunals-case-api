package uk.gov.hmcts.reform.sscs.ccd.presubmit.notification;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.PARTY_NOTIFIED;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

import java.util.List;
import javax.validation.Valid;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSentSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.snl.enabled}")
    private boolean isScheduleListingEnabled;

    private List<EventType> eventsHandled = List.of(
        EventType.NOTIFICATION_SENT,
        EventType.LETTER_NOTIFICATION_SENT,
        EventType.EMAIL_NOTIFICATION_SENT,
        EventType.SMS_NOTIFICATION_SENT);

    private final ListAssistHearingMessageHelper hearingMessageHelper;

    @Override
    public boolean canHandle(CallbackType callbackType, @Valid Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType == CallbackType.SUBMITTED
            && nonNull(callback.getEvent()) && eventsHandled.contains(callback.getEvent())
            && isScheduleListingEnabled
            && nonNull(callback.getCaseDetails()) && SscsUtil.isSAndLCase(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, @Valid Callback<SscsCaseData> callback, String userAuthorisation) {
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        String caseId = caseData.getCcdCaseId();

        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        log.info("Notification Sent Submitted: Adding parties message to list assist queue for Case Id {}", caseId);

        hearingMessageHelper.sendHearingMessage(caseId, LIST_ASSIST, PARTY_NOTIFIED, null);

        return response;
    }
}
