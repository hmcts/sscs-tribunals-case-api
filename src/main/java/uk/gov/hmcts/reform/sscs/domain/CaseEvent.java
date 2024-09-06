package uk.gov.hmcts.reform.sscs.domain;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class CaseEvent {

    private final String caseCreatedEventId;
    private final String validAppealCreatedEventId;
    private final String incompleteApplicationEventId;
    private final String nonCompliantEventId;

    public CaseEvent(@Value("${ccd.case.caseCreatedEventId}") String caseCreatedEventId,
                     @Value("${ccd.case.validAppealCreatedEventId}") String validAppealCreatedEventId,
                     @Value("${ccd.case.incompleteApplicationEventId}") String incompleteApplicationEventId,
                     @Value("${ccd.case.nonCompliantEventId}") String nonCompliantEventId) {
        this.caseCreatedEventId = caseCreatedEventId;
        this.validAppealCreatedEventId = validAppealCreatedEventId;
        this.incompleteApplicationEventId = incompleteApplicationEventId;
        this.nonCompliantEventId = nonCompliantEventId;
    }

}
