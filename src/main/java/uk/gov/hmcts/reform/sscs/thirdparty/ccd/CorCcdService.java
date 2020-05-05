package uk.gov.hmcts.reform.sscs.thirdparty.ccd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CreateCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.ReadCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.SearchCcdCaseService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdClient;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


@Service
public class CorCcdService extends uk.gov.hmcts.reform.sscs.ccd.service.CcdService {
    private final CitizenCcdClient citizenCcdClient;
    private final IdamService idamService;
    private final CcdRequestDetails ccdRequestDetails;
    private final CoreCaseDataApi coreCaseDataApi;

    public CorCcdService(CreateCcdCaseService createCcdCaseService,
                         SearchCcdCaseService searchCcdCaseService,
                         UpdateCcdCaseService updateCcdCaseService,
                         ReadCcdCaseService readCcdCaseService,
                         CitizenCcdClient citizenCcdClient,
                         IdamService idamService,
                         CcdRequestDetails ccdRequestDetails,
                         CoreCaseDataApi coreCaseDataApi) {
        super(createCcdCaseService, searchCcdCaseService, updateCcdCaseService, readCcdCaseService);

        this.citizenCcdClient = citizenCcdClient;
        this.idamService = idamService;
        this.ccdRequestDetails = ccdRequestDetails;
        this.coreCaseDataApi = coreCaseDataApi;
    }

    public void addUserToCase(String userIdToAdd, long caseId) {
        IdamTokens idamTokens = idamService.getIdamTokens();
        citizenCcdClient.addUserToCase(
                idamTokens,
                userIdToAdd,
                caseId
        );
    }

    public void removeUserFromCase(String userIdToRemove, long caseId) {
        IdamTokens idamTokens = idamService.getIdamTokens();
        citizenCcdClient.removeUserFromCase(
                idamTokens,
                userIdToRemove,
                caseId
        );
    }

    public List<CaseDetails> searchForCitizen(IdamTokens idamTokens) {
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("sortDirection", "desc");
        return coreCaseDataApi.searchForCitizen(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                ccdRequestDetails.getJurisdictionId(),
                ccdRequestDetails.getCaseTypeId(),
                searchCriteria
        );
    }
}
