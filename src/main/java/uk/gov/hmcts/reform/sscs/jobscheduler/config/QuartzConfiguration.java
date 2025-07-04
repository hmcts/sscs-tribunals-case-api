package uk.gov.hmcts.reform.sscs.jobscheduler.config;

import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.QuartzFailedJobRescheduler;

@Configuration
@ConfigurationProperties(prefix = "job.scheduler")
public class QuartzConfiguration {

    private final Map<String, String> quartzProperties = new HashMap<>();

    // this getter is needed by the framework
    public Map<String, String> getQuartzProperties() {
        return quartzProperties;
    }

    @Bean
    public JobFactory jobFactory(ApplicationContext context) {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(context);
        return jobFactory;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(JobFactory jobFactory) {

        Properties properties = new Properties();
        properties.putAll(quartzProperties);

        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setJobFactory(jobFactory);
        schedulerFactory.setQuartzProperties(properties);
        schedulerFactory.setAutoStartup(false);

        return schedulerFactory;
    }

    @Bean
    @Singleton
    @Primary
    public Scheduler scheduler(
        SchedulerFactoryBean factory,
        @Value("${job.scheduler.autoStart:true}") boolean autoStart,
        @Value("${job.scheduler.retryPolicy.maxNumberOfJobExecutions}") int maxJobExecutionAttempts,
        @Value("${job.scheduler.retryPolicy.delayBetweenAttemptsInMs}") long delayBetweenAttemptsInMs
    ) throws SchedulerException {

        Scheduler scheduler = factory.getScheduler();

        QuartzFailedJobRescheduler failedJobRescheduler = new QuartzFailedJobRescheduler(
            maxJobExecutionAttempts,
            Duration.ofMillis(delayBetweenAttemptsInMs)
        );

        scheduler.getListenerManager().addJobListener(failedJobRescheduler);

        if (autoStart) {
            scheduler.start();
        }

        return scheduler;
    }

}
