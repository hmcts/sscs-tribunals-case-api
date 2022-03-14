package uk.gov.hmcts.reform.sscs.servicebus.producer;

import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import uk.gov.hmcts.reform.sscs.model.hearings.SessionAwareRequest;

@Configuration
@Slf4j
@ConditionalOnProperty("feature.gaps-switchover.enabled")
public class HearingEventConfiguration {

    @Bean
    public Sinks.Many<Message<SessionAwareRequest>> hearingsEventSink() {
        return Sinks.many().unicast().onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<Message<SessionAwareRequest>>> hearingEventsSupply(Sinks.Many<Message<SessionAwareRequest>>
                                                                                   hearingsEventSink) {
        return () -> hearingsEventSink.asFlux()
            .doOnNext(message -> log.info("Attempting to publish message {}", message))
            .doOnError(throwable -> log.error("An error occurred during hearing events processing", throwable));
    }
}
