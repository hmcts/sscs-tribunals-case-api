package uk.gov.hmcts.reform.sscs.functional.handlers.addotherparty;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler.getJsonCallbackForTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.functional.ccd.CcdEventResponse;
import uk.gov.hmcts.reform.sscs.functional.mya.BaseFunctionTest;

@Slf4j
public class AddOtherPartyMidEventFunctionalTest extends BaseFunctionTest {

    private static final String ADD_OTHER_PARTY_VALID_JSON = "addOtherPartyCallback.json";
    private static final String ADD_OTHER_PARTY_WITH_TWO_PARTIES_JSON = "addOtherPartyWith2PartiesCallback.json";

    @Autowired
    protected ObjectMapper objectMapper;

    public AddOtherPartyMidEventFunctionalTest() {
        baseURI = baseUrl;
        useRelaxedHTTPSValidation();
    }

    @Test
    public void givenValidRequestWithOneOtherPart_thenReturnOk() throws IOException {
        String json = getJsonCallbackForTest(ADD_OTHER_PARTY_VALID_JSON);

        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(json), "");

        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);

        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);

        assertThat(ccdEventResponse.getErrors()).isEmpty();
    }

    @Test
    public void givenTwoOtherParties_thenReturnError() throws IOException {
        String json = getJsonCallbackForTest(ADD_OTHER_PARTY_WITH_TWO_PARTIES_JSON);

        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(json), "");

        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);

        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);

        assertThat(ccdEventResponse.getErrors())
            .hasSize(1)
            .contains("Only one other party data can be added using this event!");
    }

    private CcdEventResponse getCcdEventResponse(HttpResponse httpResponse) throws IOException {
        String response = EntityUtils.toString(httpResponse.getEntity());
        return objectMapper.readValue(response, CcdEventResponse.class);
    }
}
