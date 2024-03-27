package uk.gov.hmcts.reform.sscs.tyanotifications.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;

@Data
@Builder(toBuilder = true)
public class SscsCaseDataWrapper {

    private SscsCaseData newSscsCaseData;
    private SscsCaseData oldSscsCaseData;
    private NotificationEventType notificationEventType;
    private State state;

}
