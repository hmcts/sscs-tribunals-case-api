package uk.gov.hmcts.reform.sscs.ccd;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.util.ObjectMapperTestUtil.convertObjectToJsonString;
import static uk.gov.hmcts.reform.sscs.ccd.util.PactDslBuilderForCaseDetailsList.buildCaseDetailsDsl;
import static uk.gov.hmcts.reform.sscs.ccd.util.PactDslFixtureHelper.getCaseDataContent;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import org.hamcrest.CoreMatchers;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;

public class SubmitEventForCitizenConsumerTest extends CcdConsumerTestBase {

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
    public V4Pact submitEventForCitizen(PactBuilder builder) throws Exception {
        return builder
            .usingLegacyDsl()
            .given("A Submit Event for a Citizen is requested", setUpStateMapForProviderWithCaseData(caseDataContent))
            .uponReceiving("A Submit Event for a Citizen")
            .path(buildPath())
            .query("ignore-warning=true")
            .method("POST")
            .body(convertObjectToJsonString(getCaseDataContent(UPDATE_DRAFT)))
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN,
                SERVICE_AUTHORIZATION, SOME_SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .status(201)
            .body(buildCaseDetailsDsl(CASE_ID))
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "submitEventForCitizen")
    public void verifySubmitEventForCitizen() throws Exception {

        caseDataContent = getCaseDataContent(UPDATE_DRAFT);
        CaseDetails caseDetails = coreCaseDataApi.submitEventForCitizen(SOME_AUTHORIZATION_TOKEN,
            SOME_SERVICE_AUTHORIZATION_TOKEN, USER_ID, jurisdictionId,
            caseType, CASE_ID.toString(), true, caseDataContent);

        assertThat(caseDetails.getId(), CoreMatchers.is(CASE_ID));
        assertThat(caseDetails.getJurisdiction(), is("SSCS"));
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
            .append("/events")
            .toString();
    }

}
