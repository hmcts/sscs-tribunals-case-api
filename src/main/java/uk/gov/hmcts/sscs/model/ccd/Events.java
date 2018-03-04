package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Events {
    Event value;

    public Event getValue() {
        return value;
    }

    public void setValue(Event value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Events{" +
                "value=" + value +
                '}';
    }
}
