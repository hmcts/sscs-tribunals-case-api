package uk.gov.hmcts.reform.sscs.evidenceshare.config;

import java.util.Properties;
import javax.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties(prefix = "send-grid")
public class SendGridSmtpConfig {

    @NotEmpty
    private String host;
    private int port;
    @NotEmpty
    private String apiKey;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Bean("sendGridMailSender")
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(host);
        javaMailSender.setPort(port);
        javaMailSender.setUsername("apikey");
        javaMailSender.setPassword(apiKey);

        Properties properties = new Properties();
        properties.setProperty("mail.transport.protocol", "smtp");

        javaMailSender.setJavaMailProperties(properties);
        return javaMailSender;
    }

}
