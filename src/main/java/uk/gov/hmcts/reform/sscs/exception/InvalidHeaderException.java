package uk.gov.hmcts.reform.sscs.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.io.Serial;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(BAD_REQUEST)
public class InvalidHeaderException extends Exception  {
    @Serial
    private static final long serialVersionUID = 7849412332464241373L;

    public InvalidHeaderException(Exception ex) {
        super(ex);
    }
}
