package uk.gov.hmcts.reform.sscs.domain.email;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.FeatureToggleService;

@Component
public class RoboticsEmailTemplate {
    private final FeatureToggleService featureToggleService;
    private final String from;
    private final String fromSendGrid;
    private final String to;
    private final String message;
    private final String scottishTo;
    private final String pipAeTo;

    public RoboticsEmailTemplate(
        @Autowired FeatureToggleService featureToggleService,
        @Value("${robotics.email.from}") String from,
        @Value("${robotics.email.from-send-grid}") String fromSendGrid,
        @Value("${robotics.email.to}") String to,
        @Value("${robotics.email.scottishTo}") String scottishTo,
        @Value("${robotics.email.pipAeTo}") String pipAeTo,
        @Value("${robotics.email.message}") String message
    ) {
        this.featureToggleService = featureToggleService;
        this.from = from;
        this.fromSendGrid = fromSendGrid;
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
            featureToggleService.isSendGridEnabled() ? fromSendGrid : from,
            isScottish ? scottishTo : isPipAeTo ? pipAeTo : to,
            subject,
            message,
            attachments
        );
    }
}
