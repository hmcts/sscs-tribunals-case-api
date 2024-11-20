package uk.gov.hmcts.reform.sscs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No appeal for given id")
public class AppealNotFoundException extends RuntimeException {

    public AppealNotFoundException(String appealNumber) {
        super("Appeal not found for appeal number: %s ".formatted(appealNumber));
    }

    public AppealNotFoundException(Long caseId) {
        super("Appeal not found for case id: %s ".formatted(caseId));
    }

}
