package uk.gov.hmcts.reform.sscs.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.io.Serial;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(BAD_REQUEST)
public class InvalidMappingException extends ListingException {
    @Serial
    private static final long serialVersionUID = -5687439455391806310L;

    public InvalidMappingException(String message) {
        super(message);
    }
}
