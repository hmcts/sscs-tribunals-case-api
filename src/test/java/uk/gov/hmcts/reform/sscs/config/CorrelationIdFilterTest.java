package uk.gov.hmcts.reform.sscs.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.reform.sscs.config.CorrelationIdFilter.CORRELATION_ID_HEADER;
import static uk.gov.hmcts.reform.sscs.config.CorrelationIdFilter.CORRELATION_ID_MDC_KEY;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    private CorrelationIdFilter correlationIdFilter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        correlationIdFilter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldGenerateUuidWhenNoHeaderPresent() throws ServletException, IOException {
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        String responseHeader = response.getHeader(CORRELATION_ID_HEADER);
        assertNotNull(responseHeader);
        UUID.fromString(responseHeader); // validates it is a valid UUID
    }

    @Test
    void shouldGenerateUuidWhenHeaderIsBlank() throws ServletException, IOException {
        request.addHeader(CORRELATION_ID_HEADER, "   ");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        String responseHeader = response.getHeader(CORRELATION_ID_HEADER);
        assertNotNull(responseHeader);
        UUID.fromString(responseHeader);
    }

    @Test
    void shouldPreserveExistingHeaderValue() throws ServletException, IOException {
        String existingId = "existing-correlation-id-123";
        request.addHeader(CORRELATION_ID_HEADER, existingId);

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        assertEquals(existingId, response.getHeader(CORRELATION_ID_HEADER));
    }

    @Test
    void shouldSetMdcDuringFilterExecution() throws ServletException, IOException {
        String existingId = "test-mdc-correlation-id";
        request.addHeader(CORRELATION_ID_HEADER, existingId);

        final String[] mdcValueDuringFilter = {null};

        correlationIdFilter.doFilterInternal(request, response, (req, res) -> {
            mdcValueDuringFilter[0] = MDC.get(CORRELATION_ID_MDC_KEY);
        });

        assertEquals(existingId, mdcValueDuringFilter[0]);
    }

    @Test
    void shouldClearMdcAfterFilterExecution() throws ServletException, IOException {
        request.addHeader(CORRELATION_ID_HEADER, "some-id");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        assertNull(MDC.get(CORRELATION_ID_MDC_KEY));
    }

    @Test
    void shouldClearMdcEvenWhenExceptionOccurs() throws ServletException, IOException {
        request.addHeader(CORRELATION_ID_HEADER, "some-id");

        try {
            correlationIdFilter.doFilterInternal(request, response, (req, res) -> {
                throw new ServletException("test exception");
            });
        } catch (ServletException e) {
            // expected
        }

        assertNull(MDC.get(CORRELATION_ID_MDC_KEY));
    }

    @Test
    void shouldSetResponseHeader() throws ServletException, IOException {
        String existingId = "response-header-test";
        request.addHeader(CORRELATION_ID_HEADER, existingId);

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        assertEquals(existingId, response.getHeader(CORRELATION_ID_HEADER));
    }
}
