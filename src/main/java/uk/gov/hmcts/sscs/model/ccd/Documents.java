package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Documents {
    private Doc value;

    public Doc getValue() {
        return value;
    }

    public void setValue(Doc value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Documents{" +
                "value=" + value +
                '}';
    }
}
