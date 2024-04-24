package uk.gov.hmcts.reform.sscs.hearings.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.reform.sscs.hearings.exception.ListingException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class InvalidMappingException extends ListingException {
    private static final long serialVersionUID = -5687439455391806310L;

    public InvalidMappingException(String message) {
        super(message);
    }
}
