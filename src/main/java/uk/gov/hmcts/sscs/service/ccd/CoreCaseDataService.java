package uk.gov.hmcts.sscs.service.ccd;

import java.util.Base64;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.idam.Authorize;
import uk.gov.hmcts.sscs.service.idam.IdamApiClient;

@Service
@Slf4j
public class CoreCaseDataService {

    private final CoreCaseDataApi coreCaseDataApi;
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

    @Value("${core_case_data.api.url}")
    private String coreCaseDataApiUrl;

    @Value("${core_case_data.userId}")
    private String coreCaseDataUserId;

    @Value("${core_case_data.jurisdictionId}")
    private String coreCaseDataJurisdictionId;

    @Value("${core_case_data.caseTypeId}")
    private String coreCaseDataCaseTypeId;

    @Autowired
    public CoreCaseDataService(CoreCaseDataApi coreCaseDataApi,
                               AuthTokenGenerator authTokenGenerator,
                               IdamApiClient idamApiClient) {
        this.coreCaseDataApi = coreCaseDataApi;
        this.authTokenGenerator = authTokenGenerator;
        this.idamApiClient = idamApiClient;
    }

    protected String generateServiceAuthorization() {
        return authTokenGenerator.generate();
    }

    private String getIdamOauth2Token() {
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

        return  "Bearer " + authorizeToken.getAccessToken();
    }

    protected EventRequestData getEventRequestData(String eventId) {
        log.info("getEventRequestData...");
        return EventRequestData.builder()
            .userToken(getIdamOauth2Token())
            .userId(coreCaseDataUserId)
            .jurisdictionId(coreCaseDataJurisdictionId)
            .caseTypeId(coreCaseDataCaseTypeId)
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
        return coreCaseDataApiUrl;
    }

    protected CoreCaseDataApi getCoreCaseDataApi() {
        return coreCaseDataApi;
    }
}
