package uk.gov.hmcts.sscs.service.ccd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@Service
@Slf4j
public class UpdateCoreCaseDataService {

    private final CoreCaseDataService coreCaseDataService;
    private final IdamService idamService;

    @Autowired
    public UpdateCoreCaseDataService(CoreCaseDataService coreCaseDataService,
                                     IdamService idamService) {
        this.coreCaseDataService = coreCaseDataService;
        this.idamService = idamService;
    }

    public CaseDetails updateCcdCase(CaseData caseData, Long caseId, String eventId) {
        log.info("*** tribunals-service *** updateCcdCase ");
        EventRequestData eventRequestData = coreCaseDataService.getEventRequestData(eventId);
        String serviceAuthorization = idamService.generateServiceAuthorization();
        StartEventResponse startEventResponse = start(eventRequestData, serviceAuthorization, caseId);

        return create(eventRequestData, serviceAuthorization, coreCaseDataService.getCaseDataContent(caseData,
            startEventResponse, "SSCS - appeal updated event", "Updated SSCS"), caseId);
    }

    private StartEventResponse start(EventRequestData eventRequestData, String serviceAuthorization, Long caseId) {
        log.info("*** tribunals-service *** Calling CCD (url: {}) endpoint to update Case For Caseworker...",
            coreCaseDataService.getCcdUrl());
        return coreCaseDataService.getCoreCaseDataApi().startEventForCaseWorker(
                eventRequestData.getUserToken(),
                serviceAuthorization,
                eventRequestData.getUserId(),
                eventRequestData.getJurisdictionId(),
                eventRequestData.getCaseTypeId(),
                caseId.toString(),
                eventRequestData.getEventId()
        );
    }

    private CaseDetails create(EventRequestData eventRequestData, String serviceAuthorization,
                               CaseDataContent caseDataContent, Long caseId) {

        return coreCaseDataService.getCoreCaseDataApi().submitEventForCaseWorker(
                eventRequestData.getUserToken(),
                serviceAuthorization,
                eventRequestData.getUserId(),
                eventRequestData.getJurisdictionId(),
                eventRequestData.getCaseTypeId(),
                caseId.toString(),
                eventRequestData.isIgnoreWarning(),
                caseDataContent
        );
    }

}
