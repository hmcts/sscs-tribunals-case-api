package uk.gov.hmcts.sscs.model.idam;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;

@Value
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
