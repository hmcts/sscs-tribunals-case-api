package uk.gov.hmcts.sscs;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class TribunalsCaseApiApplication {

    @Value("${appeal.email.host}")
    private String emailHost;

    @Value("${appeal.email.port}")
    private int emailPort;

    @Value("${appeal.email.smtp.tls.enabled}")
    private String smtpTlsEnabled;

    public static void main(String[] args) {
        SpringApplication.run(TribunalsCaseApiApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(emailHost);
        javaMailSender.setPort(emailPort);
        Properties properties = new Properties();
        properties.setProperty("mail.transport.protocol","smtp");
        properties.setProperty("mail.smtp.starttls.enable", smtpTlsEnabled);
        properties.put("mail.smtp.ssl.trust","*");
        javaMailSender.setJavaMailProperties(properties);
        return javaMailSender;
    }

}
