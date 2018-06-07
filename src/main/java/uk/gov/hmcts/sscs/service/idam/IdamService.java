package uk.gov.hmcts.sscs.service.idam;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.sscs.model.idam.Authorize;

@Service
public class IdamService {

    private final AuthTokenGenerator authTokenGenerator;
    private final IdamApiClient idamApiClient;

    @Value("${idam.oauth2.user.email}")
    private String idamOauth2UserEmail;

    @Value("${idam.oauth2.user.password}")
    private String idamOauth2UserPassword;

    @Value("${idam.oauth2.client.id}")
    private String idamOauth2ClientId;

    @Value("${idam.oauth2.client.secret}")
    private String idamOauth2ClientSecret;

    @Value("${idam.oauth2.client.secret}")
    private String idamOauth2RedirectUrl;


    @Autowired
    public IdamService(AuthTokenGenerator authTokenGenerator, IdamApiClient idamApiClient
                       ) {
        this.authTokenGenerator = authTokenGenerator;
        this.idamApiClient = idamApiClient;
    }

    public String generateServiceAuthorization() {
        return authTokenGenerator.generate();
    }

    public String getIdamOauth2Token() {
        String authorisation = idamOauth2UserEmail
            + ":" + idamOauth2UserPassword;
        String base64Authorisation = Base64.getEncoder().encodeToString(authorisation.getBytes());

        Authorize authorize = idamApiClient.authorizeCodeType(
            "Basic " + base64Authorisation,
            "code",
            idamOauth2ClientId,
            idamOauth2RedirectUrl
        );

        Authorize authorizeToken = idamApiClient.authorizeToken(
            authorize.getCode(),
            "authorization_code",
            idamOauth2RedirectUrl,
            idamOauth2ClientId,
            idamOauth2ClientSecret
        );

        return "Bearer " + authorizeToken.getAccessToken();
    }
}
