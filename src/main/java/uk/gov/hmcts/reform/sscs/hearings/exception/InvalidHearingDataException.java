package uk.gov.hmcts.reform.sscs.hearings.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.reform.sscs.hearings.exception.MessageProcessingException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class InvalidHearingDataException extends MessageProcessingException {
    private static final long serialVersionUID = -4089879478303228007L;

    public InvalidHearingDataException(String message) {
        super(message);
    }
}
