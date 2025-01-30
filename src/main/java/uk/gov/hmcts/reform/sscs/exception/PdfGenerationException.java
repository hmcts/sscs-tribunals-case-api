package uk.gov.hmcts.reform.sscs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("squid:MaximumInheritanceDepth")
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR,
        reason = "Error while generating appeal pdf")
public class PdfGenerationException extends RuntimeException {
    public PdfGenerationException(String message, Throwable cause) {
        super(cause);
    }
}
