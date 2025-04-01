package uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OcrDataField {

    @JsonProperty
    private final String name;

    @JsonProperty
    private final String value;

    @JsonCreator
    public OcrDataField(
        @JsonProperty("name") String name,
        @JsonProperty("value") String value
    ) {
        this.name = name;
        this.value = value;
    }
}
