package uk.gov.hmcts.reform.sscs.functional.mya;

import static org.apache.hc.core5.http.io.support.ClassicRequestBuilder.post;
import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.http.HttpStatus;

public class IdamTestApiRequests {
    private final HttpClient client;
    private final String baseIdamApiUrl;

    public IdamTestApiRequests(HttpClient client, String baseIdamApiUrl) {
        this.client = client;
        this.baseIdamApiUrl = baseIdamApiUrl;
    }

    public CreateUser createUser(String email) throws IOException {
        CreateUser createUser = new CreateUser(
                email,
                "ATestForename",
                "ATestSurname",
                "Apassword123",
                Collections.singletonList(new Role("citizen"))
        );

        String body = new ObjectMapper().writeValueAsString(createUser);
        makePostRequest(client, baseIdamApiUrl + "/testing-support/accounts", body);

        return createUser;
    }

    private void makePostRequest(HttpClient client, String uri, String body) throws IOException {
        HttpResponse httpResponse = client.execute(post(uri)
                .setEntity(new StringEntity(body, APPLICATION_JSON))
                .build());

        assertThat(httpResponse.getCode(), is(HttpStatus.CREATED.value()));
    }
}
