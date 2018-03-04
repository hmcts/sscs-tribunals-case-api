package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Hearing {
    private HearingDetails value;

    public HearingDetails getValue() {
        return value;
    }

    public void setValue(HearingDetails value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Hearing{" +
                "value=" + value +
                '}';
    }
}
