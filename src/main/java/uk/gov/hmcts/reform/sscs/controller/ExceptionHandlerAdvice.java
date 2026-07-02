package uk.gov.hmcts.reform.sscs.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.exception.DocumentNotFoundException;

@Slf4j
@ControllerAdvice
class ExceptionHandlerAdvice extends ResponseEntityExceptionHandler {
    @ResponseBody
    @ExceptionHandler(value = { AppealNotFoundException.class })
    protected ResponseEntity<Object> handleAppealNotFound(AppealNotFoundException ex, WebRequest request) {
        logStructuredError(request, HttpStatus.NOT_FOUND, ex);
        return handleExceptionInternal(ex, "No appeal for given id", new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ResponseBody
    @ExceptionHandler(value = { DocumentNotFoundException.class })
    protected ResponseEntity<Object> handleDocumentNotFound(DocumentNotFoundException ex, WebRequest request) {
        logStructuredError(request, HttpStatus.NOT_FOUND, ex);
        return handleExceptionInternal(ex, "Document not found", new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ResponseBody
    @ExceptionHandler(value = { Exception.class })
    protected ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
        logStructuredError(request, HttpStatus.INTERNAL_SERVER_ERROR, ex);
        return handleExceptionInternal(ex, "Internal server error", new HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private void logStructuredError(WebRequest request, HttpStatus status, Exception ex) {
        String uri = "unknown";
        String method = "unknown";
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletRequest servletRequest = servletWebRequest.getRequest();
            uri = servletRequest.getRequestURI();
            method = servletRequest.getMethod();
        }
        log.error("operation=unhandledException endpoint={} method={} status={} error={}",
                uri, method, status.value(), ex.getMessage(), ex);
    }
}
