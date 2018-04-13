package uk.gov.hmcts.sscs.service.ccd;

import com.google.common.collect.ImmutableMap;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.ccd.CcdUtil;

@Service
@Slf4j
public class ReadCoreCaseDataService {

    private final CoreCaseDataService coreCaseDataService;

    @Autowired
    public ReadCoreCaseDataService(CoreCaseDataService coreCaseDataService) {
        this.coreCaseDataService = coreCaseDataService;
    }

    public CaseDetails getCcdCaseDetailsByCaseId(String caseId) {
        EventRequestData eventRequestData = coreCaseDataService.getEventRequestData(null);
        String serviceAuthorization = coreCaseDataService.generateServiceAuthorization();

        return getByCaseId(eventRequestData, serviceAuthorization, caseId);
    }

    public CaseData getCcdCaseDataByCaseId(String caseId) {
        CaseDetails caseDetails = getCcdCaseDetailsByCaseId(caseId);
        return CcdUtil.getCaseData(caseDetails != null ? caseDetails.getData() : null);
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
        EventRequestData eventRequestData = coreCaseDataService.getEventRequestData(null);
        String serviceAuthorization = coreCaseDataService.generateServiceAuthorization();

        List<CaseDetails> caseDetailsList = getByAppealNumber(eventRequestData, serviceAuthorization, appealNumber);
        CaseDetails caseDetails = null;
        if (!caseDetailsList.isEmpty()) {
            caseDetails = caseDetailsList.get(0);
        }
        return caseDetails;
    }

    public CaseData getCcdCaseDataByAppealNumber(String appealNumber) {
        CaseDetails caseDetails = getCcdCaseDetailsByAppealNumber(appealNumber);
        return CcdUtil.getCaseData(caseDetails != null ? caseDetails.getData() : null);
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
}
