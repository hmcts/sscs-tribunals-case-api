package uk.gov.hmcts.reform.sscs.config;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.config.CorrelationIdFilter.CORRELATION_ID_HEADER;
import static uk.gov.hmcts.reform.sscs.config.CorrelationIdFilter.CORRELATION_ID_MDC_KEY;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterConcurrencyTest {

    private CorrelationIdFilter correlationIdFilter;

    @BeforeEach
    void setUp() {
        correlationIdFilter = new CorrelationIdFilter();
    }

    @Test
    @DisplayName("Should maintain MDC isolation across concurrent threads")
    void shouldMaintainMdcIsolationAcrossConcurrentThreads() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ConcurrentHashMap<String, String> observedCorrelationIds = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> mdcAfterFilter = new ConcurrentHashMap<>();

        for (int i = 0; i < threadCount; i++) {
            final String correlationId = "thread-corr-id-" + i;
            final String threadName = "test-thread-" + i;

            executor.submit(() -> {
                try {
                    startLatch.await(); // all threads start simultaneously

                    MockHttpServletRequest request = new MockHttpServletRequest();
                    request.addHeader(CORRELATION_ID_HEADER, correlationId);
                    MockHttpServletResponse response = new MockHttpServletResponse();

                    correlationIdFilter.doFilterInternal(request, response, (req, res) -> {
                        // Record the MDC value this thread sees during filter execution
                        String mdcValue = MDC.get(CORRELATION_ID_MDC_KEY);
                        observedCorrelationIds.put(threadName, mdcValue);
                        // Add a small busy loop to increase chance of thread interleaving
                        for (int j = 0; j < 1000; j++) {
                            Thread.yield();
                        }
                    });

                    // Record MDC value after filter completes
                    String mdcValueAfter = MDC.get(CORRELATION_ID_MDC_KEY);
                    mdcAfterFilter.put(threadName, mdcValueAfter == null ? "null" : mdcValueAfter);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // release all threads
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // Each thread should see its own correlation ID, not another thread's
        for (int i = 0; i < threadCount; i++) {
            String threadName = "test-thread-" + i;
            String expectedId = "thread-corr-id-" + i;
            assertThat(observedCorrelationIds.get(threadName))
                    .as("Thread %s should see its own correlation ID", threadName)
                    .isEqualTo(expectedId);
        }

        // MDC should be cleared after filter execution for all threads
        for (int i = 0; i < threadCount; i++) {
            String threadName = "test-thread-" + i;
            assertThat(mdcAfterFilter.get(threadName))
                    .as("Thread %s should have MDC cleared after filter", threadName)
                    .isEqualTo("null");
        }
    }

    @Test
    @DisplayName("Should not leak MDC between sequential requests on same thread")
    void shouldNotLeakMdcBetweenSequentialRequests() throws ServletException, IOException {
        String firstCorrelationId = "first-request-id";

        // First request
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader(CORRELATION_ID_HEADER, firstCorrelationId);
        MockHttpServletResponse response1 = new MockHttpServletResponse();

        final String[] mdcDuringFirst = {null};
        correlationIdFilter.doFilterInternal(request1, response1, (req, res) -> {
            mdcDuringFirst[0] = MDC.get(CORRELATION_ID_MDC_KEY);
        });

        // MDC should be cleared between requests
        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();

        // Second request
        String secondCorrelationId = "second-request-id";
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.addHeader(CORRELATION_ID_HEADER, secondCorrelationId);
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        final String[] mdcDuringSecond = {null};
        correlationIdFilter.doFilterInternal(request2, response2, (req, res) -> {
            mdcDuringSecond[0] = MDC.get(CORRELATION_ID_MDC_KEY);
        });

        assertThat(mdcDuringFirst[0]).isEqualTo(firstCorrelationId);
        assertThat(mdcDuringSecond[0]).isEqualTo(secondCorrelationId);
        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should generate unique correlation IDs when no header provided across multiple requests")
    void shouldGenerateUniqueIdsForMultipleRequests() throws ServletException, IOException {
        final String[] generatedIds = new String[5];

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            final int index = i;

            correlationIdFilter.doFilterInternal(request, response, (req, res) -> {
                generatedIds[index] = MDC.get(CORRELATION_ID_MDC_KEY);
            });
        }

        // All generated IDs should be unique
        assertThat(generatedIds).doesNotHaveDuplicates();
        // All should be non-null
        for (String id : generatedIds) {
            assertThat(id).isNotNull().isNotBlank();
        }
    }
}
