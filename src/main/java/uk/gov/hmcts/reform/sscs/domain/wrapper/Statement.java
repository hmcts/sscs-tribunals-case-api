package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Statement {
    @ApiModelProperty(example = "this is the text of a personal statement", required = true)
    @JsonProperty(value = "body")
    private String body;
    @JsonProperty(value = "tya")
    private String tya;

    // needed for Jackson
    private Statement() {
    }

    public Statement(String body, String tya) {
        this.body = body;
        this.tya = tya;
    }

    public String getBody() {
        return body;
    }

    public String getTya() {
        return tya;
    }
}
