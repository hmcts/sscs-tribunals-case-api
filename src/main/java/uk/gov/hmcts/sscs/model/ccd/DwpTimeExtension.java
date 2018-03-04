package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DwpTimeExtension {
    private DwpTimeExtensionDetails value;

    public DwpTimeExtensionDetails getValue() {
        return value;
    }

    public void setValue(DwpTimeExtensionDetails value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "DwpTimeExtension{" +
                "value=" + value +
                '}';
    }
}
