package uk.gov.hmcts.reform.sscs.evidenceshare.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class DwpAddressLookupException extends RuntimeException {
    public static final long serialVersionUID = -7268250396297541580L;

    public DwpAddressLookupException(String message) {
        super(message);
    }
}
