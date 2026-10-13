package uk.gov.hmcts.reform.sscs.bulkscan.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getMaskedPostcode;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;

@ExtendWith(MockitoExtension.class)
public class PostcodeValidatorTest {
    private static final String URL = "https://api.postcodes.io/postcodes/{postcode}/validate";
    private static final String TEST_POSTCODES = "TS2 2ST, TS1 1ST";

    @Mock
    private RestTemplate restTemplate;
    @Mock private ResponseEntity<byte[]> responseEntity;

    @RegisterExtension
    private final LogCaptureExtension logCapture =
            new LogCaptureExtension(PostcodeValidator.class);

    private PostcodeValidator postcodeValidator;

    @BeforeEach
    public void setup() {
        postcodeValidator = new PostcodeValidator(URL, true, TEST_POSTCODES, restTemplate);
    }

    private void setupRestTemplateResponse() {
        when(restTemplate
            .exchange(
                any(String.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class),
                any(String.class)
            )
        ).thenReturn(responseEntity);
    }

    private void setUpSuccessResponse() {
        setupRestTemplateResponse();
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        when(responseEntity.getBody()).thenReturn("true".getBytes());
    }

    private void setUpFailureResponse() {
        setupRestTemplateResponse();
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        when(responseEntity.getBody()).thenReturn("unknown".getBytes());
    }

    @Test
    void shouldReturnTrueForAValidPostCode() {
        setUpSuccessResponse();
        assertThat(postcodeValidator.isValid("w11 1AA")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"W1 1aa", "70002"})
    void shouldReturnFalseForAnInValidPostCode(String postcode) {
        setUpFailureResponse();
        assertThat(postcodeValidator.isValid(postcode)).isFalse();
    }

    @Test
    void shouldReturnTrueWhenNotEnabled() {
        PostcodeValidator postcodeValidator = new PostcodeValidator(URL, false, TEST_POSTCODES, restTemplate);
        assertThat(postcodeValidator.isValid("W11 1AA")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TS1 1ST", "TS2 2ST"})
    void shouldReturnTrueForTheTestPostCode(String postcode) {
        assertThat(postcodeValidator.isValid(postcode)).isTrue();
    }

    @Test
    void shouldHandleRestClientResponseException() {
        when(restTemplate
            .exchange(
                any(String.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class),
                any(String.class)
            )
        ).thenThrow(new RestClientResponseException("error", 404, "error", null, null, null));
        assertThat(postcodeValidator.isValid("70002")).isFalse();
    }

    @Test
    void shouldHandleNon200Exception() {
        setupRestTemplateResponse();
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        assertThat(postcodeValidator.isValid("80202")).isFalse();
    }

    @Test
    void shouldMaskPostcodeInLogsWhenTestPostcodeReceived() {
        postcodeValidator.isValid("TS1 1ST");

        logCapture
                .assertLogContains(getMaskedPostcode("TS1 1ST"), Level.INFO)
                .assertLogDoesNotContain("TS1 1ST", Level.INFO);
    }

    @Test
    void shouldMaskPostcodeInLogsWhenNon200ResponseReceived() {
        setupRestTemplateResponse();
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

        postcodeValidator.isValid("80202");

        logCapture
                .assertLogContains(getMaskedPostcode("80202"), Level.INFO)
                .assertLogDoesNotContain("80202", Level.INFO);
    }

    @Test
    void shouldMaskPostcodeInLogsWhenRestClientResponseExceptionThrown() {
        when(restTemplate
            .exchange(
                any(String.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class),
                any(String.class)
            )
        ).thenThrow(new RestClientResponseException("error", 404, "error", null, null, null));

        postcodeValidator.isValid("70002");

        logCapture
                .assertLogContains(getMaskedPostcode("70002"), Level.INFO)
                .assertLogDoesNotContain("70002", Level.INFO);
    }
}
