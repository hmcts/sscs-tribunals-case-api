package uk.gov.hmcts.reform.sscs;

import static java.util.Arrays.asList;

import com.microsoft.applicationinsights.web.internal.ApplicationInsightsServletContextListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContextListener;
import javax.validation.ValidatorFactory;
import okhttp3.OkHttpClient;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
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
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.jobscheduler.config.QuartzConfiguration;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.JobClassMapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.JobClassMapping;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.JobMapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.JobMapping;
import uk.gov.hmcts.reform.sscs.service.ScheduledTaskRunner;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.RetryNotificationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.scheduler.*;
import uk.gov.service.notify.NotificationClient;

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
    "uk.gov.hmcts.reform.ccd.client",
    "uk.gov.hmcts.reform.sscs.tyanotifications.service.coh"
})

@ComponentScan(basePackages = {"uk.gov.hmcts.reform", "uk.gov.hmcts.reform.sscs",
    "uk.gov.hmcts.reform.ccd.document.am"})
@EnableScheduling
@EnableRetry
@EnableAsync
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


    @Value("${gov.uk.notification.api.key}")
    private String apiKey;

    @Value("${gov.uk.notification.api.testKey}")
    private String testApiKey;

    @Bean
    public ServletListenerRegistrationBean<ServletContextListener> appInsightsServletContextListenerRegistrationBean(
        ApplicationInsightsServletContextListener applicationInsightsServletContextListener) {
        ServletListenerRegistrationBean<ServletContextListener> srb =
            new ServletListenerRegistrationBean<>();
        srb.setListener(applicationInsightsServletContextListener);
        return srb;
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationInsightsServletContextListener applicationInsightsServletContextListener() {
        return new ApplicationInsightsServletContextListener();
    }


    @Bean
    @Primary
    public NotificationClient notificationClient() {
        return new NotificationClient(apiKey);
    }

    @Bean
    public NotificationClient testNotificationClient() {
        return new NotificationClient(testApiKey);
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource bean = new ReloadableResourceBundleMessageSource();
        bean.setBasename("classpath:application");
        bean.setDefaultEncoding("UTF-8");
        return bean;
    }

    @Bean
    @ConditionalOnProperty("spring.flyway.enabled")
    public JobFactory jobFactory(ApplicationContext context) {
        return (new QuartzConfiguration()).jobFactory(context);
    }

    @Bean
    public JobMapper getJobMapper(CcdActionDeserializer ccdActionDeserializer,
                                  NotificationService notificationService,
                                  RetryNotificationService retryNotificationService,
                                  CcdService ccdService,
                                  UpdateCcdCaseService updateCcdCaseService,
                                  IdamService idamService,
                                  SscsCaseCallbackDeserializer deserializer) {
        // Had to wire these up like this Spring will not wire up CcdActionExecutor otherwise.
        CcdActionExecutor ccdActionExecutor = new CcdActionExecutor(notificationService, retryNotificationService, ccdService, updateCcdCaseService, idamService, deserializer);
        return new JobMapper(asList(
            new JobMapping<>(payload -> !payload.contains("onlineHearingId"), ccdActionDeserializer, ccdActionExecutor)
        ));
    }

    @Bean
    public JobClassMapper getJobClassMapper(CohActionSerializer cohActionSerializer,
                                            CcdActionSerializer ccdActionSerializer) {
        return new JobClassMapper(asList(
            new JobClassMapping<>(CohJobPayload.class, cohActionSerializer),
            new JobClassMapping<>(String.class, ccdActionSerializer)
        ));
    }


}
