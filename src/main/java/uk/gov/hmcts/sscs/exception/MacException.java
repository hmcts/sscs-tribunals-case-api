package uk.gov.hmcts.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MacException extends UnknownErrorCodeException {
    public MacException(Throwable cause) {
        super(AlertLevel.P4, cause);
    }
}