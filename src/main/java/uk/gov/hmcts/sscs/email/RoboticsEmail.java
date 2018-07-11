package uk.gov.hmcts.sscs.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RoboticsEmail extends Email {

    public RoboticsEmail(@Value("${robotics.email.from}") String from,
                         @Value("${robotics.email.to}") String to,
                         @Value("${robotics.email.subject}") String subject,
                         @Value("${robotics.email.message}") String message) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.message = message;
    }
}
