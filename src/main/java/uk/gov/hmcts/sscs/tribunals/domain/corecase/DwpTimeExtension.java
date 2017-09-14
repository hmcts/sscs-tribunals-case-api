package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public class DwpTimeExtension {

    private String requested;

    private String granted;


    public DwpTimeExtension() {
    }

    public String getRequested() {
        return requested;
    }

    public void setRequested(String requested) {
        this.requested = requested;
    }

    public String getGranted() {
        return granted;
    }

    public void setGranted(String granted) {
        this.granted = granted;
    }

    @Override
    public String toString() {
        return "DwpTimeExtension{"
                +    "requested='" + requested + '\''
                +    ", granted='" + granted + '\''
                +    '}';
    }
}
