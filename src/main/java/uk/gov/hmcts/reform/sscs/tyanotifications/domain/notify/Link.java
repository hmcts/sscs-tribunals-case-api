package uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Link {

    private final String linkUrl;

    public String replace(String key, String value) {
        return linkUrl.replace(key, value);
    }
}
