package uk.gov.hmcts.reform.sscs.ccd;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.util.PactDslBuilderForCaseDetailsList.buildCaseDetailsDsl;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.hamcrest.core.Is;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

public class ReadForCitizenConsumerTest extends CcdConsumerTestBase {

    @Pact(provider = "ccdDataStoreAPI_Cases", consumer = "sscs_tribunalsCaseApi")
    public V4Pact readForCitizen(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("A Read for a Citizen is requested", setUpStateMapForProviderWithCaseData(caseDataContent))
            .uponReceiving("A Read For a Citizen")
            .path(buildPath())
            .method("GET")
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN,
                SERVICE_AUTHORIZATION, SOME_SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .status(200)
            .body(buildCaseDetailsDsl(CASE_ID))
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "readForCitizen")
    public void verifyReadForCitizen() throws JSONException {

        CaseDetails caseDetailsReponse = coreCaseDataApi.readForCitizen(SOME_AUTHORIZATION_TOKEN,
            SOME_SERVICE_AUTHORIZATION_TOKEN, USER_ID, jurisdictionId,
            caseType, String.valueOf(CASE_ID));

        assertThat(caseDetailsReponse.getId(), Is.is(CASE_ID));
        assertThat(caseDetailsReponse.getJurisdiction(), Is.is("SSCS"));

    }

    private String buildPath() {
        return new StringBuilder()
            .append("/citizens/")
            .append(USER_ID)
            .append("/jurisdictions/")
            .append(jurisdictionId)
            .append("/case-types/")
            .append(caseType)
            .append("/cases/")
            .append(CASE_ID).toString();
    }
}
