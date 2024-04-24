package uk.gov.hmcts.reform.sscs.hearings.service.exceptions;

public class UpdateCcdCaseDetailsException extends Exception {

    private static final long serialVersionUID = -315707861582772008L;
    private final Exception e;

    public UpdateCcdCaseDetailsException(String message, Exception e) {
        super(message);
        this.e = e;
    }


    public Exception getException() {
        return e;
    }
}
