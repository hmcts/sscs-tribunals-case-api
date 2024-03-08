package uk.gov.hmcts.reform.sscs.exception.evidenceshare;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class ClientAuthorisationException extends RuntimeException {

    public ClientAuthorisationException(Exception ex) {
        super(ex);
    }
}
