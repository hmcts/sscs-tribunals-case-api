package uk.gov.hmcts.sscs.service.ccd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.sscs.ccd.properties.CoreCaseDataProperties;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@Service
@Slf4j
public class CoreCaseDataService {

    private final CoreCaseDataApi coreCaseDataApi;
    private final CoreCaseDataProperties coreCaseDataProperties;
    private final IdamService idamService;

    @Autowired
    public CoreCaseDataService(CoreCaseDataApi coreCaseDataApi,
                               CoreCaseDataProperties coreCaseDataProperties,
                               IdamService idamService) {
        this.coreCaseDataApi = coreCaseDataApi;
        this.coreCaseDataProperties = coreCaseDataProperties;
        this.idamService = idamService;
    }

    protected EventRequestData getEventRequestData(String eventId) {
        log.info("getEventRequestData...");
        String oauth2Token = idamService.getIdamOauth2Token();
        return EventRequestData.builder()
            .userToken(oauth2Token)
            .userId(idamService.getUserId(oauth2Token))
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
