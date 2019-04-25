package uk.gov.hmcts.reform.sscs.config;

import java.util.Comparator;
import java.util.HashMap;
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
public class CitizenCcdClient {

    private final CcdRequestDetails ccdRequestDetails;
    private final CoreCaseDataApi coreCaseDataApi;

    @Autowired
    public CitizenCcdClient(CcdRequestDetails ccdRequestDetails,
                            CoreCaseDataApi coreCaseDataApi) {
        this.ccdRequestDetails = ccdRequestDetails;
        this.coreCaseDataApi = coreCaseDataApi;
    }

    StartEventResponse startCaseForCitizen(IdamTokens idamTokens, String eventId) {
        return coreCaseDataApi.startForCitizen(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            ccdRequestDetails.getJurisdictionId(),
            ccdRequestDetails.getCaseTypeId(),
            eventId);
    }

    public CaseDetails submitForCitizen(IdamTokens idamTokens, CaseDataContent caseDataContent) {
        return coreCaseDataApi.submitForCitizen(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            ccdRequestDetails.getJurisdictionId(),
            ccdRequestDetails.getCaseTypeId(),
            true,
            caseDataContent
        );
    }

    List<CaseDetails> searchForCitizen(IdamTokens idamTokens) {
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("sortDirection", "desc");
        List<CaseDetails> caseDetailsList = coreCaseDataApi.searchForCitizen(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            ccdRequestDetails.getJurisdictionId(),
            ccdRequestDetails.getCaseTypeId(),
            searchCriteria
        );

        try {
            caseDetailsList.sort(Comparator.comparing(CaseDetails::getLastModified));
        } catch (NullPointerException e) {
            log.warn("Found a null in the lastModified date of a draft", e);
        }

        return caseDetailsList;
    }

    CaseDetails submitEventForCitizen(IdamTokens idamTokens, String caseId, CaseDataContent caseDataContent) {
        return coreCaseDataApi.submitEventForCitizen(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            ccdRequestDetails.getJurisdictionId(),
            ccdRequestDetails.getCaseTypeId(),
            caseId,
            true,
            caseDataContent
        );
    }

    StartEventResponse startEventForCitizen(IdamTokens idamTokens, String caseId, String eventType) {
        return coreCaseDataApi.startEventForCitizen(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            ccdRequestDetails.getJurisdictionId(),
            ccdRequestDetails.getCaseTypeId(),
            caseId,
            eventType
        );
    }
}

