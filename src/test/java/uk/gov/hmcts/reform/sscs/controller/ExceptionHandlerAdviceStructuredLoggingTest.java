package uk.gov.hmcts.reform.sscs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.exception.DocumentNotFoundException;

class ExceptionHandlerAdviceStructuredLoggingTest {

    private ExceptionHandlerAdvice exceptionHandlerAdvice;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        exceptionHandlerAdvice = new ExceptionHandlerAdvice();
        logger = (Logger) LoggerFactory.getLogger(ExceptionHandlerAdvice.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("Should log structured error with endpoint and method for AppealNotFoundException")
    void shouldLogStructuredErrorForAppealNotFound() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/appeals/123");
        WebRequest webRequest = new ServletWebRequest(servletRequest);

        AppealNotFoundException exception = new AppealNotFoundException("123");

        ResponseEntity<Object> response = exceptionHandlerAdvice.handleAppealNotFound(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(logEvent.getFormattedMessage())
                .contains("operation=unhandledException")
                .contains("endpoint=/appeals/123")
                .contains("method=GET")
                .contains("status=404");
    }

    @Test
    @DisplayName("Should log structured error with endpoint and method for DocumentNotFoundException")
    void shouldLogStructuredErrorForDocumentNotFound() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/documents/456");
        WebRequest webRequest = new ServletWebRequest(servletRequest);

        DocumentNotFoundException exception = new DocumentNotFoundException();

        ResponseEntity<Object> response = exceptionHandlerAdvice.handleDocumentNotFound(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(logEvent.getFormattedMessage())
                .contains("operation=unhandledException")
                .contains("endpoint=/documents/456")
                .contains("method=GET")
                .contains("status=404");
    }

    @Test
    @DisplayName("Should log structured error for unhandled exception with 500 status")
    void shouldLogStructuredErrorForUnhandledException() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", "/cases/submit");
        WebRequest webRequest = new ServletWebRequest(servletRequest);

        RuntimeException exception = new RuntimeException("Something went wrong");

        ResponseEntity<Object> response = exceptionHandlerAdvice.handleUnexpectedException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Internal server error");
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(logEvent.getFormattedMessage())
                .contains("operation=unhandledException")
                .contains("endpoint=/cases/submit")
                .contains("method=POST")
                .contains("status=500")
                .contains("error=Something went wrong");
    }

    @Test
    @DisplayName("Should log unknown endpoint and method when WebRequest is not a ServletWebRequest")
    void shouldLogUnknownWhenNotServletWebRequest() {
        // Use a Mockito mock of WebRequest which is NOT a ServletWebRequest instance
        WebRequest nonServletRequest = mock(WebRequest.class);

        RuntimeException exception = new RuntimeException("error msg");

        ResponseEntity<Object> response = exceptionHandlerAdvice.handleUnexpectedException(exception, nonServletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getFormattedMessage())
                .contains("endpoint=unknown")
                .contains("method=unknown");
    }

    @Test
    @DisplayName("Should include exception stack trace in log event")
    void shouldIncludeExceptionStackTraceInLog() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("PUT", "/case/789");
        WebRequest webRequest = new ServletWebRequest(servletRequest);

        RuntimeException exception = new RuntimeException("Detailed error");

        exceptionHandlerAdvice.handleUnexpectedException(exception, webRequest);

        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        // The throwable proxy should be present (the exception is passed to log.error)
        assertThat(logEvent.getThrowableProxy()).isNotNull();
        assertThat(logEvent.getThrowableProxy().getMessage()).isEqualTo("Detailed error");
    }

    @Test
    @DisplayName("Should handle exception with null message gracefully")
    void shouldHandleExceptionWithNullMessage() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("DELETE", "/case/999");
        WebRequest webRequest = new ServletWebRequest(servletRequest);

        RuntimeException exception = new RuntimeException((String) null);

        ResponseEntity<Object> response = exceptionHandlerAdvice.handleUnexpectedException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getFormattedMessage())
                .contains("operation=unhandledException")
                .contains("error=null");
    }
}
