package uk.gov.hmcts.reform.sscs.ccd.util;

import java.util.Map;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;

public final class PactDslFixtureHelper {

    private PactDslFixtureHelper() {

    }

    public static final String SOME_AUTHORIZATION_TOKEN = "Bearer UserAuthToken";
    public static final String SOME_SERVICE_AUTHORIZATION_TOKEN = "ServiceToken";
    private static final String VALID_PAYLOAD_PATH = "json/base-case.json";

    static final String SSCS_CASE_SUBMISSION_EVENT_SUMMARY = "SSCS case submission event";
    static final String SSCS_CASE_SUBMISSION_EVENT_DESCRIPTION = "Submitting SSCS Case";

    public static CaseDataContent getCaseDataContent(String eventId) throws Exception {
        return PactDslFixtureHelper.getCaseDataContentWithPath(eventId, VALID_PAYLOAD_PATH);
    }

    public static CaseDataContent getCaseDataContentWithPath(String eventId, String payloadPath) throws Exception {

        final String caseData = ResourceLoader.loadJson(payloadPath);

        return CaseDataContent.builder()
            .eventToken(SOME_AUTHORIZATION_TOKEN)
            .event(
                Event.builder()
                    .id(eventId)
                    .summary(SSCS_CASE_SUBMISSION_EVENT_SUMMARY)
                    .description(SSCS_CASE_SUBMISSION_EVENT_DESCRIPTION)
                    .build()
            ).data(ObjectMapperTestUtil.convertStringToObject(caseData, Map.class))
            .build();
    }
}
