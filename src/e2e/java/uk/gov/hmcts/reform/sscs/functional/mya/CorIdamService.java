package uk.gov.hmcts.reform.sscs.functional.mya;

import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.idam.Authorize;
import uk.gov.hmcts.reform.sscs.idam.IdamApiClient;

@Service
@Slf4j
public class CorIdamService {
    private final IdamApiClient idamApiClient;

    private final String idamOauth2ClientId;
    private final String idamOauth2ClientSecret;
    private final String idamOauth2RedirectUrl;

    @Autowired
    CorIdamService(IdamApiClient idamApiClient,
                   @Value("${idam.oauth2.client.id}") String idamOauth2ClientId,
                   @Value("${idam.oauth2.client.secret}") String idamOauth2ClientSecret,
                   @Value("${idam.oauth2.redirectUrl}") String idamOauth2RedirectUrl
    ) {
        this.idamApiClient = idamApiClient;
        this.idamOauth2ClientId = idamOauth2ClientId;
        this.idamOauth2ClientSecret = idamOauth2ClientSecret;
        this.idamOauth2RedirectUrl = idamOauth2RedirectUrl;
    }

    public String getUserToken(String idamOauth2UserEmail, String idamOauth2UserPassword) {
        log.info("Getting new idam token");
        String authorisation = idamOauth2UserEmail + ":" + idamOauth2UserPassword;
        String base64Authorisation = Base64.getEncoder().encodeToString(authorisation.getBytes());

        Authorize authorize = idamApiClient.authorizeCodeType(
                "Basic " + base64Authorisation,
                "code",
                idamOauth2ClientId,
                idamOauth2RedirectUrl,
                " "
        );

        Authorize authorizeToken = idamApiClient.authorizeToken(
                authorize.getCode(),
                "authorization_code",
                idamOauth2RedirectUrl,
                idamOauth2ClientId,
                idamOauth2ClientSecret,
                " "
        );

        return "Bearer " + authorizeToken.getAccessToken();
    }
}
