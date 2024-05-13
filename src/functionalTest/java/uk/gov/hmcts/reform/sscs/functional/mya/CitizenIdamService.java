package uk.gov.hmcts.reform.sscs.functional.mya;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.IdamClient;

@Service
@Slf4j
public class CitizenIdamService {
    private final IdamClient idamApiClient;

    private final String idamOauth2ClientId;
    private final String idamOauth2ClientSecret;
    private final String idamOauth2RedirectUrl;

    @Autowired
    CitizenIdamService(IdamClient idamApiClient,
                       @Value("${idam.client.id}") String idamOauth2ClientId,
                       @Value("${idam.client.secret}") String idamOauth2ClientSecret,
                       @Value("${idam.oauth2.redirectUrl}") String idamOauth2RedirectUrl
    ) {
        this.idamApiClient = idamApiClient;
        this.idamOauth2ClientId = idamOauth2ClientId;
        this.idamOauth2ClientSecret = idamOauth2ClientSecret;
        this.idamOauth2RedirectUrl = idamOauth2RedirectUrl;
    }

    public String getUserToken(String idamOauth2UserEmail, String idamOauth2UserPassword) {
        log.info("Getting new idam token");
        return idamApiClient.getAccessToken(idamOauth2UserEmail,  idamOauth2UserPassword);
    }
}
