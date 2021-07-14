package uk.gov.hmcts.reform.sscs;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.validation.ValidatorFactory;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;

@SpringBootApplication
@EnableFeignClients(basePackages =
        {
                "uk.gov.hmcts.reform.authorisation",
                "uk.gov.hmcts.reform.sscs.idam",
                "uk.gov.hmcts.reform.sscs.document",
                "uk.gov.hmcts.reform.docassembly",
                "uk.gov.hmcts.reform.sscs.thirdparty",
                "uk.gov.hmcts.reform.idam"
        })
@ComponentScan(basePackages = {"uk.gov.hmcts.reform"}, excludeFilters = {@ComponentScan.Filter(type = FilterType.REGEX, pattern = "uk.gov.hmcts.reform.ccd.document.am.config.ClientConfiguration")})
@EnableScheduling
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

    @Bean
    ValidatorFactory validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public OkHttpClient okHttpClient() {
        int timeout = 10;
        return new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MINUTES)
                .readTimeout(timeout, TimeUnit.MINUTES)
                .retryOnConnectionFailure(true)
                .build();
    }

    @Bean
    public CcdRequestDetails getRequestDetails(@Value("${core_case_data.jurisdictionId}") String coreCaseDataJurisdictionId,
                                               @Value("${core_case_data.caseTypeId}") String coreCaseDataCaseTypeId) {
        return CcdRequestDetails.builder()
                .caseTypeId(coreCaseDataCaseTypeId)
                .jurisdictionId(coreCaseDataJurisdictionId)
                .build();
    }

    @Bean
    public DocmosisPdfGenerationService docmosisPdfGenerationService(
            @Value("${docmosis.uri}") String docmosisServiceEndpoint,
            @Value("${docmosis.accessKey}") String docmosisServiceAccessKey,
            RestTemplate restTemplate
    ) {
        return new DocmosisPdfGenerationService(docmosisServiceEndpoint, docmosisServiceAccessKey, restTemplate);
    }

}
