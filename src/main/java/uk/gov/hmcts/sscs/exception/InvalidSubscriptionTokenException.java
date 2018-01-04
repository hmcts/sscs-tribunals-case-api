package uk.gov.hmcts.sscs.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.BAD_REQUEST, reason="Given token is invalid")
public class InvalidSubscriptionTokenException extends RuntimeException {

    public InvalidSubscriptionTokenException(String message){
        super(message);
    }
}
