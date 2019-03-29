package uk.gov.hmcts.reform.logging.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractLoggingException extends RuntimeException {

    private final AlertLevel alertLevel;
    private final String errorCode;

    private static final Logger log = LoggerFactory.getLogger(AbstractLoggingException.class);

    protected AbstractLoggingException(AlertLevel alertLevel, String errorCode, Throwable cause) {
        super(cause);

        this.alertLevel = alertLevel;
        this.errorCode = errorCode;
    }

    protected AbstractLoggingException(AlertLevel alertLevel, String errorCode, String message) {
        super(message);

        this.alertLevel = alertLevel;
        this.errorCode = errorCode;
    }

    protected AbstractLoggingException(AlertLevel alertLevel, String errorCode, String message, Throwable cause) {
        super(message, cause);

        this.alertLevel = alertLevel;
        this.errorCode = errorCode;
    }

    public AlertLevel getAlertLevel() {
        return alertLevel;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static AbstractLoggingException getFromThrowableProxy(
        ThrowableProxy proxy,
        String logger,
        Level eventLevel
    ) {
        Throwable eventException = proxy == null ? null : proxy.getThrowable();
        AbstractLoggingException exception = eventException == null ? null : extractFromEventException(eventException);

        if (eventLevel.isGreaterOrEqual(Level.ERROR) && exception == null && logger.startsWith("uk.gov.hmcts")) {
            triggerBadImplementationLog(eventException);
        }

        return exception;
    }

    private static AbstractLoggingException extractFromEventException(Throwable eventException) {
        if (eventException instanceof AbstractLoggingException) {
            return (AbstractLoggingException) eventException;
        } else if (eventException.getCause() instanceof AbstractLoggingException) {
            // for spring boot projects there's a generic exception wrapper
            // let's try to cast the cause instead
            return (AbstractLoggingException) eventException.getCause();
        }

        return null;
    }

    private static void triggerBadImplementationLog(Throwable cause) {
        Throwable invalid = new InvalidExceptionImplementation("AlertLevel is mandatory as per configuration", cause);
        String message;

        if (cause == null) {
            message = "Exception not found";
        } else {
            message = "Bad implementation of '" + cause.getClass().getCanonicalName() + "' in use";
        }

        log.error(message, invalid);
    }
}
