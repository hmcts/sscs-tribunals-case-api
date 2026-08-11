package uk.gov.hmcts.reform.sscs.domain.email;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RoboticsEmailTemplate {
    private final String from;
    private final String to;
    private final String message;
    private final String scottishTo;
    private final String pipAeTo;

    public RoboticsEmailTemplate(
        @Value("${robotics.email.from}") String from,
        @Value("${robotics.email.to}") String to,
        @Value("${robotics.email.scottishTo}") String scottishTo,
        @Value("${robotics.email.pipAeTo}") String pipAeTo,
        @Value("${robotics.email.message}") String message
    ) {
        this.from = from;
        this.to = to;
        this.scottishTo = scottishTo;
        this.pipAeTo = pipAeTo;
        this.message = message;
    }

    public Email generateEmail(String subject,
                               List<EmailAttachment> attachments,
                               boolean isScottish,
                               boolean isPipAeTo) {
        return new Email(
            from,
            isScottish ? scottishTo : isPipAeTo ? pipAeTo : to,
            subject,
            message,
            attachments
        );
    }
}
