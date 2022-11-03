package uk.gov.hmcts.reform.sscs.ccd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.util.PactDslBuilderForCaseDetailsList.buildSearchResultDsl;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.sscs.ccd.util.ResourceLoader;

public class SearchCasesConsumerTest extends CcdConsumerTestBase {

    private static final String VALID_QUERY = "json/esQuery.json";
    private String queryString;

    @BeforeEach
    public void setUpEachTest() throws Exception {
        queryString = ResourceLoader.loadJson(VALID_QUERY);
    }

    @Pact(provider = "ccdDataStoreAPI_Cases", consumer = "sscs_tribunalsCaseApi")
    public V4Pact searchCases(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("A Search for cases is requested", setUpStateMapForProviderWithCaseData(caseDataContent))
            .uponReceiving("A Search Cases request")
            .path("/searchCases")
            .query("ctid=Benefit")
            .method("POST")
            .body(queryString)
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN,
                SERVICE_AUTHORIZATION, SOME_SERVICE_AUTHORIZATION_TOKEN)
            .headers(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .status(200)
            .body(buildSearchResultDsl())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "searchCases")
    public void verifySearchCases() throws JSONException {

        SearchResult searchResult = coreCaseDataApi.searchCases(SOME_AUTHORIZATION_TOKEN,
            SOME_SERVICE_AUTHORIZATION_TOKEN, "Benefit", queryString);
        assertEquals(searchResult.getCases().size(), 1);
    }
}
