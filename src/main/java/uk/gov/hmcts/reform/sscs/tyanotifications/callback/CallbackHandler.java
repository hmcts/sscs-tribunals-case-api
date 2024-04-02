package uk.gov.hmcts.reform.sscs.tyanotifications.callback;

import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;

public interface CallbackHandler {
    boolean canHandle(NotificationSscsCaseDataWrapper notificationSscsCaseDataWrapper);

    void handle(NotificationSscsCaseDataWrapper notificationSscsCaseDataWrapper);

    DispatchPriority getPriority();
}
