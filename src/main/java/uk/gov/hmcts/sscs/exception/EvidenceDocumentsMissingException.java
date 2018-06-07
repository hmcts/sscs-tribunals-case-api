package uk.gov.hmcts.sscs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class EvidenceDocumentsMissingException extends RuntimeException {
}
