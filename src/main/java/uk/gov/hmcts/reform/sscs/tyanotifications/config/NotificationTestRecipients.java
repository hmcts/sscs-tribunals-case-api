package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gov.uk.notification.api.test")
@Getter
public class NotificationTestRecipients {
    private final List<String> emails = new ArrayList<>();

    private final List<String> sms = new ArrayList<>();

    private final List<String> postcodes = new ArrayList<>();
}
