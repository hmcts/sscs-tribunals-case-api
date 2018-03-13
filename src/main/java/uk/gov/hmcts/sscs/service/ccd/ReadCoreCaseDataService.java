package uk.gov.hmcts.sscs.service.ccd;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import java.util.List;

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

    public CaseDetails getCcdCaseDetailsByCaseId(String caseId) {
        log.info("Get CcdCaseDetails by CaseId...");
        EventRequestData eventRequestData = coreCaseDataService.getEventRequestData("appealCreated");
        String serviceAuthorization = coreCaseDataService.generateServiceAuthorization();

        return getByCaseId(eventRequestData, serviceAuthorization, caseId);
    }

    public CaseData getCcdCaseDataByCaseId(String caseId) {
        log.info("Get CcdCaseData by CaseId...");
        CaseDetails caseDetails = getCcdCaseDetailsByCaseId(caseId);
        return getCaseData(caseDetails != null ? caseDetails.getData() : null);
    }

    private CaseDetails getByCaseId(EventRequestData eventRequestData, String serviceAuthorization,
                                    String caseId) {
        log.info("Get getByCaseId...");
        return coreCaseDataService.getCoreCaseDataApi().readForCaseWorker(
            eventRequestData.getUserToken(),
            serviceAuthorization,
            eventRequestData.getUserId(),
            eventRequestData.getJurisdictionId(),
            eventRequestData.getCaseTypeId(),
            caseId
        );
    }

    public CaseDetails getCcdCaseDetailsByAppealNumber(String appealNumber) {
        log.info("Get CcdCaseDetails by appealNumber...");
        EventRequestData eventRequestData = coreCaseDataService.getEventRequestData("appealCreated");
        String serviceAuthorization = coreCaseDataService.generateServiceAuthorization();

        List<CaseDetails> caseDetailsList = getByAppealNumber(eventRequestData, serviceAuthorization, appealNumber);
        CaseDetails caseDetails = null;
        if (!caseDetailsList.isEmpty()) {
            caseDetails = caseDetailsList.get(0);
        }
        return caseDetails;
    }

    public CaseData getCcdCaseDataByAppealNumber(String appealNumber) {
        log.info("Get CcdCaseData by appealNumber...");
        CaseDetails caseDetails = getCcdCaseDetailsByAppealNumber(appealNumber);
        return getCaseData(caseDetails != null ? caseDetails.getData() : null);
    }

    private List<CaseDetails> getByAppealNumber(EventRequestData eventRequestData, String serviceAuthorization,
                                                String appealNumber) {
        log.info("Get getByAppealNumber...");
        return coreCaseDataService.getCoreCaseDataApi().searchForCaseworker(
                eventRequestData.getUserToken(),
                serviceAuthorization,
                eventRequestData.getUserId(),
                eventRequestData.getJurisdictionId(),
                eventRequestData.getCaseTypeId(),
                ImmutableMap.of("case.subscriptions.appellantSubscription.tya", appealNumber)
        );
    }

    private CaseData getCaseData(Object object) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        try {
            return mapper.convertValue(object, CaseData.class);
        } catch (Exception e) {
            throw new ApplicationErrorException("Error occurred when CaseDetails are mapped into CcdCase", e);
        }
    }
}
