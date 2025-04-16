package uk.gov.hmcts.reform.sscs.bulkscan.exceptionhandlers;

import static java.util.Collections.emptyList;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.ResponseEntity.status;

import feign.FeignException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ErrorResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.InvalidExceptionRecordException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.UnauthorizedException;
import uk.gov.hmcts.reform.sscs.domain.CaseResponse;

@ControllerAdvice(basePackages = "uk.gov.hmcts.reform.sscs.bulkscan")
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseExceptionHandler.class);

    @ExceptionHandler(InvalidTokenException.class)
    protected ResponseEntity<ApiError> handleInvalidTokenException(InvalidTokenException exc) {
        log.warn(exc.getMessage(), exc);
        return status(UNAUTHORIZED).body(new ApiError(exc.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    protected ResponseEntity<ApiError> handleUnAuthorizedException(UnauthorizedException exception) {
        log.error(exception.getMessage(), exception);

        return status(UNAUTHORIZED).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    protected ResponseEntity<ApiError> handleForbiddenException(ForbiddenException exception) {
        log.error(exception.getMessage(), exception);

        return status(FORBIDDEN).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(FeignException.class)
    protected ResponseEntity<String> handleFeignException(FeignException exception) {
        log.error(exception.getMessage(), exception);

        return status(exception.status()).body(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<CaseResponse> handleInternalException(Exception exception) {
        log.error(exception.getMessage(), exception);

        List<String> errors = new ArrayList<>();

        errors.add("There was an unknown error when processing the case. If the error persists, please contact the Bulk Scan development team");

        return ResponseEntity.ok(CaseResponse.builder().errors(errors).build());
    }

    @ExceptionHandler(InvalidExceptionRecordException.class)
    protected ResponseEntity<ErrorResponse> handleInvalidExceptionRecord(InvalidExceptionRecordException exc) {
        return status(UNPROCESSABLE_ENTITY).body(new ErrorResponse(exc.getErrors(), emptyList()));
    }
}
