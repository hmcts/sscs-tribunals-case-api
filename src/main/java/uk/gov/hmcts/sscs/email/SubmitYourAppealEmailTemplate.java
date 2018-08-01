package uk.gov.hmcts.sscs.email;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SubmitYourAppealEmailTemplate {
    private final String from;
    private final String to;
    private final String message;

    public SubmitYourAppealEmailTemplate(@Value("${appeal.email.from}") String from,
                                         @Value("${appeal.email.to}") String to,
                                         @Value("${appeal.email.message}") String message) {
        this.from = from;
        this.to = to;
        this.message = message;
    }

    public Email generateEmail(String subject, List<EmailAttachment> attachments) {
        return new Email(
                from,
                to,
                subject,
                message,
                attachments
        );
    }
}
