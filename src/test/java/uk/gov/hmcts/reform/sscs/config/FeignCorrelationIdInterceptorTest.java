package uk.gov.hmcts.reform.sscs.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.config.CorrelationIdFilter.CORRELATION_ID_HEADER;
import static uk.gov.hmcts.reform.sscs.config.CorrelationIdFilter.CORRELATION_ID_MDC_KEY;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class FeignCorrelationIdInterceptorTest {

    private RequestInterceptor interceptor;

    @Mock
    private RequestTemplate requestTemplate;

    @BeforeEach
    void setUp() {
        FeignCorrelationIdConfig config = new FeignCorrelationIdConfig();
        interceptor = config.correlationIdRequestInterceptor();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldPropagateCorrelationIdFromMdc() {
        String correlationId = "test-correlation-id-456";
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

        interceptor.apply(requestTemplate);

        verify(requestTemplate).header(CORRELATION_ID_HEADER, correlationId);
    }

    @Test
    void shouldNotSetHeaderWhenMdcIsEmpty() {
        MDC.remove(CORRELATION_ID_MDC_KEY);

        interceptor.apply(requestTemplate);

        verify(requestTemplate, never()).header(eq(CORRELATION_ID_HEADER), anyString());
    }

    @Test
    void shouldNotSetHeaderWhenMdcValueIsBlank() {
        MDC.put(CORRELATION_ID_MDC_KEY, "   ");

        interceptor.apply(requestTemplate);

        verify(requestTemplate, never()).header(eq(CORRELATION_ID_HEADER), anyString());
    }

    @Test
    void shouldPropagateUuidFormat() {
        String correlationId = "550e8400-e29b-41d4-a716-446655440000";
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

        interceptor.apply(requestTemplate);

        verify(requestTemplate).header(CORRELATION_ID_HEADER, correlationId);
    }
}
