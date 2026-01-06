package uk.gov.hmcts.reform.sscs.bulkscan;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.functional.tyanotifications.Retry;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@TestPropertySource(locations = "classpath:application_e2e.yaml")
public class BaseFunctionalTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Rule
    public Retry retry = new Retry(2);

    protected IdamTokens idamTokens;

    @Autowired
    private IdamService idamService;

    @Autowired
    protected CcdService ccdService;

    @Value("${test-url}")
    private String testUrl;

    protected String ccdCaseId;

    @Before
    public void setup() {
        RestAssured.baseURI = testUrl;
        idamTokens = idamService.getIdamTokens();
        log.info("idamTokens.getUserId()" + idamTokens.getUserId());
        log.info("idamTokens.getServiceAuthorization()" + idamTokens.getServiceAuthorization());
        log.info("idamTokens.getIdamOauth2Token()" + idamTokens.getIdamOauth2Token());
    }

    protected Response simulateCcdCallbackNoUserIdOrAuthorization(String json, String urlPath, int expectedStatusCode) {
        final String callbackUrl = testUrl + urlPath;

        RestAssured.useRelaxedHTTPSValidation();
        Response response = RestAssured
            .given()
            .header("ServiceAuthorization", idamTokens.getServiceAuthorization())
            .contentType("application/json")
            .body(json)
            .when()
            .post(callbackUrl);

        assertEquals(expectedStatusCode, response.getStatusCode());

        return response;
    }

    protected Response simulateCcdCallbackNoUserIdOrAuthorization(String json, JsonPath expectedJson, String urlPath, int expectedStatusCode) {
        final String callbackUrl = testUrl + urlPath;

        RestAssured.useRelaxedHTTPSValidation();
        Response response = RestAssured
            .given()
            .header("ServiceAuthorization", idamTokens.getServiceAuthorization())
            .contentType("application/json")
            .then()
            .body(json, equalTo(expectedJson.getMap("")))
            .when()
            .post(callbackUrl);

        assertEquals(expectedStatusCode, response.getStatusCode());

        return response;
    }

    protected Response simulateCcdCallback(String json, String urlPath, int expectedStatusCode) {
        final String callbackUrl = testUrl + urlPath;

        RestAssured.useRelaxedHTTPSValidation();
        Response response = RestAssured
            .given()
            .header("ServiceAuthorization", idamTokens.getServiceAuthorization())
            .header(AUTHORIZATION, idamTokens.getIdamOauth2Token())
            .header("user-id", idamTokens.getUserId())
            .contentType("application/json")
            .body(json)
            .when()
            .post(callbackUrl);

        assertEquals(expectedStatusCode, response.getStatusCode());

        return response;
    }

    protected Response simulateCcdCallback(String json, JsonPath expectedJson, String urlPath, int expectedStatusCode) {
        final String callbackUrl = testUrl + urlPath;

        RestAssured.useRelaxedHTTPSValidation();
        Response response = RestAssured
            .given()
            .header("ServiceAuthorization", idamTokens.getServiceAuthorization())
            .header(AUTHORIZATION, idamTokens.getIdamOauth2Token())
            .header("user-id", idamTokens.getUserId())
            .contentType("application/json")
            .then()
            .body("", equalTo(expectedJson.getMap("")))
            .when()
            .post(callbackUrl);

        assertEquals(expectedStatusCode, response.getStatusCode());

        return response;
    }

    protected void createCase() {
        SscsCaseData caseData = CaseDataUtils.buildMinimalCaseData();
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated",
            "Bulk Scan appeal created", "Bulk Scan appeal created in test", idamTokens);
        ccdCaseId = String.valueOf(caseDetails.getId());
    }

    protected SscsCaseDetails findCaseInCcd(Response response) {
        JsonPath jsonPathEvaluator = response.jsonPath();
        @SuppressWarnings("rawtypes")
        Long caseRef = Long.parseLong(((HashMap) jsonPathEvaluator.get("data")).get("caseReference").toString());
        return ccdService.getByCaseId(caseRef, idamTokens);
    }

    protected String getJson(String resource) throws IOException {
        String file = Objects.requireNonNull(getClass().getClassLoader()
            .getResource(resource)).getFile();
        return FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8.name());
    }

    protected Response validateRecordEndpointRequest(String json, int statusCode) {
        return simulateCcdCallback(json, "/validate-record", statusCode);
    }

    protected Response validateOcrEndpointRequest(String json, String formType, int statusCode) {
        return simulateCcdCallbackNoUserIdOrAuthorization(json, "/forms/" + formType + "/validate-ocr", statusCode);
    }

    protected Response transformExceptionRequest(String json, int statusCode) {
        return simulateCcdCallbackNoUserIdOrAuthorization(json,"/transform-exception-record", statusCode);
    }

    protected String generateRandomNino() {
        String firstChar = generateRandomCharacterFromRange("ABCEHJKLMNOPRSTWXYZ");
        String secondChar = generateRandomCharacterFromRange("ABCEHJLMPRSWXY");
        String lastChar = generateRandomCharacterFromRange("ABCD");

        return firstChar + secondChar + RandomStringUtils.secure().next(6, false, true) + lastChar;
    }

    private String generateRandomCharacterFromRange(String range) {
        Random r = new Random();
        return String.valueOf(range.charAt(r.nextInt(range.length())));
    }

    protected String replaceNino(String json, String person1Nino, String person2Nino) {
        json = json.replace("{PERSON1_NINO}", person1Nino);
        json = json.replace("{PERSON2_NINO}", person2Nino);
        return json;
    }

    protected String replaceMrnDate(String json, String value) {
        json = json.replace("MRN_DATE", value);
        return json;
    }

    @SuppressWarnings("unchecked")
    protected void verifyResponseIsExpected(String expectedJson, Response response) {
        JsonPath transformationResponse = response.getBody().jsonPath();

        Map<String, Object> caseData = ((Map<String, Object>) transformationResponse
            .getMap("case_creation_details")
            .get("case_data"));

        LinkedHashMap subscriptions = ((LinkedHashMap) caseData.get("subscriptions"));

        expectedJson = replaceTyaInSubscription(expectedJson, "appellantSubscription", "TYA_RANDOM_NUMBER_APPELLANT", subscriptions);
        expectedJson = replaceTyaInSubscription(expectedJson, "appointeeSubscription", "TYA_RANDOM_NUMBER_APPOINTEE", subscriptions);
        expectedJson = replaceTyaInSubscription(expectedJson, "representativeSubscription", "TYA_RANDOM_NUMBER_REP", subscriptions);
        expectedJson = replaceTyaInSubscription(expectedJson, "jointPartySubscription", "TYA_RANDOM_NUMBER_JOINT_PARTY", subscriptions);

        String actualJson = response.getBody().prettyPrint();
        assertThatJson(actualJson)
            .whenIgnoringPaths(
                "case_creation_details.case_data.regionalProcessingCenter.epimsId",
                "case_creation_details.case_data.caseManagementLocation.region",
                "case_creation_details.case_data.regionalProcessingCenter.hearingRoute",
                "case_creation_details.case_data.appeal.appellant.id",
                "case_creation_details.case_data.appeal.rep.id",
                "case_creation_details.case_data.appeal.appellant.appointee.id"
            )
            .withFailMessage(String.format("Expected: %s%nActual: %s", expectedJson, actualJson)
            )
            .isEqualTo(expectedJson);
    }

    @SuppressWarnings("unchecked")
    private String replaceTyaInSubscription(String expectedJson, String subscriptionType, String replaceKey, LinkedHashMap subscriptions) {

        LinkedHashMap subscriptionFromType = (LinkedHashMap) subscriptions.get(subscriptionType);

        if (subscriptionFromType != null) {
            expectedJson = expectedJson.replace(replaceKey, (String) subscriptionFromType.get("tya"));
        }
        return expectedJson;
    }
}
