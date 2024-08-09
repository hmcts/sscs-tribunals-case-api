package uk.gov.hmcts.reform.sscs.consumer;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static uk.gov.hmcts.reform.sscs.ContractTestDataProvider.CONSUMER_NAME;
import static uk.gov.hmcts.reform.sscs.ContractTestDataProvider.IDAM_OAUTH2_TOKEN;
import static uk.gov.hmcts.reform.sscs.ContractTestDataProvider.PROVIDER_NAME;
import static uk.gov.hmcts.reform.sscs.ContractTestDataProvider.SERVICE_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.sscs.ContractTestDataProvider.UNAUTHORISED_IDAM_OAUTH2_TOKEN;
import static uk.gov.hmcts.reform.sscs.ContractTestDataProvider.UNAUTHORISED_SERVICE_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.sscs.ContractTestDataProvider.authorisedHeadersGet;
import static uk.gov.hmcts.reform.sscs.ContractTestDataProvider.unauthorisedHeadersGet;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.sscs.ContractTestDataProvider;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingGetResponse;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApi;
import uk.gov.hmcts.reform.sscs.utility.BasePactTest;

@SpringBootTest
@ExtendWith(PactConsumerTestExt.class)
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.sscs.service"})
@ActiveProfiles("contract")
@PactTestFor(port = "10000")
class HearingGetConsumerTest extends BasePactTest {

    private static final String PATH_HEARING = "/hearing";
    private static final String VALID_CASE_ID = "123";
    private static final String OPTION_FIELD_IS_VALID = "isValid";
    private static final boolean OPTION_FIELD_IS_VALID_VALUE = false;
    private static final String VALID_NO_CONTENT_CASE_ID = "0";

    private static final String BAD_REQUEST_CASE_ID = "400";
    private static final String UNAUTHORISED_CASE_ID = "401";
    private static final String FORBIDDEN_CASE_ID = "403";
    private static final String NOT_FOUND_CASE_ID = "404";

    private static final LocalDateTime date = LocalDateTime.now();
    public static final String STATUS = "status";
    public static final String STATE = "sscs hearing api successfully returns case";

    @Autowired
    private HmcHearingApi hmcHearingApi;

