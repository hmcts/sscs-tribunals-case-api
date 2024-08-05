package uk.gov.hmcts.reform.sscs.tyanotifications.domain.idam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Authorize {
    private String defaultUrl;
    private String code;
    private String accessToken;

    public Authorize(@JsonProperty("default-url") String defaultUrl,
                     @JsonProperty("code") String code,
                     @JsonProperty("access_token") String accessToken) {
        this.defaultUrl = defaultUrl;
        this.code = code;
        this.accessToken = accessToken;
    }
}
