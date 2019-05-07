package uk.gov.hmcts.reform.sscs.model;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Data
@Builder(toBuilder = true)
public class SscsCaseDataWrapper {

    private SscsCaseData newSscsCaseData;
    private SscsCaseData oldSscsCaseData;
    private EventType notificationEventType;

}
