package uk.gov.hmcts.reform.sscs.tyanotifications.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.model.AbstractCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder(toBuilder = true)
public class NotificationSscsCaseDataWrapper extends AbstractCaseDataWrapper<NotificationEventType> {
    private NotificationEventType notificationEventType;
    private State state;

}
