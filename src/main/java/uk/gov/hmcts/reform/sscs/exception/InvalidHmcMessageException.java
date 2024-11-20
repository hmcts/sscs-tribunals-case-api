package uk.gov.hmcts.reform.sscs.exception;

import java.io.Serial;

public class InvalidHmcMessageException extends MessageProcessingException {
    @Serial
    private static final long serialVersionUID = 3892214875734169729L;

    public InvalidHmcMessageException(String message) {
        super(message);
    }
}
