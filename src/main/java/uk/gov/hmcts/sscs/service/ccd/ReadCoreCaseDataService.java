package uk.gov.hmcts.sscs.service.ccd;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;
import uk.gov.hmcts.sscs.exception.ApplicationErrorException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;

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
        CaseDetails caseDetails = get(eventRequestData, serviceAuthorization, caseId);

        return caseDetails;
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

    private CaseData printCaseDetailsInJson(Object object) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.convertValue(object, CaseData.class);
        } catch (Exception e) {
            throw new ApplicationErrorException("Oops...something went wrong...", e);
        }
    }

}
