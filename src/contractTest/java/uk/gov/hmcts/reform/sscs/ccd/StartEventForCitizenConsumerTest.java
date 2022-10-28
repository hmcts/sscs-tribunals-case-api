package uk.gov.hmcts.reform.sscs.ccd;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.util.PactDslBuilderForCaseDetailsList.buildStartEventReponse;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;

public class StartEventForCitizenConsumerTest extends CcdConsumerTestBase {

    @BeforeAll
    @Override
    public void setUp() throws Exception {
        caseDetailsMap = getCaseDetailsAsMap("sscs-map.json");
        caseDataContent = CaseDataContent.builder()
            .eventToken("someEventToken")
            .event(
                Event.builder()
                    .id(CREATE_DRAFT)
                    .summary(SSCS_CASE_SUBMISSION_EVENT)
                    .description(SSCS_CASE_SUBMISSION_EVENT_DESCRIPTION)
                    .build()
            ).data(caseDetailsMap.get("case_data"))
            .build();
    }


    @Pact(provider = "ccdDataStoreAPI_Cases", consumer = "sscs_tribunalsCaseApi")
    public V4Pact startEventForCitizen(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("A Start Event for a Citizen is requested", setUpStateMapForProviderWithCaseData(caseDataContent))
            .uponReceiving("A Start Event a Citizen")
            .path(buildPath())
            .method("GET")
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN,
                SERVICE_AUTHORIZATION, SOME_SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .status(200)
            .body(buildStartEventReponse(UPDATE_DRAFT))
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "startEventForCitizen")
    public void verifyStartEventForCitizen() throws JSONException {

        StartEventResponse startEventResponse = coreCaseDataApi.startEventForCitizen(SOME_AUTHORIZATION_TOKEN,
            SOME_SERVICE_AUTHORIZATION_TOKEN, USER_ID.toString(), jurisdictionId,
            caseType, CASE_ID.toString(), UPDATE_DRAFT);

        assertThat(startEventResponse.getEventId(), equalTo(UPDATE_DRAFT));

    }

    @Override
    protected Map<String, Object> setUpStateMapForProviderWithCaseData(CaseDataContent caseDataContent) throws JSONException {
        Map<String, Object> caseDataContentMap = super.setUpStateMapForProviderWithCaseData(caseDataContent);
        caseDataContentMap.put(EVENT_ID, UPDATE_DRAFT);
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
            .append("/cases/")
            .append(CASE_ID)
            .append("/event-triggers/")
            .append(UPDATE_DRAFT)
            .append("/token")
            .toString();
    }

}
