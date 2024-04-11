package uk.gov.hmcts.reform.sscs.idam;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.idam.client.models.TokenRequest;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

public class IdamApiConsumerTest extends IdamConsumerTestBase {

    @Pact(provider = "idamApi_oidc", consumer = "sscs_tribunalsCaseApi")
    public V4Pact generatePactForUserInfo(PactBuilder builder) throws JSONException {

        return builder
            .usingLegacyDsl()
            .given("userinfo is requested")
            .uponReceiving("A request for a UserInfo from SSCS Tribunals API")
            .path("/o/userinfo")
            .method("GET")
            .matchHeader(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN)
            .willRespondWith()
            .status(200)
            .body(createUserDetailsResponse())
            .toPact(V4Pact.class);
    }

    @Pact(provider = "idamApi_oidc", consumer = "sscs_tribunalsCaseApi")
    public V4Pact generatePactForToken(PactBuilder builder) {

        Map<String, String> responseheaders = ImmutableMap.<String, String>builder()
            .put("Content-Type", "application/json")
            .build();

        return builder
            .usingLegacyDsl()
            .given("a token is requested")
            .uponReceiving("Provider receives a POST /o/token request from SSCS Tribunals API")
            .path("/o/token")
            .method(HttpMethod.POST.toString())
            .body("redirect_uri=http%3A%2F%2Fwww.dummy-pact-service.com%2Fcallback"
                    + "&client_id=sscs"
                    + "&grant_type=password"
                    + "&username=" + caseworkerUsername
                    + "&password=" + caseworkerPwd
                    + "&client_secret=" + clientSecret
                    + "&scope=openid profile roles",
                APPLICATION_FORM_URLENCODED_VALUE)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(responseheaders)
            .body(createAuthResponse())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "generatePactForUserInfo")
    public void verifyIdamUserDetailsRolesPactUserInfo() {
        UserInfo userInfo = idamApi.retrieveUserInfo(SOME_AUTHORIZATION_TOKEN);
        assertNotNull(userInfo.getUid());
        assertNotNull(userInfo.getSub());
        assertNotNull(userInfo.getGivenName());
        assertNotNull(userInfo.getFamilyName());
        assertNotNull(userInfo.getRoles());
        assertTrue(userInfo.getRoles().size() > 0);

    }

    @Test
    @PactTestFor(pactMethod = "generatePactForToken")
    public void verifyIdamUserDetailsRolesPactToken() {

        TokenResponse token = idamApi.generateOpenIdToken(buildTokenRequestMap());
        assertNotNull("Token is expected", token.accessToken);
    }

    private TokenRequest buildTokenRequestMap() {
        return new TokenRequest(
            "sscs",
            clientSecret,
            "password",
            "http://www.dummy-pact-service.com/callback",
            caseworkerUsername,
            caseworkerPwd,
            "openid profile roles",
            null, null);
    }


    private PactDslJsonBody createUserDetailsResponse() {

        return new PactDslJsonBody()
            .stringType("sub", "61")
            .stringType("uid", "sscs-citizen2@hmcts.net")
            .stringType("givenName", "Test")
            .stringType("familyName", "User")
            .minArrayLike("roles", 1, PactDslJsonRootValue.stringType("citizen"), 1);
    }

    private PactDslJsonBody createAuthResponse() {
        return new PactDslJsonBody()
            .stringType("access_token", "eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre")
            .stringType("scope", "openid roles profile");
    }

}
