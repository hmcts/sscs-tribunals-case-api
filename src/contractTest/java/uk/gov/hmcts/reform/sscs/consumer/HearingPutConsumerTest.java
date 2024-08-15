package uk.gov.hmcts.reform.sscs.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static uk.gov.hmcts.reform.sscs.ContractTestDataProvider.*;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.HEARING_REQUESTED;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactFolder;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.sscs.ContractTestDataProvider;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApi;
import uk.gov.hmcts.reform.sscs.utility.BasePactTest;

@ExtendWith(PactConsumerTestExt.class)
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.sscs.service"})
@ActiveProfiles("contract")
@SpringBootTest
@PactTestFor(port = "10000")
@PactFolder("pacts")
class HearingPutConsumerTest extends BasePactTest {

    @Autowired
    private HmcHearingApi hmcHearingApi;

    @BeforeEach
    public void timeGapBetweenEachTest() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);
    }

    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact updateHearingRequestForValidRequest(PactDslWithProvider builder) {
        return builder.given(CONSUMER_NAME + " successfully updating hearing request ")
            .uponReceiving("Request to update hearing request to save details")
            .path(HEARING_PATH + "/" + VALID_CASE_ID)
            .method(HttpMethod.PUT.toString())
            .body(toJsonString(generateHearingRequest()))
            .headers(authorisedHeaders).willRespondWith()
            .status(HttpStatus.OK.value())
            .body(generateHearingsJsonBody(MSG_200_HEARING, HEARING_REQUESTED))
            .toPact();
    }

    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact validationErrorFromPutHearing(PactDslWithProvider builder) {
        return builder.given(CONSUMER_NAME
                                 + " throws validation error while trying to update hearing")
            .uponReceiving("Request to UPDATE hearing for invalid hearing request")
            .path(HEARING_PATH + "/" + VALID_CASE_ID)
            .method(HttpMethod.PUT.toString())
            .body(toJsonString(generateInvalidHearingRequest()))
            .headers(authorisedHeaders)
            .willRespondWith()
             .status(HttpStatus.BAD_REQUEST.value())
            .body(new PactDslJsonBody()
                      .stringType(FIELD_MESSAGE, MSG_400_HEARING)
                      .stringValue(FIELD_STATUS, BAD_REQUEST)
                      .eachLike(FIELD_ERRORS, 1)
                      .closeArray())
            .toPact();
    }

    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact unauthorisedRequestErrorFromPutHearing(PactDslWithProvider builder) {
        return builder.given(CONSUMER_NAME
                                 + " throws unauthorised error while trying to update hearing")
            .uponReceiving("Request to UPDATE hearing for unauthorised hearing request")
            .path(HEARING_PATH + "/" + VALID_CASE_ID).method(HttpMethod.PUT.toString())
            .body(toJsonString(generateHearingRequest()))
            .headers(unauthorisedHeaders)
            .willRespondWith().status(HttpStatus.UNAUTHORIZED.value())
            .body(new PactDslJsonBody()
                      .stringType(FIELD_MESSAGE, MSG_401_HEARING)
                      .stringValue(FIELD_STATUS, HttpStatus.UNAUTHORIZED.value()
                          + " " + HttpStatus.UNAUTHORIZED.getReasonPhrase())
                      .eachLike(FIELD_ERRORS, 1)
                      .closeArray())
            .toPact();
    }

    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact forbiddenRequestErrorFromPutHearing(PactDslWithProvider builder) {
        return builder.given(CONSUMER_NAME
                                 + " throws forbidden error while trying to updating hearing")
            .uponReceiving("Request to UPDATE hearing for forbidden hearing request")
            .path(HEARING_PATH + "/" + FORBIDDEN_CASE_ID).method(HttpMethod.PUT.toString())
            .body(toJsonString(generateHearingRequest()))
            .headers(authorisedHeaders)
            .willRespondWith().status(HttpStatus.FORBIDDEN.value())
            .body(new PactDslJsonBody()
                      .stringType(FIELD_MESSAGE, MSG_403_HEARING)
                      .stringValue(FIELD_STATUS, HttpStatus.FORBIDDEN.value()
                          + " " + HttpStatus.FORBIDDEN.getReasonPhrase())
                      .eachLike(FIELD_ERRORS, 1)
                      .closeArray())
            .toPact();
    }

    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact notFoundRequestErrorFromPutHearing(PactDslWithProvider builder) {
        return builder.given(CONSUMER_NAME
                                 + " throws not found request error while trying to update hearing")
            .uponReceiving("Request to UPDATE hearing for not found hearing request")
            .path(HEARING_PATH + "/" + NOT_FOUND_CASE_ID).method(HttpMethod.PUT.toString())
            .body(toJsonString(generateHearingRequest()))
            .headers(authorisedHeaders)
            .willRespondWith().status(HttpStatus.NOT_FOUND.value())
            .body(new PactDslJsonBody()
                      .stringType(FIELD_MESSAGE, MSG_404_HEARING)
                      .stringValue(FIELD_STATUS, HttpStatus.NOT_FOUND.value()
                          + " " + HttpStatus.NOT_FOUND.getReasonPhrase())
                      .eachLike(FIELD_ERRORS, 1)
                      .closeArray())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "updateHearingRequestForValidRequest", pactVersion = PactSpecVersion.V3)
    void shouldSuccessfullyPutHearingRequest() {
        HmcUpdateResponse hmcUpdateResponse = hmcHearingApi.updateHearingRequest(
            IDAM_OAUTH2_TOKEN,
            SERVICE_AUTHORIZATION_TOKEN,
            null,
            VALID_CASE_ID,
            generateHearingRequest()
        );

        assertNotNull(hmcUpdateResponse.getHearingRequestId());
        assertThat(hmcUpdateResponse.getStatus()).isEqualTo(HEARING_REQUESTED);
        assertNotNull(hmcUpdateResponse.getVersionNumber());
        assertNotSame(ZERO_NUMBER_LENGTH, hmcUpdateResponse.getVersionNumber());
        assertThat(hmcUpdateResponse.getTimeStamp()).isEqualToIgnoringSeconds(LocalDateTime.parse(ContractTestDataProvider.HEARING_DATE));
    }

    @Test
    @PactTestFor(pactMethod = "validationErrorFromPutHearing", pactVersion = PactSpecVersion.V3)
    void shouldReturn400BadRequestForPutHearing(MockServer mockServer) {

        executeCall(mockServer, authorisedHeaders,
                VALID_CASE_ID,
                           toJsonString(
                           generateInvalidHearingRequest()),
                HttpStatus.BAD_REQUEST.value()
        );
    }

    @Test
    @PactTestFor(pactMethod = "unauthorisedRequestErrorFromPutHearing", pactVersion = PactSpecVersion.V3)
    void shouldReturn401UnauthorisedRequestForPutHearing(MockServer mockServer) {
        executeCall(mockServer, unauthorisedHeaders,
                VALID_CASE_ID,
                           toJsonString(generateHearingRequest()),
                HttpStatus.UNAUTHORIZED.value()
        );
    }

    @Test
    @PactTestFor(pactMethod = "forbiddenRequestErrorFromPutHearing", pactVersion = PactSpecVersion.V3)
    void shouldReturn403ForbiddenRequestForPutHearing(MockServer mockServer) {
        executeCall(mockServer, authorisedHeaders,
                FORBIDDEN_CASE_ID,
                           toJsonString(generateHearingRequest()),
                HttpStatus.FORBIDDEN.value()
        );
    }

    @Test
    @PactTestFor(pactMethod = "notFoundRequestErrorFromPutHearing", pactVersion = PactSpecVersion.V3)
    void shouldReturn404NotFoundRequestForPutHearing(MockServer mockServer) {

        executeCall(mockServer, authorisedHeaders,
                NOT_FOUND_CASE_ID,
                           toJsonString(generateHearingRequest()),
                HttpStatus.NOT_FOUND.value()
        );

    }

    private void executeCall(MockServer mockServer, Map<String, String> headers,
                             String idValue, String hearingRequest,
                             int httpStatus) {
        RestAssured.given().headers(headers)
            .contentType(io.restassured.http.ContentType.JSON)
            .body(hearingRequest)
            .when()
            .put(mockServer.getUrl() + HEARING_PATH + "/" + idValue)
            .then().statusCode(httpStatus)
            .and().extract()
            .body();
    }
}
