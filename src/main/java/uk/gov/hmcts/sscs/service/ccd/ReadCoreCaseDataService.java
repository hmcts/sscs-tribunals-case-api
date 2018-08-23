package uk.gov.hmcts.sscs.service.ccd;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Retryable
    public CaseData getCcdCaseDataByAppealNumber(String appealNumber) {
        CaseDetails caseDetails = getCcdCaseDetailsByAppealNumber(appealNumber);
        return CcdUtil.getCaseData(caseDetails != null ? caseDetails.getData() : null);
    }

    @Retryable
    public CaseDetails getCcdCaseDetailsByAppealNumber(String appealNumber) {
        log.info("*** tribunals-service *** get case details by appeal number {}", appealNumber);
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

    @Retryable
    public CaseDetails getCcdCaseByNinoAndBenefitTypeAndMrnDate(String nino, String benefitType, String mrnDate) {
        log.info("*** tribunals-service *** get case details by nino and benefit type and mrn date {}, {}, {}", nino, benefitType, mrnDate);
        EventRequestData eventRequestData = coreCaseDataService.getEventRequestData("emptyEvent");
        String serviceAuthorization = idamService.generateServiceAuthorization();

        List<CaseDetails> caseDetailsList = getByNinoAndBenefitTypeAndMrnDate(eventRequestData, serviceAuthorization, nino, benefitType, mrnDate);
        return !caseDetailsList.isEmpty() ? caseDetailsList.get(0) : null;
    }

    private List<CaseDetails> getByNinoAndBenefitTypeAndMrnDate(EventRequestData eventRequestData, String serviceAuthorization,
                                                                String nino, String benefitType, String mrnDate) {
        log.info("Get getByNinoAndBenefitTypeAndMrnDate {}, {}, {} ...", nino, benefitType, mrnDate);
        return coreCaseDataService.getCoreCaseDataApi().searchForCaseworker(
                eventRequestData.getUserToken(),
                serviceAuthorization,
                eventRequestData.getUserId(),
                eventRequestData.getJurisdictionId(),
                eventRequestData.getCaseTypeId(),
                ImmutableMap.of("case.generatedNino", nino,
                        "case.appeal.benefitType.code", benefitType,
                        "case.appeal.mrnDetails.mrnDate", mrnDate)
        );
    }
}
