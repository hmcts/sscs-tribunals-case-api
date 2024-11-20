package uk.gov.hmcts.reform.sscs.exception;

import java.io.Serial;

public class ClientAuthorisationException extends Exception  {

    @Serial
    private static final long serialVersionUID = -6354006769932043468L;

    public ClientAuthorisationException(Exception ex) {
        super(ex);
    }
}
