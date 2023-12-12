package uk.gov.hmcts.reform.sscs.functional.mya;

import static org.apache.http.client.methods.RequestBuilder.post;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
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

        assertThat(httpResponse.getStatusLine().getStatusCode(), is(HttpStatus.CREATED.value()));
    }
}
