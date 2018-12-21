package uk.gov.hmcts.reform.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class CreateCaseInCcdException extends UnknownErrorCodeException {

    public CreateCaseInCcdException(String message, Throwable cause) {
        super(AlertLevel.P3, message, cause);
    }
}
