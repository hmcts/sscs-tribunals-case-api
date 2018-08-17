package uk.gov.hmcts.sscs.service.ccd;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.EventRequestData;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.ccd.CcdUtil;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@Service
@Slf4j
public class ReadCoreCaseDataService {

    private final CoreCaseDataService coreCaseDataService;
    private final IdamService idamService;

    @Autowired
    public ReadCoreCaseDataService(CoreCaseDataService coreCaseDataService,
                                   IdamService idamService) {
        this.coreCaseDataService = coreCaseDataService;
        this.idamService = idamService;
    }

    public CaseData getCcdCaseDataByCaseId(String caseId) {
        CaseDetails caseDetails = getCcdCaseDetailsByCaseId(caseId);
        return CcdUtil.getCaseData(caseDetails != null ? caseDetails.getData() : null);
    }

    @Retryable
    public CaseDetails getCcdCaseDetailsByCaseId(String caseId) {
        log.info("*** tribunals-service *** get case details by case id {}", caseId);
        return tryGetCcdCaseDetailsByCaseId(caseId);
    }

    @Recover
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private CaseDetails retryGetCcdCaseDetailsByCaseId(String caseId) {
        log.info("*** tribunals-service *** retrying get case details by case id {}", caseId);
        return tryGetCcdCaseDetailsByCaseId(caseId);
    }

    private CaseDetails tryGetCcdCaseDetailsByCaseId(String caseId) {
        EventRequestData eventRequestData = coreCaseDataService.getEventRequestData("emptyEvent");
        String serviceAuthorization = idamService.generateServiceAuthorization();

        return getByCaseId(eventRequestData, serviceAuthorization, caseId);
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

    public CaseData getCcdCaseDataByAppealNumber(String appealNumber) {
        CaseDetails caseDetails = getCcdCaseDetailsByAppealNumber(appealNumber);
        return CcdUtil.getCaseData(caseDetails != null ? caseDetails.getData() : null);
    }

    @Retryable
    public CaseDetails getCcdCaseDetailsByAppealNumber(String appealNumber) {
        log.info("*** tribunals-service *** get case details by appeal number {}", appealNumber);
        return tryGetCcdCaseDetailsByAppealNumber(appealNumber);
    }

    @Recover
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private CaseDetails retryGetCcdCaseDetailsByAppealNumber(String appealNumber) {
        log.info("*** tribunals-service *** retrying get case details by appeal number {}", appealNumber);
        return tryGetCcdCaseDetailsByAppealNumber(appealNumber);
    }

    private CaseDetails tryGetCcdCaseDetailsByAppealNumber(String appealNumber) {
        EventRequestData eventRequestData = coreCaseDataService.getEventRequestData("emptyEvent");
        String serviceAuthorization = idamService.generateServiceAuthorization();

        List<CaseDetails> caseDetailsList = getByAppealNumber(eventRequestData, serviceAuthorization, appealNumber);
        CaseDetails caseDetails = null;
        if (!caseDetailsList.isEmpty()) {
            caseDetails = caseDetailsList.get(0);
        }
        return caseDetails;
    }

    private List<CaseDetails> getByAppealNumber(EventRequestData eventRequestData, String serviceAuthorization,
                                                String appealNumber) {
        log.info("Get getByAppealNumber {} ...", appealNumber);
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
