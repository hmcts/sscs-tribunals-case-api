package uk.gov.hmcts.reform.sscs.bulkscan.validators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@RunWith(JUnitParamsRunner.class)
public class PostcodeValidatorTest {
    private static final String URL = "https://api.postcodes.io/postcodes/{postcode}/validate";
    private static final String TEST_POSTCODES = "TS2 2ST, TS1 1ST";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private RestTemplate restTemplate;
    @Mock private ResponseEntity<byte[]> responseEntity;

    private PostcodeValidator postcodeValidator;

    @Before
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
    public void shouldReturnTrueForAValidPostCode() {
        setUpSuccessResponse();
        boolean valid = postcodeValidator.isValid("w11 1AA");
        assertTrue(valid);
    }

    @Test
    @Parameters({"W1 1aa", "70002"})
    public void shouldReturnFalseForAnInValidPostCode(String postcode) {
        setUpFailureResponse();
        assertFalse(postcodeValidator.isValid(postcode));
    }

    @Test
    public void shouldReturnTrueWhenNotEnabled() {
        PostcodeValidator postcodeValidator = new PostcodeValidator(URL, false, TEST_POSTCODES, restTemplate);
        assertTrue(postcodeValidator.isValid("W11 1AA"));
    }

    @Test
    @Parameters({"TS1 1ST", "TS2 2ST"})
    public void shouldReturnTrueForTheTestPostCode(String postcode) {
        boolean valid = postcodeValidator.isValid(postcode);
        assertTrue(valid);
    }

    @Test
    public void shouldHandleRestClientResponseException() {
        when(restTemplate
            .exchange(
                any(String.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class),
                any(String.class)
            )
        ).thenThrow(new RestClientResponseException("error", 404, "error", null, null, null));
        assertFalse(postcodeValidator.isValid("70002"));
    }

    @Test
    public void shouldHandleNon200Exception() {
        setupRestTemplateResponse();
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        assertFalse(postcodeValidator.isValid("80202"));
    }
}
