package uk.gov.hmcts.reform.sscs.bulkscan.exceptions;

public class BulkScanJsonException extends RuntimeException {
    private static final long serialVersionUID = 1641102126427991237L;

    public BulkScanJsonException(Throwable throwable) {
        super(throwable);
    }
}
