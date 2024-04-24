package uk.gov.hmcts.reform.sscs.hearings.exception;

import uk.gov.hmcts.reform.sscs.hearings.exception.MessageProcessingException;

public class GetHearingException extends MessageProcessingException {
    private static final long serialVersionUID = -7151664282882753981L;

    public GetHearingException(String message) {
        super(message);
    }
}
