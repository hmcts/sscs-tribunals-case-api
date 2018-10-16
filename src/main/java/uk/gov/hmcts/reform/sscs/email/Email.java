package uk.gov.hmcts.reform.sscs.email;

import java.util.List;
import lombok.Value;

@Value
public class Email {

    private final String from;
    private final String to;
    private final String subject;
    private final String message;

    private final List<EmailAttachment> attachments;

    public Email(String from, String to, String subject, String message, List<EmailAttachment> attachments) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.message = message;
        this.attachments = attachments;
    }

    public boolean hasAttachments() {
        return this.attachments != null && !this.attachments.isEmpty();
    }
}
