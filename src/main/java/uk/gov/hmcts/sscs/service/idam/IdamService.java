package uk.gov.hmcts.sscs.service.idam;

import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.sscs.ccd.properties.IdamProperties;
import uk.gov.hmcts.sscs.model.idam.Authorize;

@Service
@Slf4j
public class IdamService {

    private final AuthTokenGenerator authTokenGenerator;
    private final AuthTokenSubjectExtractor authTokenSubjectExtractor;
    private final IdamApiClient idamApiClient;
    private final IdamProperties idamProperties;

    @Autowired
    public IdamService(AuthTokenGenerator authTokenGenerator, AuthTokenSubjectExtractor authTokenSubjectExtractor,
                       IdamApiClient idamApiClient, IdamProperties idamProperties) {
        this.authTokenGenerator = authTokenGenerator;
        this.authTokenSubjectExtractor = authTokenSubjectExtractor;
        this.idamApiClient = idamApiClient;
        this.idamProperties = idamProperties;
    }

    public String generateServiceAuthorization() {
        return authTokenGenerator.generate();
    }

    public String getUserId(String oauth2Token) {
        return authTokenSubjectExtractor.extract(oauth2Token);
    }

    public String getIdamOauth2Token() {
        String authorisation = idamProperties.getOauth2().getUser().getEmail()
            + ":" + idamProperties.getOauth2().getUser().getPassword();
        String base64Authorisation = Base64.getEncoder().encodeToString(authorisation.getBytes());

        Authorize authorize = idamApiClient.authorizeCodeType(
            "Basic " + base64Authorisation,
            "code",
            idamProperties.getOauth2().getClient().getId(),
            idamProperties.getOauth2().getRedirectUrl()
        );

        Authorize authorizeToken = idamApiClient.authorizeToken(
            authorize.getCode(),
            "authorization_code",
            idamProperties.getOauth2().getRedirectUrl(),
            idamProperties.getOauth2().getClient().getId(),
            idamProperties.getOauth2().getClient().getSecret()
        );

        return "Bearer " + authorizeToken.getAccessToken();
    }
}
