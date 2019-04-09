package uk.gov.hmcts.reform.sscs.functional.sya;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.useRelaxedHTTPSValidation;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.util.Base64;
import java.util.List;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.Authorize;
import uk.gov.hmcts.reform.sscs.idam.IdamApiClient;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.util.SyaServiceHelper;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class SubmitDraftTest {

    private static final String CLIENT_ID = "sscs";
    private static final String BASIC_AUTHORIZATION = "Basic ";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String RESPONSE_TYPE = "code";

    @Value("${test-url}")
    private String testUrl;

    @Autowired
    private IdamApiClient idamApiClient;

    @Autowired
    private CitizenCcdService citizenCcdService;

    @Autowired
    private IdamService idamService;

    @Value("${idam.oauth2.client.secret}")
    private String idamOauth2ClientSecret;

    @Value("${idam.oauth2.redirectUrl}")
    private String idamOauth2RedirectUrl;

    @Value("${idam.oauth2.citizen.email}")
    private String username;

    @Value("${idam.oauth2.citizen.password}")
    private String password;

    private String userToken;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
        userToken = getIdamOauth2Token(username, password);
    }

    @Test
    public void givenDraft_shouldBeStoredInCcd() {
        SyaCaseWrapper draftAppeal = new SyaCaseWrapper();
        draftAppeal.setBenefitType(new SyaBenefitType("PIP", "pip benefit"));

        saveDraft(draftAppeal);
    }

    @Test
    public void savingMultipleDrafts_thereShouldOnlyEverBeOneDraftForTheUser() {
        SyaCaseWrapper draftAppeal = new SyaCaseWrapper();
        draftAppeal.setBenefitType(new SyaBenefitType("PIP", "pip benefit"));

        saveDraft(draftAppeal);
        saveDraft(draftAppeal);

        List<SscsCaseData> sscsCaseDataList = citizenCcdService.findCase(getIdamTokens());
        assertEquals(1, sscsCaseDataList.size());
    }

    private void saveDraft(SyaCaseWrapper draftAppeal) {
        RestAssured.given()
                .log().method().log().headers().log().uri().log().body(true)
                .contentType(ContentType.JSON)
                .header(new Header(AUTHORIZATION, userToken))
                .body(SyaServiceHelper.asJsonString(draftAppeal))
                .put("/drafts")
                .then()
                .statusCode(HttpStatus.CREATED_201)
                .assertThat().header("location", not(isEmptyOrNullString())).log().all(true);
    }


    private IdamTokens getIdamTokens() {
        return IdamTokens.builder()
                .idamOauth2Token(userToken)
                .serviceAuthorization(idamService.generateServiceAuthorization())
                .userId(idamService.getUserId(userToken))
                .build();
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
