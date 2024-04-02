package uk.gov.hmcts.reform.sscs.tyanotifications.callback.handlers;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;

public class HandlerHelper {

    private HandlerHelper() {

    }

    public static NotificationSscsCaseDataWrapper buildTestCallbackForGivenData(SscsCaseData sscsCaseData, State state, NotificationEventType eventType) {
        return NotificationSscsCaseDataWrapper.builder()
            .newSscsCaseData(sscsCaseData)
            .oldSscsCaseData(sscsCaseData)
            .notificationEventType(eventType)
            .state(state).build();
    }
}
