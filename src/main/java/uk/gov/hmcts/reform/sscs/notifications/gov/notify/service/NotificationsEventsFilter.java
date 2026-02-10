package uk.gov.hmcts.reform.sscs.notifications.gov.notify.service;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.NotificationEventTypeLists.*;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.*;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.CcdNotificationWrapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationsEventsFilter {
    private final NotificationProcessingService notificationProcessingService;
    private static final int RETRY = 1;
    private final NotificationExecutionManager notificationExecutionManager;

    @Value("${feature.postHearings.enabled}")
    private boolean isPostHearingsEnabled;

    @Value("${feature.postHearingsB.enabled}")
    private boolean isPostHearingsBEnabled;

    public boolean canHandle(NotificationSscsCaseDataWrapper callback) {
        return nonNull(callback.getNotificationEventType())
            && EVENTS_TO_HANDLE.contains(callback.getNotificationEventType())
            || (isPostHearingsEnabled && EVENTS_TO_HANDLE_POSTHEARINGS_A.contains(callback.getNotificationEventType()))
            || (isPostHearingsBEnabled && EVENTS_TO_HANDLE_POSTHEARINGS_B.contains(callback.getNotificationEventType()))
            || shouldActionPostponementBeNotified(callback)
            || hasNewAppointeeAddedForAppellantDeceasedCase(callback)
            || shouldHandleForHearingRoute(callback);
    }

    public void handle(NotificationSscsCaseDataWrapper callback) {
        if (!canHandle(callback)) {
            IllegalStateException illegalStateException = new IllegalStateException("Cannot handle callback");
            String caseId = Optional.ofNullable(callback.getOldSscsCaseData())
                .map(SscsCaseData::getCcdCaseId)
                .orElse(null);
            log.error("Cannot handle callback for event {} for caseId {}",
                callback.getNotificationEventType(), caseId, illegalStateException);
            throw illegalStateException;
        }
        final CcdNotificationWrapper notificationWrapper = new CcdNotificationWrapper(callback);
        try {
            notificationProcessingService.processNotification(notificationWrapper, false);
        } catch (NotificationServiceException e) {
            notificationExecutionManager.rescheduleIfHandledGovNotifyErrorStatus(RETRY, notificationWrapper, e);
            throw e;
        }
    }

    private boolean shouldActionPostponementBeNotified(NotificationSscsCaseDataWrapper callback) {
        String actionPostponementRequestSelected = callback.getNewSscsCaseData().getPostponementRequest().getActionPostponementRequestSelected();
        return ACTION_POSTPONEMENT_REQUEST.equals(callback.getNotificationEventType())
                && (!ProcessRequestAction.SEND_TO_JUDGE.getValue().equals(actionPostponementRequestSelected)
                && !ProcessRequestAction.REFUSE_ON_THE_DAY.getValue().equals(actionPostponementRequestSelected));
    }

    private boolean hasNewAppointeeAddedForAppellantDeceasedCase(NotificationSscsCaseDataWrapper callback) {
        if (!(DEATH_OF_APPELLANT.equals(callback.getNotificationEventType())
            || PROVIDE_APPOINTEE_DETAILS.equals(callback.getNotificationEventType()))) {
            return false;
        }

        Appointee appointeeBefore = null;
        if (callback.getOldSscsCaseData() != null
            && "yes".equalsIgnoreCase(callback.getOldSscsCaseData().getAppeal().getAppellant().getIsAppointee())
            && callback.getOldSscsCaseData().getAppeal().getAppellant().getAppointee() != null) {
            appointeeBefore = callback.getOldSscsCaseData().getAppeal().getAppellant().getAppointee();
        }

        Appointee appointeeAfter = null;
        if ("yes".equalsIgnoreCase(callback.getNewSscsCaseData().getAppeal().getAppellant().getIsAppointee())
            && callback.getNewSscsCaseData().getAppeal().getAppellant().getAppointee() != null) {
            appointeeAfter = callback.getNewSscsCaseData().getAppeal().getAppellant().getAppointee();
        }


        return ((appointeeBefore == null && appointeeAfter != null)
            || (appointeeBefore != null && appointeeAfter != null && !appointeeBefore.equals(appointeeAfter)));
    }

    private boolean shouldHandleForHearingRoute(NotificationSscsCaseDataWrapper callback) {
        return HEARING_BOOKED == callback.getNotificationEventType()
            && GAPS != callback.getNewSscsCaseData().getSchedulingAndListingFields().getHearingRoute();
    }

    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
