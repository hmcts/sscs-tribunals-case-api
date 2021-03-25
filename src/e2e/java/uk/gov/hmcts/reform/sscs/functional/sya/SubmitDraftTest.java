package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_DWP_REGIONAL_CENTRE;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.*;
import uk.gov.hmcts.reform.sscs.util.SyaServiceHelper;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class SubmitDraftTest {

    private static final String CLIENT_ID = "sscs";
    private static final String BASIC_AUTHORIZATION = "Basic ";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String RESPONSE_TYPE = "code";
    private static final String LOCATION_HEADER_NAME = "Location";

    @Value("${test-url}")
    private String testUrl;

    @Autowired
    private IdamClient idamApiClient;

    @Autowired
    private CitizenCcdService citizenCcdService;

    @Value("${idam.client.secret}")
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
        UserDetails userDetails = getUserDetails(citizenToken);
        citizenIdamTokens = IdamTokens.builder()
            .idamOauth2Token(citizenToken)
            .serviceAuthorization(idamService.generateServiceAuthorization())
            .userId(userDetails.getId())
            .roles(userDetails.getRoles())
            .email(userDetails.getEmail())
            .build();

        userIdamTokens = idamService.getIdamTokens();
        draftAppeal = buildTestDraftAppeal();
    }

    @After
    public void tearDown() throws InterruptedException {
        List<SscsCaseData> savedDrafts = findCase(citizenIdamTokens);

        if (savedDrafts.size() > 0) {
            savedDrafts.stream().forEach(d -> archiveDraft(d));
        }
    }

    private UserDetails getUserDetails(String userToken) {
        return new UserDetailsTransformer(idamApiClient.getUserInfo(userToken)).asLocalUserDetails();
    }

    private SyaCaseWrapper buildTestDraftAppeal() {
        SyaCaseWrapper draftAppeal = new SyaCaseWrapper();
        draftAppeal.setCaseType("draft");
        draftAppeal.setBenefitType(new SyaBenefitType("Personal Independence Payment", "PIP"));
        return draftAppeal;
    }

    @Test
    public void givenAppealIsSubmitted_shouldSetDwpRegionalCentreToNewcastle() throws InterruptedException {
        String expectedDwpRegionalCentre = "Newcastle";

        RestAssured.given()
            .log().method().log().headers().log().uri().log().body(true)
            .contentType(ContentType.JSON)
            .header(new Header(AUTHORIZATION, citizenToken))
            .body(getAllDetailsDwpRegionalCentre("PIP", "DWP PIP (1)"))
            .put("/drafts");

        SscsCaseData draft = findCase(citizenIdamTokens).get(0);
        assertEquals(expectedDwpRegionalCentre, draft.getDwpRegionalCentre());
    }

    @Test
    public void givenAppealIsSubmitted_shouldSetDwpRegionalCentreToGlasgow() throws InterruptedException {
        String expectedDwpRegionalCentre = "Glasgow";

        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .header(new Header(AUTHORIZATION, citizenToken))
                .body(getAllDetailsDwpRegionalCentre("PIP", "DWP PIP (2)"))
                .put("/drafts");

        SscsCaseData draft = findCase(citizenIdamTokens).get(0);
        assertEquals(expectedDwpRegionalCentre, draft.getDwpRegionalCentre());
    }

    @Test
    public void givenAppealIsSubmitted_shouldSetDwpRegionalCentreToInvernessDrt() throws InterruptedException {
        String expectedDwpRegionalCentre = "Inverness DRT";

        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .header(new Header(AUTHORIZATION, citizenToken))
                .body(getAllDetailsDwpRegionalCentre("ESA", expectedDwpRegionalCentre))
                .put("/drafts");

        SscsCaseData draft = findCase(citizenIdamTokens).get(0);
        assertEquals(expectedDwpRegionalCentre, draft.getDwpRegionalCentre());
    }

    @Test
    public void givenAppealIsSubmitted_shouldSetDwpRegionalCentreToCoatbridgeBenefitCentre() throws InterruptedException {
        String expectedDwpRegionalCentre = "Coatbridge Benefit Centre";

        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .header(new Header(AUTHORIZATION, citizenToken))
                .body(getAllDetailsDwpRegionalCentre("ESA", expectedDwpRegionalCentre))
                .put("/drafts");

        SscsCaseData draft = findCase(citizenIdamTokens).get(0);
        assertEquals(expectedDwpRegionalCentre, draft.getDwpRegionalCentre());
    }

    @Test
    public void givenAppealIsSubmitted_shouldSetDwpRegionalCentreToUniversalCredit() throws InterruptedException {
        String expectedDwpRegionalCentre = "Universal Credit";
        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .header(new Header(AUTHORIZATION, citizenToken))
                .body(getAllDetailsDwpRegionalCentre("UC", ""))
                .put("/drafts");

        SscsCaseData draft = findCase(citizenIdamTokens).get(0);
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
    public void onceADraftIsArchived_itCannotBeRetrievedByTheCitizenUser() throws InterruptedException {
        saveDraft(draftAppeal);

        List<SscsCaseData> savedDrafts = findCase(citizenIdamTokens);
        assertTrue(CollectionUtils.isNotEmpty(savedDrafts));
        SscsCaseData caseData = savedDrafts.get(0);

        archiveDraft(caseData);

        assertEquals(0, citizenCcdService.findCase(citizenIdamTokens).size());
    }

    private List<SscsCaseData> findCase(IdamTokens idamTokens) throws InterruptedException {
        List<SscsCaseData> savedDrafts = citizenCcdService.findCase(idamTokens);
        if (CollectionUtils.isEmpty(savedDrafts)) {
            Thread.sleep(5000);
            savedDrafts = citizenCcdService.findCase(citizenIdamTokens);
        }
        return savedDrafts;
    }

    private String getAllDetailsDwpRegionalCentre(String benefitCode, String dwpIssuingOffice) {
        String body = ALL_DETAILS_DWP_REGIONAL_CENTRE.getSerializedMessage();
        String nino = submitHelper.getRandomNino();
        body = submitHelper.setNino(body, nino);
        body = submitHelper.setLatestMrnDate(body, LocalDate.now());
        body = submitHelper.setDwpIssuingOffice(body, dwpIssuingOffice);
        body = submitHelper.setBenefitCode(body, benefitCode);
        return body;
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
        citizenCcdService.draftArchivedFirst(draftAppeal, citizenIdamTokens, userIdamTokens);
    }

    public String getIdamOauth2Token(String username, String password) {
        return idamApiClient.getAccessToken(username, password);
    }
}
