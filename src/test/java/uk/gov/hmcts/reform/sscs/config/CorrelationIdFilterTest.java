package uk.gov.hmcts.reform.sscs.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.sscs.config.CorrelationIdFilter.CORRELATION_ID_HEADER;
import static uk.gov.hmcts.reform.sscs.config.CorrelationIdFilter.CORRELATION_ID_MDC_KEY;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldGenerateUuidWhenNoHeaderPresent() throws Exception {
        AtomicReference<String> capturedId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedId.set(MDC.get(CORRELATION_ID_MDC_KEY));

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedId.get()).isNotNull().isNotEmpty();
        assertThat(response.getHeader(CORRELATION_ID_HEADER)).isEqualTo(capturedId.get());
    }

    @Test
    void shouldPreserveExistingHeaderValue() throws Exception {
        String existingId = "test-correlation-id-123";
        request.addHeader(CORRELATION_ID_HEADER, existingId);
        AtomicReference<String> capturedId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedId.set(MDC.get(CORRELATION_ID_MDC_KEY));

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedId.get()).isEqualTo(existingId);
        assertThat(response.getHeader(CORRELATION_ID_HEADER)).isEqualTo(existingId);
    }

    @Test
    void shouldSetMdcValueDuringRequestProcessing() throws Exception {
        AtomicReference<String> capturedId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedId.set(MDC.get(CORRELATION_ID_MDC_KEY));

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedId.get()).isNotNull();
    }

    @Test
    void shouldClearMdcValueAfterRequestCompletes() throws Exception {
        FilterChain chain = (req, res) -> { };

        filter.doFilterInternal(request, response, chain);

        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldClearMdcValueEvenOnException() throws Exception {
        FilterChain chain = (req, res) -> {
            throw new ServletException("test exception");
        };

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
            .isInstanceOf(ServletException.class);

        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldAddCorrelationIdToResponseHeaders() throws Exception {
        FilterChain chain = (req, res) -> { };

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader(CORRELATION_ID_HEADER)).isNotNull().isNotEmpty();
    }

    @Test
    void shouldGenerateUuidWhenHeaderIsBlank() throws Exception {
        request.addHeader(CORRELATION_ID_HEADER, "   ");
        AtomicReference<String> capturedId = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedId.set(MDC.get(CORRELATION_ID_MDC_KEY));

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedId.get()).isNotNull().isNotEqualTo("   ");
    }
}
