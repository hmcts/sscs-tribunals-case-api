package uk.gov.hmcts.reform.sscs.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(BAD_REQUEST)
public class InvalidHearingDataException extends MessageProcessingException {
    private static final long serialVersionUID = -4089879478303228007L;

    public InvalidHearingDataException(String message) {
        super(message);
    }
}
