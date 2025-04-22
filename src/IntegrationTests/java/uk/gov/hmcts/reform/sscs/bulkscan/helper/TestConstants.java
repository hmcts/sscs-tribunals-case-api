package uk.gov.hmcts.reform.sscs.bulkscan.helper;

public class TestConstants {

    private TestConstants() {

    }

    public static final String SERVICE_AUTHORIZATION_HEADER_KEY = "ServiceAuthorization";

    public static final String BEARER = "Bearer ";

    public static final String USER_AUTH_TOKEN = BEARER + "TEST_USER_AUTH_TOKEN";

    public static final String SERVICE_AUTH_TOKEN = BEARER + "TEST_SERVICE_AUTH";

    public static final String USER_TOKEN_WITHOUT_CASE_ACCESS = "USER_TOKEN_WITHOUT_CASE_ACCESS";

    public static final String USER_ID = "1234";

    public static final String JSON_TYPE = "application/json";

    public static final String START_EVENT_APPEAL_CREATED_URL =
        "/caseworkers/1234/jurisdictions/SSCS/case-types/Benefit/event-triggers/appealCreated/token";

    public static final String START_EVENT_VALID_APPEAL_CREATED_URL =
        "/caseworkers/1234/jurisdictions/SSCS/case-types/Benefit/event-triggers/validAppealCreated/token";

    public static final String UPDATE_EVENT_SEND_TO_DWP_URL =
        "/caseworkers/1234/jurisdictions/SSCS/case-types/Benefit/cases/1539878003972756/event-triggers/sendToDwp/token";

    public static final String START_EVENT_INCOMPLETE_CASE_URL =
        "/caseworkers/1234/jurisdictions/SSCS/case-types/Benefit/event-triggers/incompleteApplicationReceived/token";

    public static final String START_EVENT_NON_COMPLIANT_CASE_URL =
        "/caseworkers/1234/jurisdictions/SSCS/case-types/Benefit/event-triggers/nonCompliant/token";

    public static final String SUBMIT_EVENT_URL =
        "/caseworkers/1234/jurisdictions/SSCS/case-types/Benefit/cases?ignore-warning=true";

    public static final String SUBMIT_UPDATE_EVENT_URL =
        "/caseworkers/1234/jurisdictions/SSCS/case-types/Benefit/cases/1539878003972756/events?ignore-warning=true";

    public static final String READ_EVENT_URL =
        "/caseworkers/1234/jurisdictions/SSCS/case-types/Benefit/cases/1539878003972756";

    public static final String FIND_CASE_EVENT_URL =
        "/searchCases?ctid=Benefit";

    public static final String USER_ID_HEADER = "user-id";

    public static final String CONTENT_TYPE = "Content-Type";

    public static final String KEY = "key";

    public static final String VALUE = "value";
}
