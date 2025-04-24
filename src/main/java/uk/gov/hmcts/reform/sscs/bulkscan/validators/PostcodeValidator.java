package uk.gov.hmcts.reform.sscs.bulkscan.validators;

import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.split;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class PostcodeValidator {
    @SuppressWarnings("squid:S5843")
    private static final String POSTCODE_REGEX = "^((([A-Za-z][0-9]{1,2})|(([A-Za-z][A-Ha-hJ-Yj-y][0-9]{1,2})|(([A-Za-z][0-9][A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y][0-9]?[A-Za-z])|([Gg][Ii][Rr]))))\\s?([0-9][A-Za-z]{2})|(0[Aa]{2}))$";
    private static final String POSTCODE_RESULT = "true";
    private final String url;
    private final boolean enabled;
    private final RestTemplate restTemplate;
    private final List<String> testPostcodes;

    @Autowired
    public PostcodeValidator(
        @Value("${postcode-validator.url}") final String url,
        @Value("${postcode-validator.enabled}") final boolean enabled,
        @Value("${postcode-validator.test-postcodes}") final String testPostcodes,
        RestTemplate restTemplate) {
        this.url = url;
        this.enabled = enabled;
        this.restTemplate = restTemplate;
        this.testPostcodes = stream(split(testPostcodes, ","))
            .map(StringUtils::stripToNull)
            .filter(StringUtils::isNotBlank)
            .collect(toList());
    }

    public boolean isValidPostcodeFormat(String postcode) {
        return postcode != null && postcode.matches(POSTCODE_REGEX);
    }

    public boolean isValid(String postcode) {
        if (!enabled) {
            log.info("PostcodeValidator is not enabled");
            return true;
        }
        if (testPostcodes.contains(postcode)) {
            log.info("PostcodeValidator received a test postcode {}", postcode);
            return true;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(headers);
        try {
            ResponseEntity<byte[]> response = restTemplate
                .exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    byte[].class,
                    postcode
                );
            logIfNotValidPostCode(postcode, response.getStatusCode().value());
            return response.getStatusCode().is2xxSuccessful() && nonNull(response.getBody()) && contains(new String(response.getBody()), POSTCODE_RESULT);

        } catch (RestClientResponseException e) {
            logIfNotValidPostCode(postcode, e.getStatusCode().value());
            return false;
        }
    }

    private void logIfNotValidPostCode(String postCode, int statusCode) {
        if (statusCode != 200) {
            log.info("Post code search returned statusCode {} for postcode {}", statusCode, postCode);
        }
    }


}
