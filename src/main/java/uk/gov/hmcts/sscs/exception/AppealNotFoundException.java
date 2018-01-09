package uk.gov.hmcts.sscs.exception;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No appeal for given id")
public class AppealNotFoundException extends RuntimeException {

    public AppealNotFoundException(String appealNumber) {
        super(String.format("Appeal not found for appeal number: %s ", appealNumber));
    }

}
