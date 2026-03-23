package uk.gov.hmcts.reform.sscs.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.sscs.config.CorrelationIdFilter.CORRELATION_ID_HEADER;
import static uk.gov.hmcts.reform.sscs.config.CorrelationIdFilter.CORRELATION_ID_MDC_KEY;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Collection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import uk.gov.hmcts.reform.sscs.service.AppInsightsService;

class FeignClientConfigTest {

    private FeignClientConfig config;
    private RequestInterceptor interceptor;

    @BeforeEach
    void setUp() {
        config = new FeignClientConfig(mock(AppInsightsService.class));
        interceptor = config.correlationIdInterceptor();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldPropagateCorrelationIdFromMdcToOutboundRequest() {
        String correlationId = "test-id-456";
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        Collection<String> headers = template.headers().get(CORRELATION_ID_HEADER);
        assertThat(headers).containsExactly(correlationId);
    }

    @Test
    void shouldNotAddHeaderWhenMdcHasNoCorrelationId() {
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey(CORRELATION_ID_HEADER);
    }
}
