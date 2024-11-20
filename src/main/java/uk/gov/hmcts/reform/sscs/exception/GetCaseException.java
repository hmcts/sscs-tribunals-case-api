package uk.gov.hmcts.reform.sscs.exception;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.io.Serial;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(NOT_FOUND)
public class GetCaseException extends CaseException  {
    @Serial
    private static final long serialVersionUID = -7206725950985350023L;

    public GetCaseException(String message) {
        super(message);
    }
}
