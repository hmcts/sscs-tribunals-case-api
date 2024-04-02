package uk.gov.hmcts.reform.sscs.tyanotifications.domain;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.model.AbstractCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;

@Data
@SuperBuilder(toBuilder = true)
public class NotificationSscsCaseDataWrapper extends AbstractCaseDataWrapper<NotificationEventType> {
    private NotificationEventType notificationEventType;
    private State state;

}
