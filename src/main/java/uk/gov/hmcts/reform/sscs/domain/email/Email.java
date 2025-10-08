package uk.gov.hmcts.reform.sscs.domain.email;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Email {

    private final String from;
    private final String to;
    private final String subject;
    private final String message;
    private final List<EmailAttachment> attachments;

    public boolean hasAttachments() {
        return this.attachments != null && !this.attachments.isEmpty();
    }
}
