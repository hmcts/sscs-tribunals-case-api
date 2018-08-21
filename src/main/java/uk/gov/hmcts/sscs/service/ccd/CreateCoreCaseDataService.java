package uk.gov.hmcts.sscs.service.ccd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@Service
@Slf4j
public class CreateCoreCaseDataService {

    private final CoreCaseDataService coreCaseDataService;
    private final IdamService idamService;

    @Autowired
    CreateCoreCaseDataService(CoreCaseDataService coreCaseDataService,
                              IdamService idamService) {
        this.coreCaseDataService = coreCaseDataService;
        this.idamService = idamService;
    }

    @Retryable
    public CaseDetails createCcdCase(CaseData caseData) {
        log.info("*** tribunals-service *** createCcdCase for Nino {} and benefit type {}", caseData.getGeneratedNino(), caseData.getAppeal().getBenefitType());
        EventRequestData eventRequestData = coreCaseDataService.getEventRequestData("appealCreated");
        log.info("*** tribunals-service *** eventRequestData: {}", eventRequestData);
        String serviceAuthorization = idamService.generateServiceAuthorization();
        log.info("*** tribunals-service *** s2s token: {}", serviceAuthorization);
        StartEventResponse startEventResponse = start(eventRequestData, serviceAuthorization);
        return create(eventRequestData, serviceAuthorization, coreCaseDataService.getCaseDataContent(caseData,
                startEventResponse, "SSCS - appeal created event", "Created SSCS"));
    }

    private StartEventResponse start(EventRequestData eventRequestData, String serviceAuthorization) {
        log.info("*** tribunals-service *** Calling CCD (url: {}) endpoint to start Case For Caseworker...",
                coreCaseDataService.getCcdUrl());
        return coreCaseDataService.getCoreCaseDataApi().startForCaseworker(
                eventRequestData.getUserToken(),
                serviceAuthorization,
                eventRequestData.getUserId(),
                eventRequestData.getJurisdictionId(),
                eventRequestData.getCaseTypeId(),
                eventRequestData.getEventId());
    }

    private CaseDetails create(EventRequestData eventRequestData, String serviceAuthorization,
                               CaseDataContent caseDataContent) {
        log.info("*** tribunals-service *** Calling CCD endpoint to save CaseDetails For CaseWorker...");
        return coreCaseDataService.getCoreCaseDataApi().submitForCaseworker(
                eventRequestData.getUserToken(),
                serviceAuthorization,
                eventRequestData.getUserId(),
                eventRequestData.getJurisdictionId(),
                eventRequestData.getCaseTypeId(),
                eventRequestData.isIgnoreWarning(),
                caseDataContent
        );
    }

}
