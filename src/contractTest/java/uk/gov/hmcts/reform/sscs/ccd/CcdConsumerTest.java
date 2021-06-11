package uk.gov.hmcts.reform.sscs.ccd;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.model.RequestResponsePact;
import com.google.common.collect.Maps;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.parsing.Parser;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
public class CcdConsumerTest {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private static final String USER_ID = "1";
    private static final String JURSIDICTION_ID = "1";
    private static final String CASE_TYPE = "benefit";
    private static final String CASE_ID = "282";
    private static final String EVENT_ID = "APPEAL_CREATED";
    private static final String IDAM_OAUTH2_TOKEN = "123456";
    private static final String SERVICE_AUTHORIZATION_TOKEN = "678910";

    private static final String CCD_BASE_URL =
            "/caseworkers/" + USER_ID + "/jurisdictions/" + JURSIDICTION_ID
                    + "/case-types/" + CASE_TYPE;

    private static final String CCD_START_FOR_CASEWORKER_URL =
            CCD_BASE_URL + "/event-triggers/" + EVENT_ID + "/token";

    @BeforeEach
    public void setUp() {
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.config().encoderConfig(new EncoderConfig("UTF-8", "UTF-8"));
    }

    @Pact(provider = "ccd", consumer = "sscs_tribunals_case_api")
    public RequestResponsePact executeStartCaseForCaseworkerAndGet200(PactDslWithProvider builder) {

        Map<String, String> headers = Maps.newHashMap();
        headers.put(HttpHeaders.AUTHORIZATION, IDAM_OAUTH2_TOKEN);
        headers.put(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN);

        return builder
                .given("CCD starts a case for a caseworker")
                .uponReceiving("Provider receives a start case for caseworker request from an SSCS API")
                .path(CCD_START_FOR_CASEWORKER_URL)
                .method(HttpMethod.GET.toString())
                .headers(headers)
                .willRespondWith()
                .status(HttpStatus.OK.value())
                .body(createStartEventResponse())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeStartCaseForCaseworkerAndGet200")
    public void should_start_case_for_caseworker(MockServer mockServer) throws JSONException {

        Map<String, String> headers = Maps.newHashMap();
        headers.put(HttpHeaders.AUTHORIZATION, IDAM_OAUTH2_TOKEN);
        headers.put(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN);

        String actualResponseBody =
                RestAssured
                        .given()
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .when()
                        .get(mockServer.getUrl() + CCD_START_FOR_CASEWORKER_URL)
                        .then()
                        .statusCode(200)
                        .and()
                        .extract()
                        .body()
                        .asString();

        assertThat(actualResponseBody).isNotNull();

        JSONObject response = new JSONObject(actualResponseBody);

        assertThat(response).hasNoNullFieldsOrProperties();

    }

    private PactDslJsonBody createStartEventResponse() {

        return (PactDslJsonBody) new PactDslJsonBody()
                .stringValue("event_id", "123")
                .object("case_details", createCaseDetailsResponse());
    }

    private PactDslJsonBody createCaseDetailsResponse() {

        return (PactDslJsonBody) new PactDslJsonBody()
                .stringValue("id", "123")
                .stringValue("case_type_id", "1")
                .stringValue("created_date", "2018/01/01")
                .stringValue("last_modified", "2018/01/01 10:00:00")
                .stringValue("state", "123")
                .stringValue("locked_by",  "123")
                .stringValue("security_level", "1")
                .stringValue("security_classification", "")
                .stringValue("callback_response_status", "")
                .object("case_data")
                .stringValue("ccdCaseId", "123")
                .stringValue("region", "asd")
                .closeObject();
    }

}
