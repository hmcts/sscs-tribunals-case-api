package uk.gov.hmcts.reform.sscs.ccd;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.util.PactDslBuilderForCaseDetailsList.buildStartEventResponseWithEmptyCaseDetails;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;

public class StartForCitizenConsumerTest extends CcdConsumerTestBase {

    @Pact(provider = "ccdDataStoreAPI_Cases", consumer = "sscs_tribunalsCaseApi")
    public V4Pact startForCitizen(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("A Start for a Citizen is requested", setUpStateMapForProviderWithoutCaseData())
            .uponReceiving("A Start for a Citizen")
            .path(buildPath())
            .method("GET")
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN,
                SERVICE_AUTHORIZATION, SOME_SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .status(200)
            .body(buildStartEventResponseWithEmptyCaseDetails(CREATE_DRAFT))
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "startForCitizen")
    public void verifyStartForCitizen() throws JSONException {

        StartEventResponse startEventResponse = coreCaseDataApi.startForCitizen(SOME_AUTHORIZATION_TOKEN,
            SOME_SERVICE_AUTHORIZATION_TOKEN, USER_ID, jurisdictionId,
            caseType, CREATE_DRAFT);

        assertThat(startEventResponse.getEventId(), equalTo(CREATE_DRAFT));
        CaseDetails caseDetails = startEventResponse.getCaseDetails();
        assertNotNull(startEventResponse.getCaseDetails());
    }

    @Override
    protected Map<String, Object> setUpStateMapForProviderWithoutCaseData() throws JSONException {
        Map<String, Object> caseDataContentMap = super.setUpStateMapForProviderWithoutCaseData();
        caseDataContentMap.put(EVENT_ID, CREATE_DRAFT);
        return caseDataContentMap;
    }

    private String buildPath() {
        return new StringBuilder()
            .append("/citizens/")
            .append(USER_ID)
            .append("/jurisdictions/")
            .append(jurisdictionId)
            .append("/case-types/")
            .append(caseType)
            .append("/event-triggers/")
            .append(CREATE_DRAFT)
            .append("/token")
            .toString();
    }
}
