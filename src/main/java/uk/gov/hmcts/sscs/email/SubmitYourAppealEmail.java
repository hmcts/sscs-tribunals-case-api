package uk.gov.hmcts.sscs.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SubmitYourAppealEmail extends Email {

    public SubmitYourAppealEmail(@Value("${appeal.email.from}") String from,
                                   @Value("${appeal.email.to}") String to,
                                   @Value("${appeal.email.subject}") String subject,
                                   @Value("${appeal.email.message}") String message) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.message = message;
    }
}
