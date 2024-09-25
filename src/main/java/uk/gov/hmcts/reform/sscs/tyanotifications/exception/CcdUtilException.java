package uk.gov.hmcts.reform.sscs.tyanotifications.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class CcdUtilException extends RuntimeException {

    public CcdUtilException(Exception ex) {
        super(ex);
    }
}
