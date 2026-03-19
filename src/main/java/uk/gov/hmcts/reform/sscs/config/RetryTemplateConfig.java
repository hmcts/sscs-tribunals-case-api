package uk.gov.hmcts.reform.sscs.config;

import static uk.gov.hmcts.reform.sscs.config.MetricsConstants.RETRY_ATTEMPTS;
import static uk.gov.hmcts.reform.sscs.config.MetricsConstants.TAG_OPERATION;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryTemplateConfig {

    @Bean
    public RetryTemplate retryTemplate(RetryListener metricsRetryListener) {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(2000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        retryTemplate.registerListener(metricsRetryListener);

        return retryTemplate;
    }

    @Bean
    public RetryListener metricsRetryListener(MeterRegistry meterRegistry) {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(
                    RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                String operationName = context.getAttribute("context.name") != null
                    ? context.getAttribute("context.name").toString() : "unknown";
                meterRegistry.counter(RETRY_ATTEMPTS, TAG_OPERATION, operationName).increment();
            }
        };
    }
}
