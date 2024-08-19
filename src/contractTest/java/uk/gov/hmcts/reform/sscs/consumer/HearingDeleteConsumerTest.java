package uk.gov.hmcts.reform.sscs.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static uk.gov.hmcts.reform.sscs.ContractTestDataProvider.CONSUMER_NAME;
import static uk.gov.hmcts.reform.sscs.ContractTestDataProvider.PROVIDER_NAME;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus.CANCELLATION_REQUESTED;

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
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.sscs.ContractTestDataProvider;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingCancelRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApi;
import uk.gov.hmcts.reform.sscs.utility.BasePactTest;

@ExtendWith(PactConsumerTestExt.class)
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.sscs.service"})
@ActiveProfiles("contract")
@SpringBootTest
@PactTestFor(port = "10000")
@PactFolder("pacts")
class HearingDeleteConsumerTest extends BasePactTest {

    private static final String ID = "id";
    private static final String VALID_CASE_ID = "123";
    private static final String FORBIDDEN_CASE_ID = "456";
    private static final String NOT_FOUND_CASE_ID = "789";

    @Autowired
    private HmcHearingApi hmcHearingApi;


    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact badRequestErrorFromDeleteHearing(PactDslWithProvider builder) {
        return builder.given(CONSUMER_NAME
                                 + " throws bad request error while trying to delete hearing")
            .uponReceiving("Request to DELETE hearing for bad hearing request")
            .path(ContractTestDataProvider.HEARING_PATH + "/" + VALID_CASE_ID)
            .method(HttpMethod.DELETE.toString())
            .body(ContractTestDataProvider.toJsonString(ContractTestDataProvider.generateInvalidHearingDeleteRequest()))
            .headers(ContractTestDataProvider.authorisedHeaders)
            .willRespondWith().status(HttpStatus.BAD_REQUEST.value())
            .body(new PactDslJsonBody()
                      .stringType(ContractTestDataProvider.FIELD_MESSAGE,ContractTestDataProvider.MSG_400_HEARING)
                      .stringValue(ContractTestDataProvider.FIELD_STATUS, HttpStatus.BAD_REQUEST.value()
                          + " " + HttpStatus.BAD_REQUEST.getReasonPhrase())
                      .eachLike(ContractTestDataProvider.FIELD_ERRORS, 1)
                      .closeArray())
            .toPact();
    }

    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact unauthorisedRequestErrorFromDeleteHearing(PactDslWithProvider builder) {
        return builder.given(CONSUMER_NAME
                                 + " throws unauthorised error while trying to delete hearing")
            .uponReceiving("Request to DELETE hearing for unauthorised hearing request")
            .path(ContractTestDataProvider.HEARING_PATH + "/" + VALID_CASE_ID)
            .method(HttpMethod.DELETE.toString())
            .body(ContractTestDataProvider.toJsonString(ContractTestDataProvider.generateHearingDeleteRequest()))
            .headers(ContractTestDataProvider.unauthorisedHeaders)
            .willRespondWith().status(HttpStatus.UNAUTHORIZED.value())
            .body(new PactDslJsonBody()
                      .stringType(ContractTestDataProvider.FIELD_MESSAGE, ContractTestDataProvider.MSG_401_HEARING)
                      .stringValue(ContractTestDataProvider.FIELD_STATUS, HttpStatus.UNAUTHORIZED.value()
                          + " " + HttpStatus.UNAUTHORIZED.getReasonPhrase())
                      .eachLike(ContractTestDataProvider.FIELD_ERRORS, 1)
                      .closeArray())
            .toPact();
    }

    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact forbiddenRequestErrorFromDeleteHearing(PactDslWithProvider builder) {
        return builder.given(CONSUMER_NAME
                                 + " throws forbidden error while trying to delete hearing")
            .uponReceiving("Request to DELETE hearing for forbidden hearing request")
            .path(ContractTestDataProvider.HEARING_PATH + "/" + FORBIDDEN_CASE_ID)
            .method(HttpMethod.DELETE.toString())
            .body(ContractTestDataProvider.toJsonString(ContractTestDataProvider.generateHearingDeleteRequest()))
            .headers(ContractTestDataProvider.authorisedHeaders)
            .willRespondWith().status(HttpStatus.FORBIDDEN.value())
            .body(new PactDslJsonBody()
                      .stringType(ContractTestDataProvider.FIELD_MESSAGE,ContractTestDataProvider.MSG_403_HEARING)
                      .stringValue(ContractTestDataProvider.FIELD_STATUS, HttpStatus.FORBIDDEN.value()
                          + " " + HttpStatus.FORBIDDEN.getReasonPhrase())
                      .eachLike(ContractTestDataProvider.FIELD_ERRORS, 1)
                      .closeArray())
            .toPact();
    }

    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact notFoundRequestErrorFromDeleteHearing(PactDslWithProvider builder) {
        return builder.given(CONSUMER_NAME
                                 + " throws not found request error while trying to delete hearing")
            .uponReceiving("Request to DELETE hearing for not found hearing request")
            .path(ContractTestDataProvider.HEARING_PATH + "/" + NOT_FOUND_CASE_ID)
            .method(HttpMethod.DELETE.toString())
            .body(ContractTestDataProvider.toJsonString(ContractTestDataProvider.generateHearingDeleteRequest()))
            .headers(ContractTestDataProvider.authorisedHeaders)
            .willRespondWith().status(HttpStatus.NOT_FOUND.value())
            .body(new PactDslJsonBody()
                      .stringType(ContractTestDataProvider.FIELD_MESSAGE, ContractTestDataProvider.MSG_404_HEARING)
                      .stringValue(ContractTestDataProvider.FIELD_STATUS, HttpStatus.NOT_FOUND.value()
                          + " " + HttpStatus.NOT_FOUND.getReasonPhrase())
                      .eachLike(ContractTestDataProvider.FIELD_ERRORS, 1)
                      .closeArray())
            .toPact();
    }

