package uk.gov.hmcts.reform.sscs.tyanotifications.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class ClientAuthorisationException extends RuntimeException {

    public ClientAuthorisationException(Exception ex) {
        super(ex);
    }
}
