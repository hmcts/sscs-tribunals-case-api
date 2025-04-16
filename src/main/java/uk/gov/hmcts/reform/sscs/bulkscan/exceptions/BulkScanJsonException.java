package uk.gov.hmcts.reform.sscs.bulkscan.exceptions;

import java.io.Serial;

public class BulkScanJsonException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1641102126427991237L;

    public BulkScanJsonException(Throwable throwable) {
        super(throwable);
    }
}
