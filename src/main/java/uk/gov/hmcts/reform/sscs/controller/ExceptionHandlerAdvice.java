package uk.gov.hmcts.reform.sscs.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.exception.DocumentNotFoundException;

@ControllerAdvice
class ExceptionHandlerAdvice extends ResponseEntityExceptionHandler {
    @ResponseBody
    @ExceptionHandler(value = { AppealNotFoundException.class })
    protected ResponseEntity<Object> handleAppealNotFound(AppealNotFoundException ex, WebRequest request) {
        return handleExceptionInternal(ex, "No appeal for given id", new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ResponseBody
    @ExceptionHandler(value = { DocumentNotFoundException.class })
    protected ResponseEntity<Object> handleDocumentNotFound(DocumentNotFoundException ex, WebRequest request) {
        return handleExceptionInternal(ex, "Document not found", new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }
}
