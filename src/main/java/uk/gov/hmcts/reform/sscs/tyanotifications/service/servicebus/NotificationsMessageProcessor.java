package uk.gov.hmcts.reform.sscs.tyanotifications.service.servicebus;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.getNotificationByCcdEvent;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.buildSscsCaseDataWrapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.tyanotifications.callback.handlers.FilterNotificationsEventsHandler;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;

@Slf4j
@Component
@Lazy(false)
public class NotificationsMessageProcessor {

    private final FilterNotificationsEventsHandler filterNotificationsEventsHandler;

    public NotificationsMessageProcessor(FilterNotificationsEventsHandler filterNotificationsEventsHandler) {
        this.filterNotificationsEventsHandler = filterNotificationsEventsHandler;
    }

    public void processMessage(Callback<SscsCaseData> callback) {
        try {
            requireNonNull(callback, "callback must not be null");
            CaseDetails<SscsCaseData> caseDetailsBefore = callback.getCaseDetailsBefore().orElse(null);

            NotificationEventType event = getNotificationByCcdEvent(callback.getEvent());
            SscsCaseData caseData = callback.getCaseDetails().getCaseData();

            if (ISSUE_FINAL_DECISION.equals(event)
                && DwpState.CORRECTION_GRANTED.equals(caseData.getDwpState())) {
                return;
            }


            NotificationSscsCaseDataWrapper sscsCaseDataWrapper = buildSscsCaseDataWrapper(
                caseData,
                caseDetailsBefore != null ? caseDetailsBefore.getCaseData() : null,
                event,
                callback.getCaseDetails().getState());

            log.info("Ccd Response received for case id: {}, {}",
                sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(),
                sscsCaseDataWrapper.getNotificationEventType());

            if (filterNotificationsEventsHandler.canHandle(sscsCaseDataWrapper)) {
                log.info("Handling notifications for Sscs Case CCD callback `{}` for Case ID `{}`",
                    callback.getEvent(),
                    callback.getCaseDetails().getId());
                filterNotificationsEventsHandler.handle(sscsCaseDataWrapper);
            }

            log.info("Sscs Case CCD callback `{}` handled for Case ID `{}`", callback.getEvent(),
                callback.getCaseDetails().getId());
        } catch (Exception exception) {
            // unrecoverable. Catch to remove it from the queue.
            log.error(format("Caught unrecoverable error: %s", exception.getMessage()), exception);
        }
    }
}
