package uk.gov.hmcts.reform.sscs.tyanotifications.domain;

import java.util.Optional;
import lombok.Data;

@Data
public class NotifyResponse {

    private String body;
    private String subject;
    private Optional<String> from;
    private String to;

    public NotifyResponse(String body, String subject, Optional<String> from, String to) {
        this.body = body;
        this.subject = subject;
        this.from = from;
        this.to = to;
    }
}
