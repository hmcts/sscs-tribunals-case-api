package uk.gov.hmcts.reform.sscs.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.reform.sscs.ccd.util.ObjectMapperTestUtil.convertObjectToJsonString;
import static uk.gov.hmcts.reform.sscs.ccd.util.PactDslBuilderForCaseDetailsList.buildCaseDetailsDsl;
import static uk.gov.hmcts.reform.sscs.ccd.util.PactDslFixtureHelper.getCaseDataContentWithPath;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;

public class SubmitForCaseWorkerConsumerTest extends CcdConsumerTestBase {

    private static final String VALID_PAYLOAD_PATH = "json/base-case.json";

    @Override
    @BeforeAll
    public void setUp() {
        caseDataContent = CaseDataContent.builder()
            .eventToken("someEventToken")
            .event(
                Event.builder()
                    .id(createEventId)
                    .summary(SSCS_CASE_SUBMISSION_EVENT)
                    .description(SSCS_CASE_SUBMISSION_EVENT_DESCRIPTION)
                    .build()
            )
            .build();
    }

    @Pact(provider = "ccdDataStoreAPI_Cases", consumer = "sscs_tribunalsCaseApi")
    public V4Pact submitCaseWorkerDetails(PactBuilder builder) throws Exception {
        return builder
            .usingLegacyDsl()
            .given("A Submit for a Caseworker is requested",
                setUpStateMapForProviderWithoutCaseData())
            .uponReceiving("A Submit For a Caseworker")
            .path(buildPath())
            .query("ignore-warning=true")
            .method("POST")
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN,
                SERVICE_AUTHORIZATION, SOME_SERVICE_AUTHORIZATION_TOKEN)
            .body(convertObjectToJsonString(getCaseDataContentWithPath(createEventId, VALID_PAYLOAD_PATH)))
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .status(HttpStatus.SC_CREATED)
            .body(buildCaseDetailsDsl(CASE_ID))
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "submitCaseWorkerDetails")
    public void submitForCaseWorker() throws Exception {

        caseDataContent = getCaseDataContentWithPath(createEventId, VALID_PAYLOAD_PATH);
        CaseDetails caseDetailsReponse = coreCaseDataApi.submitForCaseworker(SOME_AUTHORIZATION_TOKEN,
            SOME_SERVICE_AUTHORIZATION_TOKEN, USER_ID, jurisdictionId,
            caseType, true, caseDataContent);

        assertNotNull(caseDetailsReponse);
        assertNotNull(caseDetailsReponse.getCaseTypeId());
        assertEquals(caseDetailsReponse.getJurisdiction(), "SSCS");
    }

    @Override
    protected Map<String, Object> setUpStateMapForProviderWithCaseData(CaseDataContent caseDataContent) throws JSONException {

        Map<String, Object> caseDataContentMap = super.setUpStateMapForProviderWithCaseData(caseDataContent);
        caseDataContentMap.put(EVENT_ID, createEventId);
        return caseDataContentMap;
    }

    private String buildPath() {
        return new StringBuilder()
            .append("/caseworkers/")
            .append(USER_ID)
            .append("/jurisdictions/")
            .append(jurisdictionId)
            .append("/case-types/")
            .append(caseType)
            .append("/cases")
            .toString();
    }
}
