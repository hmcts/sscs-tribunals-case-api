package uk.gov.hmcts.sscs.email;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RoboticsEmailTemplate {
    private final String from;
    private final String to;
    private final String message;

    public RoboticsEmailTemplate(@Value("${robotics.email.from}") String from,
                                 @Value("${robotics.email.to}") String to,
                                 @Value("${robotics.email.message}") String message) {
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
