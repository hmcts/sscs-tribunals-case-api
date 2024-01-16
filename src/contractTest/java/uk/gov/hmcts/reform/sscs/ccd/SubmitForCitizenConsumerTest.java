package uk.gov.hmcts.reform.sscs.ccd;

import static uk.gov.hmcts.reform.sscs.ccd.util.ObjectMapperTestUtil.convertObjectToJsonString;
import static uk.gov.hmcts.reform.sscs.ccd.util.PactDslBuilderForCaseDetailsList.buildCaseDetailsDsl;
import static uk.gov.hmcts.reform.sscs.ccd.util.PactDslFixtureHelper.getCaseDataContent;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class SubmitForCitizenConsumerTest extends CcdConsumerTestBase {

    @Pact(provider = "ccdDataStoreAPI_Cases", consumer = "sscs_tribunalsCaseApi")
    public V4Pact submitForCitizen(PactBuilder builder) throws Exception {
        return builder
            .usingLegacyDsl()
            .given("A Submit for a Citizen is requested", setUpStateMapForProviderWithoutCaseData())
            .uponReceiving("A Submit For a Citizen")
            .path(buildPath())
            .query("ignore-warning=true")
            .method("POST")
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN,
                SERVICE_AUTHORIZATION, SOME_SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(convertObjectToJsonString(getCaseDataContent(createEventId)))
            .willRespondWith()
            .status(HttpStatus.SC_CREATED)
            .body(buildCaseDetailsDsl(CASE_ID))
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "submitForCitizen")
    public void verifySubmitEventForCitizen() throws Exception {
        caseDataContent = getCaseDataContent(createEventId);

        coreCaseDataApi.submitForCitizen(SOME_AUTHORIZATION_TOKEN,
            SOME_SERVICE_AUTHORIZATION_TOKEN, USER_ID, jurisdictionId,
            caseType, true, caseDataContent);

    }

    private String buildPath() {
        return new StringBuilder()
            .append("/citizens/")
            .append(USER_ID)
            .append("/jurisdictions/")
            .append(jurisdictionId)
            .append("/case-types/")
            .append(caseType)
            .append("/cases")
            .toString();
    }
}
