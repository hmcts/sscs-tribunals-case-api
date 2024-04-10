package uk.gov.hmcts.reform.sscs.domain.email;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.FeatureToggleService;


@Component
public class RequestTranslationTemplate {
    private final FeatureToggleService featureToggleService;
    private final String from;
    private final String fromSendGrid;
    private final String to;
    private final String message;
    private final String subject;

    public RequestTranslationTemplate(
        @Autowired FeatureToggleService featureToggleService,
        @Value("${wlu.email.from}") String from,
        @Value("${wlu.email.from-send-grid}") String fromSendGrid,
        @Value("${wlu.email.to}") String to,
        @Value("${wlu.email.subject}") String subject,
        @Value("${wlu.email.message}") String message
    ) {
        this.featureToggleService = featureToggleService;
        this.from = from;
        this.fromSendGrid = fromSendGrid;
        this.to = to;
        this.subject = subject;
        this.message = message;
    }

    public Email generateEmail(List<EmailAttachment> attachments, long caseId) {
        return new Email(
                featureToggleService.isSendGridEnabled() ? fromSendGrid : from,
                to,
                subject + " (" + caseId + ")",
                message,
                attachments
        );
    }
}
