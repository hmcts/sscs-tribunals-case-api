package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_DWP_REGIONAL_CENTRE;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.Authorize;
import uk.gov.hmcts.reform.sscs.idam.IdamApiClient;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.util.SyaServiceHelper;

@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@RunWith(JUnitParamsRunner.class)
@SpringBootTest
public class SubmitDraftTest {

    private static final String CLIENT_ID = "sscs";
    private static final String BASIC_AUTHORIZATION = "Basic ";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String RESPONSE_TYPE = "code";
    private static final String LOCATION_HEADER_NAME = "Location";

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Value("${test-url}")
    private String testUrl;

    @Autowired
    private IdamApiClient idamApiClient;

    @Autowired
    private CitizenCcdService citizenCcdService;

    @Value("${idam.oauth2.client.secret}")
    private String idamOauth2ClientSecret;

    @Value("${idam.oauth2.redirectUrl}")
    private String idamOauth2RedirectUrl;

    @Value("${idam.oauth2.citizen.email}")
    private String username;

    @Value("${idam.oauth2.citizen.password}")
    private String password;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private IdamService idamService;

    private String citizenToken;

    private IdamTokens citizenIdamTokens;

    private IdamTokens userIdamTokens;

    private SyaCaseWrapper draftAppeal;

    @Autowired
    private SubmitHelper submitHelper;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
        citizenToken = getIdamOauth2Token(username, password);
        citizenIdamTokens = IdamTokens.builder()
            .idamOauth2Token(citizenToken)
            .serviceAuthorization(authTokenGenerator.generate())
            .userId(getUserId(citizenToken))
            .build();

        userIdamTokens = idamService.getIdamTokens();
        draftAppeal = buildTestDraftAppeal();
    }

    @After
    public void tearDown() {
        List<SscsCaseData> savedDrafts = citizenCcdService.findCase(citizenIdamTokens);

        if (savedDrafts.size() > 0) {
            archiveDraft(savedDrafts.get(0));
        }
    }

    private String getUserId(String userToken) {
        return idamApiClient.getUserDetails(userToken).getId();
    }

    private SyaCaseWrapper buildTestDraftAppeal() {
        SyaCaseWrapper draftAppeal = new SyaCaseWrapper();
        draftAppeal.setCaseType("draft");
        draftAppeal.setBenefitType(new SyaBenefitType("Personal Independence Payment", "PIP"));
        return draftAppeal;
    }

    @Test
    @Parameters({
        "PIP,DWP PIP (1),Newcastle", "PIP,DWP PIP (2),Glasgow", "ESA,Inverness DRT,Inverness DRT",
        "ESA,Coatbridge Benefit Centre,Coatbridge Benefit Centre", "UC,,Universal Credit"
    })
    public void givenAppealIsSubmitted_shouldSetDwpRegionalCentre(String benefitCode, String dwpIssuingOffice,
                                                                  String expectedDwpRegionalCentre) {
        String body = ALL_DETAILS_DWP_REGIONAL_CENTRE.getSerializedMessage();
        String nino = submitHelper.getRandomNino();
        body = submitHelper.setNino(body, nino);
        body = submitHelper.setLatestMrnDate(body, LocalDate.now());
        body = submitHelper.setDwpIssuingOffice(body, dwpIssuingOffice);
        body = submitHelper.setBenefitCode(body, benefitCode);

        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header(AUTHORIZATION, citizenToken))
            .body(body)
            .put("/drafts");

        SscsCaseData draft = citizenCcdService.findCase(citizenIdamTokens).get(0);
        assertEquals(expectedDwpRegionalCentre, draft.getDwpRegionalCentre());
    }

    @Test
    public void givenAnUserSaveADraftMultipleTimes_shouldOnlyUpdateTheSameDraftForTheUser() {
        Response response = saveDraft(draftAppeal);
        response.then()
            .statusCode(anyOf(is(HttpStatus.SC_OK), is(HttpStatus.SC_CREATED)))
            .assertThat().header(LOCATION_HEADER_NAME, not(isEmptyOrNullString())).log().all(true);
        String responseHeader = response.getHeader(LOCATION_HEADER_NAME);

        Response response2 = saveDraft(draftAppeal);
        response2.then()
            .statusCode(HttpStatus.SC_OK)
            .assertThat().header(LOCATION_HEADER_NAME, not(isEmptyOrNullString())).log().all(true);
        String response2Header = response.getHeader(LOCATION_HEADER_NAME);

        assertEquals("the draft updated is not the same", responseHeader, response2Header);
    }

    @Test
    public void givenADraftExistsAndTheGetIsCalled_shouldReturn200AndTheDraft() {
        saveDraft(draftAppeal);
        RestAssured.given()
            .header(new Header(AUTHORIZATION, citizenToken))
            .get("/drafts")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .assertThat().body("BenefitType.benefitType", equalTo("Personal Independence Payment (PIP)"));
    }

    @Test
    public void givenGetDraftsIsCalledWithWrongCredentials_shouldReturn500Unauthorised() {
        RestAssured.given()
            .header(new Header(AUTHORIZATION, "thisTokenIsIncorrect"))
            .get("/drafts")
            .then()
            .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void onceADraftIsArchived_itCannotBeRetrievedByTheCitizenUser() {
        saveDraft(draftAppeal);

        List<SscsCaseData> savedDrafts = citizenCcdService.findCase(citizenIdamTokens);
        assertTrue(CollectionUtils.isNotEmpty(savedDrafts));
        SscsCaseData caseData = savedDrafts.get(0);

        archiveDraft(caseData);

        assertEquals(0, citizenCcdService.findCase(citizenIdamTokens).size());
    }

    private Response saveDraft(SyaCaseWrapper draftAppeal) {
        return RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header(AUTHORIZATION, citizenToken))
            .body(SyaServiceHelper.asJsonString(draftAppeal))
            .put("/drafts");
    }

    private void archiveDraft(SscsCaseData draftAppeal) {
        citizenCcdService.draftArchived(draftAppeal, citizenIdamTokens, userIdamTokens);
    }

    public String getIdamOauth2Token(String username, String password) {
        String authorisation = username + ":" + password;
        String base64Authorisation = Base64.getEncoder().encodeToString(authorisation.getBytes());

        Authorize authorize = idamApiClient.authorizeCodeType(
            BASIC_AUTHORIZATION + base64Authorisation,
            RESPONSE_TYPE,
            CLIENT_ID,
            idamOauth2RedirectUrl,
            " "
        );

        Authorize authorizeToken = idamApiClient.authorizeToken(
            authorize.getCode(),
            AUTHORIZATION_CODE,
            idamOauth2RedirectUrl,
            CLIENT_ID,
            idamOauth2ClientSecret,
            " "
        );

        return "Bearer " + authorizeToken.getAccessToken();
    }

}
