package uk.gov.hmcts.reform.logging.exception;

import static uk.gov.hmcts.reform.logging.exception.ErrorCode.UNKNOWN;

/**
 * Exception helper class to provide requirement of error code being present.
 * Since exception classes are favoured over error codes it is irrelevant feature we must implement.
 * Underlined "error code" in use: UNKNOWN
 */
public class UnknownErrorCodeException extends AbstractLoggingException {

    protected UnknownErrorCodeException(AlertLevel alertLevel, Throwable cause) {
        super(alertLevel, UNKNOWN, cause);
    }

    protected UnknownErrorCodeException(AlertLevel alertLevel, String message) {
        super(alertLevel, UNKNOWN, message);
    }

    protected UnknownErrorCodeException(AlertLevel alertLevel, String message, Throwable cause) {
        super(alertLevel, UNKNOWN, message, cause);
    }
}
