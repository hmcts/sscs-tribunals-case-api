package uk.gov.hmcts.reform.logging.exception;

/**
 * As per business definition there can be different alert levels determined by Priorities. Hence P1, P2, ...
 * Priorities also represent the importance of the issue and how quickly business should respond to it.
 *
 * @see <a href="https://tools.hmcts.net/confluence/display/DIP/Major+Incident+Management+Process#MajorIncidentManagementProcess-IncidentPriorities(Baseline)">Read more on Confluence</a>
 */
public enum AlertLevel {
    P1, P2, P3, P4
}
