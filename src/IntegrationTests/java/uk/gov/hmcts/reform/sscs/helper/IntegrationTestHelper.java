package uk.gov.hmcts.reform.sscs.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

public class IntegrationTestHelper {

    private IntegrationTestHelper() {

    }

    public static MockHttpServletRequestBuilder getRequestWithAuthHeader(String json, String endpoint) {

        return getRequestWithoutAuthHeader(json, endpoint)
            .header(AuthorisationService.SERVICE_AUTHORISATION_HEADER, "some-auth-header");
    }

    public static MockHttpServletRequestBuilder getRequestWithoutAuthHeader(String json, String endpoint) {

        return post(endpoint)
            .contentType(APPLICATION_JSON)
             .content(json);
    }

    public static void assertHttpStatus(HttpServletResponse response, HttpStatus status) {
        assertThat(response.getStatus()).isEqualTo(status.value());
    }
}
