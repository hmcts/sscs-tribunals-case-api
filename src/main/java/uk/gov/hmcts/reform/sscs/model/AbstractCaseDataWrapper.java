package uk.gov.hmcts.reform.sscs.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Data
@SuperBuilder(toBuilder = true)
public class AbstractCaseDataWrapper<E> {

    private SscsCaseData newSscsCaseData;
    private SscsCaseData oldSscsCaseData;
    private E notificationEventType;
}
