package uk.gov.hmcts.reform.sscs.tyanotifications.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MacException extends RuntimeException {

    public MacException(Exception ex) {
        super(ex);
    }
}
