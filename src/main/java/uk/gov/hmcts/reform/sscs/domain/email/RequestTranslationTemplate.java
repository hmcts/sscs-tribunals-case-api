package uk.gov.hmcts.reform.sscs.domain.email;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class RequestTranslationTemplate {
    private final String from;
    private final String to;
    private final String message;
    private final String subject;

    public RequestTranslationTemplate(
        @Value("${wlu.email.from}") String from,
        @Value("${wlu.email.to}") String to,
        @Value("${wlu.email.subject}") String subject,
        @Value("${wlu.email.message}") String message
    ) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.message = message;
    }

    public Email generateEmail(List<EmailAttachment> attachments, long caseId) {
        return new Email(
                from,
                to,
                subject + " (" + caseId + ")",
                message,
                attachments
        );
    }
}
