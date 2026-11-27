package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.exception.CaseAssociationError;
import uk.gov.hmcts.reform.sscs.exception.CaseAssociationException;
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

    @Test
    public void willHandleCaseAssociationExceptionNotFound() {
        CaseAssociationException exception = new CaseAssociationException(CaseAssociationError.CASE_NOT_FOUND);
        ResponseEntity<Object> response = exceptionHandlerAdvice.handleCaseAssociationException(exception, webRequest);
        assertThat(response.getStatusCode().value(), equalTo(404));
        assertThat(response.getBody(), equalTo(Map.of(
            "error", "CASE_NOT_FOUND",
            "message", "No case found for the provided appeal reference number."
        )));
    }

    @Test
    public void willHandleCaseAssociationExceptionForbidden() {
        CaseAssociationException exception = new CaseAssociationException(CaseAssociationError.POSTCODE_MISMATCH);
        ResponseEntity<Object> response = exceptionHandlerAdvice.handleCaseAssociationException(exception, webRequest);
        assertThat(response.getStatusCode().value(), equalTo(403));
        assertThat(response.getBody(), equalTo(Map.of(
            "error", "POSTCODE_MISMATCH",
            "message", "Postcode or reference does not match the appeal records."
        )));
    }

    @Test
    public void willHandleCaseAssociationExceptionPostcodeOrReferenceNotConfigured() {
        CaseAssociationException exception = new CaseAssociationException(CaseAssociationError.POSTCODE_OR_REFERENCE_NOT_CONFIGURED);
        ResponseEntity<Object> response = exceptionHandlerAdvice.handleCaseAssociationException(exception, webRequest);
        assertThat(response.getStatusCode().value(), equalTo(403));
        assertThat(response.getBody(), equalTo(Map.of(
            "error", "POSTCODE_OR_REFERENCE_NOT_CONFIGURED",
            "message", "Appeal does not contain valid reference or address details to verify."
        )));
    }

    @Test
    public void willHandleCaseAssociationExceptionSubscriptionMismatch() {
        CaseAssociationException exception = new CaseAssociationException(CaseAssociationError.SUBSCRIPTION_MISMATCH);
        ResponseEntity<Object> response = exceptionHandlerAdvice.handleCaseAssociationException(exception, webRequest);
        assertThat(response.getStatusCode().value(), equalTo(403));
        assertThat(response.getBody(), equalTo(Map.of(
            "error", "SUBSCRIPTION_MISMATCH",
            "message", "The email address or appeal details do not match the registered subscription."
        )));
    }
}
