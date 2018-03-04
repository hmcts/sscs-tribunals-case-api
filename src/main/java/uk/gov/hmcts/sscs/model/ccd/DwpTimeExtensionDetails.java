package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DwpTimeExtensionDetails {
    private String requested;
    private String granted;

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
        return "DwpTimeExtensionDetails{" +
                "requested='" + requested + '\'' +
                ", granted='" + granted + '\'' +
                '}';
    }
}
