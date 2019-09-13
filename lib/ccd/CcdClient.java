package uk.gov.hmcts.reform.sscs.ccd;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class CcdClient {
    private final CcdRequestDetails ccdRequestDetails;
    private final CoreCaseDataApi coreCaseDataApi;

    @Autowired
    public CcdClient(CcdRequestDetails ccdRequestDetails,
                     CoreCaseDataApi coreCaseDataApi) {
        this.ccdRequestDetails = ccdRequestDetails;
        this.coreCaseDataApi = coreCaseDataApi;
    }

    public StartEventResponse startCaseForCaseworker(IdamTokens idamTokens, String eventId) {
        log.info("Starting CCD case for caseworker");
        return coreCaseDataApi.startForCaseworker(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                ccdRequestDetails.getJurisdictionId(),
                ccdRequestDetails.getCaseTypeId(),
                eventId);
    }

    public CaseDetails submitForCaseworker(IdamTokens idamTokens, CaseDataContent caseDataContent) {
        return coreCaseDataApi.submitForCaseworker(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                ccdRequestDetails.getJurisdictionId(),
                ccdRequestDetails.getCaseTypeId(),
                true,
                caseDataContent
        );
    }

    public StartEventResponse startEvent(IdamTokens idamTokens, Long caseId, String eventType) {
        log.info("Starting CCD event for caseworker");

        return coreCaseDataApi.startEventForCaseWorker(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                ccdRequestDetails.getJurisdictionId(),
                ccdRequestDetails.getCaseTypeId(),
                caseId.toString(),
                eventType);
    }

    public CaseDetails submitEventForCaseworker(IdamTokens idamTokens, Long caseId, CaseDataContent caseDataContent) {

        return coreCaseDataApi.submitEventForCaseWorker(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                ccdRequestDetails.getJurisdictionId(),
                ccdRequestDetails.getCaseTypeId(),
                caseId.toString(),
                true,
                caseDataContent);
    }

    public List<CaseDetails> searchForCaseworker(IdamTokens idamTokens, Map<String, String> searchCriteria) {
        return coreCaseDataApi.searchForCaseworker(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                ccdRequestDetails.getJurisdictionId(),
                ccdRequestDetails.getCaseTypeId(),
                new ImmutableMap.Builder<String, String>()
                        .putAll(searchCriteria)
                        .build());
    }

    public CaseDetails readForCaseworker(IdamTokens idamTokens, Long caseId) {
        return coreCaseDataApi.readForCaseWorker(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                ccdRequestDetails.getJurisdictionId(),
                ccdRequestDetails.getCaseTypeId(),
                caseId.toString()
        );
    }
}
