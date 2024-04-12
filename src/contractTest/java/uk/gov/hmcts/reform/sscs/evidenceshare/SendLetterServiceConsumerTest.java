package uk.gov.hmcts.reform.sscs.evidenceshare;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactFolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.hmcts.reform.sendletter.api.model.v3.Document;
import uk.gov.hmcts.reform.sendletter.api.model.v3.LetterV3;
import uk.gov.hmcts.reform.sendletter.api.proxy.SendLetterApiProxy;


@SpringBootTest
@ExtendWith(SpringExtension.class)
@ExtendWith(PactConsumerTestExt.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@PactFolder("pacts")
@PactTestFor(providerName = "rpeSendLetterService_SendLetterController", port = "4021")
@TestPropertySource(locations = {"classpath:application.properties"})
public class SendLetterServiceConsumerTest {
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private SendLetterApiProxy sendLetterApiProxy;

    private static final String XEROX_TYPE_PARAMETER = "PRO001";
    private static final String ADDITIONAL_DATA_CASE_REFERENCE = "caseReference";
    private static final String SERVICE_AUTHORIZATION_HEADER = "ServiceAuthorization";
    private final String someServiceAuthToken = "someServiceAuthToken";

    @Pact(provider = "rpeSendLetterService_SendLetterController", consumer = "sscs_tribunalsCaseApi")
    public RequestResponsePact createPactFragment(PactDslWithProvider builder) throws IOException, URISyntaxException {
        return builder
            .given("A valid send letter request is received")
            .uponReceiving("a request to send that letter")
            .path("/letters")
            .query("isAsync=false")
            .method("POST")
            .headers(SERVICE_AUTHORIZATION_HEADER, someServiceAuthToken)
            .body(createJsonObject(buildLetter()), "application/vnd.uk.gov.hmcts.letter-service.in.letter.v3+json")
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(new PactDslJsonBody()
                .uuid("letter_id", "123e4567-e89b-12d3-a456-556642440000"))
            .status(HttpStatus.SC_OK)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPactFragment", pactVersion = PactSpecVersion.V3)
    public void verifySendLetterPact() throws IOException, JSONException, URISyntaxException {
        SendLetterResponse sendLetterResponse = sendLetterApiProxy.sendLetter(someServiceAuthToken, "false", buildLetter());
        assert sendLetterResponse != null;
    }

    private LetterV3 buildLetter() throws IOException, URISyntaxException {
        Path pdfPath = Paths.get(ClassLoader.getSystemResource("files/myPdf.pdf").toURI());
        byte[] pdf = Files.readAllBytes(pdfPath);
        String response = Base64.getEncoder().encodeToString(pdf);
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put(ADDITIONAL_DATA_CASE_REFERENCE, "123421323");
        return new LetterV3(XEROX_TYPE_PARAMETER, Arrays.asList(new Document(response, 2)), additionalData);
    }

    protected String createJsonObject(Object obj) throws JSONException, IOException {
        return objectMapper.writeValueAsString(obj);
    }
}
