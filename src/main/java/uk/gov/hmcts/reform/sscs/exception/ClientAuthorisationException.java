package uk.gov.hmcts.reform.sscs.exception;

public class ClientAuthorisationException extends Exception  {

    private static final long serialVersionUID = -6354006769932043468L;

    public ClientAuthorisationException(Exception ex) {
        super(ex);
    }
}
