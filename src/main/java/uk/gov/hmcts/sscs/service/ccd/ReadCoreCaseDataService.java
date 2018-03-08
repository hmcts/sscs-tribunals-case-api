package uk.gov.hmcts.sscs.service.ccd;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;

@Service
@Slf4j
public class ReadCoreCaseDataService {

    private final CoreCaseDataService coreCaseDataService;

    @Autowired
    public ReadCoreCaseDataService(CoreCaseDataService coreCaseDataService) {
        this.coreCaseDataService = coreCaseDataService;
    }

    public CaseDetails getCcdCase(String caseId) {
        log.info("createCcdCase...");
        EventRequestData eventRequestData = coreCaseDataService.getEventRequestData("appealCreated");
        String serviceAuthorization = coreCaseDataService.generateServiceAuthorization();

        return get(eventRequestData, serviceAuthorization, caseId);
    }

    private CaseDetails get(EventRequestData eventRequestData, String serviceAuthorization,
                            String caseId) {
        log.info("get case details...");
        return coreCaseDataService.getCoreCaseDataApi().readForCaseWorker(
            eventRequestData.getUserToken(),
            serviceAuthorization,
            eventRequestData.getUserId(),
            eventRequestData.getJurisdictionId(),
            eventRequestData.getCaseTypeId(),
            caseId
        );
    }
}
