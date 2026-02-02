package uk.gov.hmcts.reform.sscs.exception;

public class TaskManagementException extends RuntimeException {

    public TaskManagementException(String message) {
        super(message);
    }

    public TaskManagementException(Throwable cause) {
        super(cause);
    }
}
