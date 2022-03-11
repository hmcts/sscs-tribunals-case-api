package uk.gov.hmcts.reform.sscs.servicebus.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;

import java.util.function.Supplier;


@Configuration
@Slf4j
//@ConditionalOnProperty //feature flag---------------------------------------
public class HearingEventProducerConfiguration {

    @Bean
    public Sinks.Many<Message<HearingRequest>> hearingsEventSink() {
        return Sinks.many().unicast().onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<Message<HearingRequest>>> hearingEventsSinkSupply(Sinks.Many<Message<HearingRequest>>
                                                                                   hearingsEventSink) {
        return () -> hearingsEventSink.asFlux()
            .doOnNext(message -> log.info("Attempting to publish message {}", message))
            .onErrorResume(error -> Flux.error(new IllegalArgumentException("oops", error)));
    }
}
