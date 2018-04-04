package uk.gov.hmcts.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

public class EmailSendFailedException extends UnknownErrorCodeException {

    public EmailSendFailedException(Throwable cause) {
        super(AlertLevel.P3, cause);
    }
}
