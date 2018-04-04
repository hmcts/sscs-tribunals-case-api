package uk.gov.hmcts.sscs.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

public class TyaJsonProcessingException extends UnknownErrorCodeException {
    public TyaJsonProcessingException(Throwable cause) {
        super(AlertLevel.P4, cause);
    }
}