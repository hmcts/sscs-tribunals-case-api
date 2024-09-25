package uk.gov.hmcts.reform.sscs.evidenceshare;

import static org.mockito.Mockito.when;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentDownloadClientApi;


@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@PactTestFor(providerName = "em_dm_store", port = "5006")
@PactDirectory("pacts")
@SpringBootTest({
    "document_management.url : http://localhost:5006"
})
public class DocumentServiceConsumerTest {
    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private DocumentDownloadClientApi deleteApi;

    public static final String SOME_SERVICE_AUTHORIZATION_TOKEN = "ServiceToken";
    private static final String USER_ID = "id1";
    private static final String USER_ROLES = "admin";
    private static final String DOCUMENT_ID = "5c3c3906-2b51-468e-8cbb-a4002eded075";
    private static final String AUTH_TOKEN = "Bearer someAuthToken";

    @Pact(provider = "em_dm_store", consumer = "sscs_tribunalsCaseApi")
    public RequestResponsePact generatePactFragment(PactDslWithProvider builder) throws IOException {
        Map<String, String> headers = Maps.newHashMap();
        headers.put("ServiceAuthorization", SOME_SERVICE_AUTHORIZATION_TOKEN);
        headers.put("user-id", USER_ID);

        return builder
            .given("I have existing document")
            .uponReceiving("a request for download the document")
            .path("/documents/" + DOCUMENT_ID + "/binary")
            .method("GET")
            .headers(headers)
            .willRespondWith()
            .status(200)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "generatePactFragment", pactVersion = PactSpecVersion.V3)
    public void verifyPactFragment() {
        when(authTokenGenerator.generate()).thenReturn(SOME_SERVICE_AUTHORIZATION_TOKEN);
        ResponseEntity<?> response = deleteApi.downloadBinary(AUTH_TOKEN, SOME_SERVICE_AUTHORIZATION_TOKEN, USER_ROLES, USER_ID,
            "/documents/" + DOCUMENT_ID + "/binary");
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
    }
}
