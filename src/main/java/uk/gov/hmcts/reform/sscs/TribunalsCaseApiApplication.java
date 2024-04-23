package uk.gov.hmcts.reform.sscs;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.validation.ValidatorFactory;
import okhttp3.OkHttpClient;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.service.ScheduledTaskRunner;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableFeignClients(basePackages = {
    "uk.gov.hmcts.reform.authorisation",
    "uk.gov.hmcts.reform.sscs.idam",
    "uk.gov.hmcts.reform.sscs.document",
    "uk.gov.hmcts.reform.docassembly",
    "uk.gov.hmcts.reform.sscs.thirdparty",
    "uk.gov.hmcts.reform.idam",
    "uk.gov.hmcts.reform.sscs.client",
    "uk.gov.hmcts.reform.sendletter",
    "uk.gov.hmcts.reform.ccd.client"
})

@ComponentScan(basePackages = {"uk.gov.hmcts.reform", "uk.gov.hmcts.reform.sscs",
    "uk.gov.hmcts.reform.ccd.document.am"})
@EnableScheduling
@EnableRetry
public class TribunalsCaseApiApplication implements CommandLineRunner {

    @Value("${appeal.email.host}")
    private String emailHost;

    @Value("${appeal.email.port}")
    private int emailPort;

    @Value("${appeal.email.smtp.tls.enabled}")
    private String smtpTlsEnabled;

    @Autowired
    private ScheduledTaskRunner taskRunner;

    public static void main(String[] args) {
        final var instance = SpringApplication.run(TribunalsCaseApiApplication.class, args);
        if (System.getenv("TASK_NAME") != null) {
            instance.close();
        }
    }

    @Override
    public void run(String... args) {
        if (System.getenv("TASK_NAME") != null) {
            taskRunner.run(System.getenv("TASK_NAME"));
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverter());
        messageConverters.add(new ResourceHttpMessageConverter(false));
        try {
            messageConverters.add(new SourceHttpMessageConverter<>());
        } catch (Error err) {
            // Ignore when no TransformerFactory implementation is available
        }

        messageConverters.add(new AllEncompassingFormHttpMessageConverter());
        messageConverters.add(new MappingJackson2HttpMessageConverter());

        DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
        uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);

        return new RestTemplateBuilder()
            .messageConverters(messageConverters)
            .uriTemplateHandler(uriFactory)
            .build();
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(emailHost);
        javaMailSender.setPort(emailPort);
        Properties properties = new Properties();
        properties.setProperty("mail.transport.protocol", "smtp");
        properties.setProperty("mail.smtp.starttls.enable", smtpTlsEnabled);
        properties.put("mail.smtp.ssl.trust", "*");
        javaMailSender.setJavaMailProperties(properties);
        return javaMailSender;
    }

    @Bean
    ValidatorFactory validator() {
        final LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
        localValidatorFactoryBean.setMessageInterpolator(new ParameterMessageInterpolator());
        return localValidatorFactoryBean;
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
    public CcdRequestDetails getRequestDetails(
        @Value("${core_case_data.jurisdictionId}") String coreCaseDataJurisdictionId,
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
