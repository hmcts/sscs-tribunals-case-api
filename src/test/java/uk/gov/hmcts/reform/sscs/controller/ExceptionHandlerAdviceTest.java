package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.exception.DocumentNotFoundException;

public class ExceptionHandlerAdviceTest {

    ExceptionHandlerAdvice exceptionHandlerAdvice = new ExceptionHandlerAdvice();

    @Mock
    private WebRequest webRequest;

    @Test
    public void willReturnNotFound() {
        AppealNotFoundException exception = new AppealNotFoundException("123a");
        ResponseEntity<Object> response = exceptionHandlerAdvice.handleAppealNotFound(exception, webRequest);
        assertThat(response.getStatusCode().value(), equalTo(404));
        assertThat(response.getBody(), equalTo("No appeal for given id"));
    }

    @Test
    public void willHandleDocumentNotFound() {
        DocumentNotFoundException exception = new DocumentNotFoundException();
        ResponseEntity<Object> response = exceptionHandlerAdvice.handleDocumentNotFound(exception, webRequest);
        assertThat(response.getStatusCode().value(), equalTo(404));
        assertThat(response.getBody(), equalTo("Document not found"));
    }

}
