package uk.gov.hmcts.reform.sscs.service.exceptions;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class ClientAuthorisationException extends RuntimeException {

    public ClientAuthorisationException(Exception ex) {
        super(ex);
    }
}
