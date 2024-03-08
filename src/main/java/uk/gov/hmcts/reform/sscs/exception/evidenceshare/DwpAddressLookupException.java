package uk.gov.hmcts.reform.sscs.exception.evidenceshare;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class DwpAddressLookupException extends RuntimeException {
    public static final long serialVersionUID = -7268250396297541580L;

    public DwpAddressLookupException(String message) {
        super(message);
    }
}
