package uk.gov.hmcts.reform.sscs.thirdparty.documentmanagement;

public class IllegalFileTypeException extends RuntimeException {
    public IllegalFileTypeException(String fileName) {
        super("File [" + fileName + "] cannot be uploaded to document store.");
    }
}
