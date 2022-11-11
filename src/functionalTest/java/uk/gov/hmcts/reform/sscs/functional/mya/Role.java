package uk.gov.hmcts.reform.sscs.functional.mya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Role {
    private String code;

    public Role(String code) {
        this.code = code;
    }

    @JsonProperty(value = "code")
    public String getCode() {
        return code;
    }
}
