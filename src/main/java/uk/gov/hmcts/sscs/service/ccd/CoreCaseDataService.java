package uk.gov.hmcts.sscs.service.ccd;

import java.util.Base64;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.sscs.ccd.properties.CoreCaseDataProperties;
import uk.gov.hmcts.sscs.ccd.properties.IdamProperties;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.idam.Authorize;
import uk.gov.hmcts.sscs.service.idam.IdamApiClient;

@Service
@Slf4j
public class CoreCaseDataService {

    private final CoreCaseDataApi coreCaseDataApi;
    private final CoreCaseDataProperties coreCaseDataProperties;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamApiClient idamApiClient;
    private final IdamProperties idamProperties;

    @Autowired
    public CoreCaseDataService(CoreCaseDataApi coreCaseDataApi,
                               CoreCaseDataProperties coreCaseDataProperties,
                               AuthTokenGenerator authTokenGenerator,
                               IdamApiClient idamApiClient,
                               IdamProperties idamProperties) {
        this.coreCaseDataApi = coreCaseDataApi;
        this.coreCaseDataProperties = coreCaseDataProperties;
        this.authTokenGenerator = authTokenGenerator;
        this.idamApiClient = idamApiClient;
        this.idamProperties = idamProperties;
    }

    protected String generateServiceAuthorization() {

        String s2sToken = authTokenGenerator.generate();
        log.info("s2s Token: {}", s2sToken);
        return s2sToken;
    }

    private String getIdamOauth2Token() {
        log.info("getIdamOauth2Token...");
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

        String oauth2Token = "Bearer " + authorizeToken.getAccessToken();
        log.info("oauth2Token: " + oauth2Token);
        return oauth2Token;
    }

    protected EventRequestData getEventRequestData(String eventId) {
        log.info("getEventRequestData...");
        return EventRequestData.builder()
            .userToken(getIdamOauth2Token())
            .userId(coreCaseDataProperties.getUserId())
            .jurisdictionId(coreCaseDataProperties.getJurisdictionId())
            .caseTypeId(coreCaseDataProperties.getCaseTypeId())
            .eventId(eventId)
            .ignoreWarning(true)
            .build();
    }

    public CaseDataContent getCaseDataContent(CaseData caseData, StartEventResponse startEventResponse,
                                              String summary, String description) {
        return CaseDataContent.builder()
                .eventToken(startEventResponse.getToken())
                .event(Event.builder()
                        .id(startEventResponse.getEventId())
                        .summary(summary)
                        .description(description)
                        .build())
                .data(caseData)
                .build();
    }

    public String getCcdUrl() {
        return coreCaseDataProperties.getApi().getUrl();
    }

    protected CoreCaseDataApi getCoreCaseDataApi() {
        return coreCaseDataApi;
    }
}
