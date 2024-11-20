package uk.gov.hmcts.reform.sscs.exception;

import java.io.Serial;

public class UpdateCaseException extends CaseException {

    @Serial
    private static final long serialVersionUID = -315707861582772008L;

    public UpdateCaseException(String message) {
        super(message);
    }
}
