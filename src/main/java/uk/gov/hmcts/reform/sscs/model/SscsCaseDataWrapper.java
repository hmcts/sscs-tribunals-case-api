package uk.gov.hmcts.reform.sscs.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

@Data
@SuperBuilder(toBuilder = true)
public class SscsCaseDataWrapper extends AbstractCaseDataWrapper<EventType> {
    private EventType notificationEventType;
}
