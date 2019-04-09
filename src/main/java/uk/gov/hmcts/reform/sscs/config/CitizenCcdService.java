package uk.gov.hmcts.reform.sscs.config;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class CitizenCcdService {

    private CitizenCcdClient citizenCcdClient;
    private SscsCcdConvertService sscsCcdConvertService;

    @Autowired
    CitizenCcdService(CitizenCcdClient citizenCcdClient, SscsCcdConvertService sscsCcdConvertService) {
        this.citizenCcdClient = citizenCcdClient;
        this.sscsCcdConvertService = sscsCcdConvertService;
    }


    public List<SscsCaseData> findCase(IdamTokens idamTokens) {
        return citizenCcdClient.searchForCitizen(idamTokens).stream().map(f -> sscsCcdConvertService.getCaseData(f.getData())).collect(Collectors.toList());
    }

    public SscsCaseDetails saveCase(SscsCaseData caseData, String eventType, String summary, String description, IdamTokens idamTokens) {

        List<CaseDetails> caseDetailsList = citizenCcdClient.searchForCitizen(idamTokens);

        CaseDetails caseDetails;
        if (caseDetailsList.size() > 0) {
            String caseId = caseDetailsList.get(0).getId().toString();
            caseDetails = updateCase(caseData, eventType, summary, description, idamTokens, caseId);
        } else {
            caseDetails = newCase(caseData, eventType, summary, description, idamTokens);
        }

        return sscsCcdConvertService.getCaseDetails(caseDetails);
    }

    private CaseDetails newCase(SscsCaseData caseData, String eventType, String summary, String description, IdamTokens idamTokens) {
        log.info("Creating a draft for a user.");
        CaseDetails caseDetails;
        StartEventResponse startEventResponse = citizenCcdClient.startCaseForCitizen(idamTokens, eventType);
        CaseDataContent caseDataContent = sscsCcdConvertService.getCaseDataContent(caseData, startEventResponse, summary, description);
        caseDetails = citizenCcdClient.submitForCitizen(idamTokens, caseDataContent);
        return caseDetails;
    }

    private CaseDetails updateCase(SscsCaseData caseData, String eventType, String summary, String description, IdamTokens idamTokens, String caseId) {
        log.info("Updating a draft with caseId {}.", caseId);
        CaseDetails caseDetails;
        StartEventResponse startEventResponse = citizenCcdClient.startEventForCitizen(idamTokens, caseId, eventType);
        CaseDataContent caseDataContent = sscsCcdConvertService.getCaseDataContent(caseData, startEventResponse, summary, description);
        caseDetails = citizenCcdClient.submitEventForCitizen(idamTokens, caseId, caseDataContent);
        return caseDetails;
    }
}
