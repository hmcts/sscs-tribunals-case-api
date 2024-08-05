package uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder;

import org.springframework.stereotype.Component;

@Component
public class JobGroupGenerator {

    public String generate(
        String caseId,
        String group
    ) {
        return caseId + "_" + group;
    }
}
