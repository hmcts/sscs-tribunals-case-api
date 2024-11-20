package uk.gov.hmcts.reform.sscs.healthcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class DocmosisHealthIndicatorTest {

    private static final String DOCMOSIS_STATUS_URI = "http://docmosis/rs/render";

    @Mock private RestTemplate restTemplate;
    @Mock private ResponseEntity responseEntity;

    private DocmosisHealthIndicator docmosisHealthIndicator;

    @BeforeEach
    public void setUp() {

        docmosisHealthIndicator =
            new DocmosisHealthIndicator(
                DOCMOSIS_STATUS_URI,
                restTemplate
            );

        doReturn(responseEntity)
            .when(restTemplate)
            .exchange(
                eq(DOCMOSIS_STATUS_URI),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)
            );
    }

    @Test
    public void should_call_docmosis_and_report_when_up() {

        Map<String, Object> exampleReadyResponse =
            ImmutableMap
                .of(
                    "ready", true
                );

        when(responseEntity.getBody()).thenReturn(exampleReadyResponse);

        assertEquals(Health.up().build(), docmosisHealthIndicator.health());
    }

    @Test
    public void should_call_docmosis_and_report_when_down() {

        Map<String, Object> exampleNotReadyResponse =
            ImmutableMap
                .of(
                    "ready", false
                );

        when(responseEntity.getBody()).thenReturn(exampleNotReadyResponse);

        assertEquals(Health.down().build(), docmosisHealthIndicator.health());
    }

    @Test
    public void should_report_as_down_if_ready_indicator_not_in_payload() {

        Map<String, Object> exampleUnexpectedResponse =
            ImmutableMap
                .of(
                    "foo", "bar"
                );

        when(responseEntity.getBody()).thenReturn(exampleUnexpectedResponse);

        assertEquals(Health.down().build(), docmosisHealthIndicator.health());
    }

    @Test
    public void should_report_as_down_if_no_data_returned() {

        when(responseEntity.getBody()).thenReturn(null);

        assertEquals(Health.down().build(), docmosisHealthIndicator.health());
    }

    @Test
    public void should_report_as_down_if_http_call_fails() {

        RestClientException underlyingException = mock(RestClientException.class);

        when(restTemplate
            .exchange(
                eq(DOCMOSIS_STATUS_URI),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)
            )).thenThrow(underlyingException);

        assertEquals(Health.down(underlyingException).build(), docmosisHealthIndicator.health());
    }
}
