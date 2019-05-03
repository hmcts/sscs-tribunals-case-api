package uk.gov.hmcts.reform.sscs.service.exceptions;

import uk.gov.hmcts.reform.sscs.exception.ApplicationErrorException;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AuthorisationException extends ApplicationErrorException {

    public AuthorisationException(Throwable cause) {
        super(cause);
    }
}