    @Disabled
    @Test
    @PactTestFor(pactMethod = "deleteHearingRequestForValidRequest", pactVersion = PactSpecVersion.V3)
    void shouldSuccessfullyDeleteHearingRequest() {
        HmcUpdateResponse hmcUpdateResponse = hmcHearingApi.cancelHearingRequest(
            ContractTestDataProvider.IDAM_OAUTH2_TOKEN,
            ContractTestDataProvider.SERVICE_AUTHORIZATION_TOKEN,
            null,
            VALID_CASE_ID,
            ContractTestDataProvider.generateHearingDeleteRequest()
        );

        assertNotNull(hmcUpdateResponse.getHearingRequestId());
        assertThat(hmcUpdateResponse.getStatus()).isEqualTo(CANCELLATION_REQUESTED);
        assertNotNull(hmcUpdateResponse.getVersionNumber());
        assertNotSame(ContractTestDataProvider.ZERO_NUMBER_LENGTH, hmcUpdateResponse.getVersionNumber());
        assertNotNull(hmcUpdateResponse.getTimeStamp());
    }

    @Test
    @PactTestFor(pactMethod = "badRequestErrorFromDeleteHearing", pactVersion = PactSpecVersion.V3)
    void shouldReturn400BadRequestForDeleteHearing(MockServer mockServer) {
        executeCall(mockServer, ContractTestDataProvider.authorisedHeaders, VALID_CASE_ID,
                    ContractTestDataProvider.generateInvalidHearingDeleteRequest(), HttpStatus.BAD_REQUEST
        );
    }

    @Test
    @PactTestFor(pactMethod = "unauthorisedRequestErrorFromDeleteHearing", pactVersion = PactSpecVersion.V3)
    void shouldReturn401UnauthorisedRequestForDeleteHearing(MockServer mockServer) {
        executeCall(mockServer, ContractTestDataProvider.unauthorisedHeaders, VALID_CASE_ID,
                    ContractTestDataProvider.generateHearingDeleteRequest(), HttpStatus.UNAUTHORIZED
        );
    }

    @Test
    @PactTestFor(pactMethod = "forbiddenRequestErrorFromDeleteHearing", pactVersion = PactSpecVersion.V3)
    void shouldReturn403ForbiddenRequestForDeleteHearing(MockServer mockServer) {
        executeCall(mockServer, ContractTestDataProvider.authorisedHeaders, FORBIDDEN_CASE_ID,
                    ContractTestDataProvider.generateHearingDeleteRequest(), HttpStatus.FORBIDDEN
        );
    }

    @Test
    @PactTestFor(pactMethod = "notFoundRequestErrorFromDeleteHearing", pactVersion = PactSpecVersion.V3)
    void shouldReturn404NotFoundRequestForDeleteHearing(MockServer mockServer) {
        executeCall(mockServer, ContractTestDataProvider.authorisedHeaders, NOT_FOUND_CASE_ID,
                    ContractTestDataProvider.generateHearingDeleteRequest(), HttpStatus.NOT_FOUND
        );
    }

    private void executeCall(MockServer mockServer, Map<String, String> headers, String caseId,
                             HearingCancelRequestPayload payload, HttpStatus status) {
        RestAssured.given().headers(headers)
            .contentType(ContentType.JSON)
            .body(ContractTestDataProvider.toJsonString(payload)).when()
            .delete(mockServer.getUrl() + ContractTestDataProvider.HEARING_PATH + "/" + caseId)
            .then().statusCode(status.value())
            .and().extract()
            .body()
            .jsonPath();
    }

}
