package uk.gov.hmcts.reform.sscs.service.exceptions;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AuthorisationException extends UnknownErrorCodeException {

    public AuthorisationException(Exception ex) {
        super(AlertLevel.P4, ex);
    }
}
