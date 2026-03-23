package uk.gov.hmcts.reform.sscs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BusinessEventLogger {

    public void logNotificationEvent(String operation, String caseId, String channel,
                                      String templateId, String outcome) {
        log.info("operation={} caseId={} channel={} templateId={} outcome={}",
            operation, caseId, channel, templateId, outcome);
    }

    public void logEvidenceShareEvent(String operation, String caseId, String outcome,
                                       String detail) {
        log.info("operation={} caseId={} outcome={} detail={}",
            operation, caseId, outcome, detail);
    }

    public void logHearingsEvent(String operation, String caseId, String hearingId,
                                  String outcome) {
        log.info("operation={} caseId={} hearingId={} outcome={}",
            operation, caseId, hearingId, outcome);
    }

    public void logDependencyError(String operation, String caseId, String dependency,
                                    String errorMessage) {
        log.error("operation={} caseId={} dependency={} error={}",
            operation, caseId, dependency, errorMessage);
    }
}
