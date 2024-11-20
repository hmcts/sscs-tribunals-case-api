package uk.gov.hmcts.reform.sscs.exception;

import java.io.Serial;

public class GetHearingException extends MessageProcessingException {
    @Serial
    private static final long serialVersionUID = -7151664282882753981L;

    public GetHearingException(String message) {
        super(message);
    }
}