    @BeforeEach
    public void timeGapBetweenEachTest() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);
    }


    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact getHearing(PactDslWithProvider builder) {

        return builder
            .given(STATE)
            .uponReceiving("Request to GET hearing for given valid case id")
            .matchPath(PATH_HEARING + "/" + VALID_CASE_ID)
            .method(GET.toString())
            .headers(authorisedHeadersGet)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(ContractTestDataProvider.generateValidHearingGetResponsePactDslJsonBody(date))
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getHearing")
    void shouldSuccessfullyGetHearing() throws JsonProcessingException {

        HearingGetResponse result = hmcHearingApi.getHearingRequest(
            IDAM_OAUTH2_TOKEN,
            SERVICE_AUTHORIZATION_TOKEN,
            null,
            VALID_CASE_ID,
            null
        );
        HearingGetResponse expected = getExpectedHearingGetResponse();
        assertEquals(expected, result);
    }

    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact getHearingWithRefCheck(PactDslWithProvider builder) {

        return builder
            .given("sscs hearing api successfully returns case and with ref check")
            .uponReceiving("Request to GET hearing for given valid case id and with ?isValid")
            .path(PATH_HEARING + "/" + VALID_CASE_ID)
            .method(GET.toString())
            .headers(authorisedHeadersGet)
            .query(OPTION_FIELD_IS_VALID + "=" + OPTION_FIELD_IS_VALID_VALUE)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(ContractTestDataProvider.generateValidHearingGetResponsePactDslJsonBody(date))
            .toPact();
    }


    @Test
    @PactTestFor(pactMethod = "getHearingWithRefCheck")
    void shouldSuccessfullyGetHearingWithRefCheck() throws JsonProcessingException {

        HearingGetResponse result = hmcHearingApi.getHearingRequest(
            IDAM_OAUTH2_TOKEN,
            SERVICE_AUTHORIZATION_TOKEN,
            null,
            VALID_CASE_ID,
            OPTION_FIELD_IS_VALID_VALUE
        );

        HearingGetResponse expected = getExpectedHearingGetResponse();

        assertEquals(expected, result);
    }

    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact getHearingWithNoContent(PactDslWithProvider builder) {

        return builder
            .given("sscs hearing api successfully returns no case")
            .uponReceiving("Request to GET hearing and get nothing")
            .path(PATH_HEARING + "/" + VALID_NO_CONTENT_CASE_ID)
            .method(GET.toString())
            .headers(authorisedHeadersGet)
            .willRespondWith()
            .status(NO_CONTENT.value())
            .body("")
            .toPact();
    }


    @Test
    @PactTestFor(pactMethod = "getHearingWithNoContent")
    void shouldSuccessfullyGetHearingWithNoContent() {

        HearingGetResponse result = hmcHearingApi.getHearingRequest(
            IDAM_OAUTH2_TOKEN,
            SERVICE_AUTHORIZATION_TOKEN,
            null,
            VALID_NO_CONTENT_CASE_ID,
            null
        );

        assertNull(result);
    }


    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact getHearingWithBadRequest(PactDslWithProvider builder) {

        return builder
            .given("sscs hearing api fail return with bad request status")
            .uponReceiving("Request to GET hearing for with bad request case id")
            .path(PATH_HEARING + "/" + BAD_REQUEST_CASE_ID)
            .method(GET.toString())
            .headers(authorisedHeadersGet)
            .willRespondWith()
            .status(BAD_REQUEST.value())
            .body("")
            .toPact();
    }


    @Test
    @PactTestFor(pactMethod = "getHearingWithBadRequest")
    void shouldFailGetHearingWithBadRequest() {

        assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(
            () -> hmcHearingApi.getHearingRequest(
                IDAM_OAUTH2_TOKEN,
                SERVICE_AUTHORIZATION_TOKEN,
                null,
                BAD_REQUEST_CASE_ID,
                null
            )).extracting(STATUS).isEqualTo(BAD_REQUEST);
    }


    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact getHearingWithUnauthorized(PactDslWithProvider builder) {

        return builder
            .given(STATE)
            .uponReceiving("Request to GET hearing for with unauthorized case id")
            .path(PATH_HEARING + "/" + UNAUTHORISED_CASE_ID)
            .method(GET.toString())
            .headers(unauthorisedHeadersGet)
            .willRespondWith()
            .status(UNAUTHORIZED.value())
            .body("")
            .toPact();
    }


    @Test
    @PactTestFor(pactMethod = "getHearingWithUnauthorized")
    void shouldFailGetHearingWithUnauthorized() {

        assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(
            () -> hmcHearingApi.getHearingRequest(
                UNAUTHORISED_IDAM_OAUTH2_TOKEN,
                UNAUTHORISED_SERVICE_AUTHORIZATION_TOKEN,
                null,
                UNAUTHORISED_CASE_ID,
                null
            )).extracting(STATUS).isEqualTo(UNAUTHORIZED);
    }

    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact getHearingWithForbidden(PactDslWithProvider builder) {

        return builder
            .given(STATE)
            .uponReceiving("Request to GET hearing for with forbidden case id")
            .path(PATH_HEARING + "/" + FORBIDDEN_CASE_ID)
            .method(GET.toString())
            .headers(authorisedHeadersGet)
            .willRespondWith()
            .status(FORBIDDEN.value())
            .body("")
            .toPact();
    }


    @Test
    @PactTestFor(pactMethod = "getHearingWithForbidden")
    void shouldFailGetHearingWithForbidden() {

        assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(
            () -> hmcHearingApi.getHearingRequest(
                IDAM_OAUTH2_TOKEN,
                SERVICE_AUTHORIZATION_TOKEN,
                null,
                FORBIDDEN_CASE_ID,
                null
            )).extracting(STATUS).isEqualTo(FORBIDDEN);
    }


    @Pact(provider = PROVIDER_NAME, consumer = CONSUMER_NAME)
    public RequestResponsePact getHearingWithNotFound(PactDslWithProvider builder) {

        return builder
            .given(STATE)
            .uponReceiving("Request to GET hearing for with not found case id")
            .path(PATH_HEARING + "/" + NOT_FOUND_CASE_ID)
            .method(GET.toString())
            .headers(authorisedHeadersGet)
            .willRespondWith()
            .status(NOT_FOUND.value())
            .body("")
            .toPact();
    }


    @Test
    @PactTestFor(pactMethod = "getHearingWithNotFound")
    void shouldFailGetHearingWithNotFound() {

        assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(
            () -> hmcHearingApi.getHearingRequest(
                IDAM_OAUTH2_TOKEN,
                SERVICE_AUTHORIZATION_TOKEN,
                null,
                NOT_FOUND_CASE_ID,
                null
            )).extracting(STATUS).isEqualTo(NOT_FOUND);

    }

    private HearingGetResponse getExpectedHearingGetResponse() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper.readValue(
            ContractTestDataProvider.generateValidHearingGetResponsePactDslJsonBody(date).toString(),
            HearingGetResponse.class
        );
    }
}
