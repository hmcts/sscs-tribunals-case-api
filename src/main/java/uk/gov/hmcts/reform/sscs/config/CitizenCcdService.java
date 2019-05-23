package uk.gov.hmcts.reform.sscs.config;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_ARCHIVED;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;

@Service
@Slf4j
public class CitizenCcdService {

    private final CitizenCcdClient citizenCcdClient;
    private final SscsCcdConvertService sscsCcdConvertService;
    private final CcdService ccdService;

    @Autowired
    CitizenCcdService(CitizenCcdClient citizenCcdClient,
                      SscsCcdConvertService sscsCcdConvertService,
                      CcdService ccdService) {
        this.citizenCcdClient = citizenCcdClient;
        this.sscsCcdConvertService = sscsCcdConvertService;
        this.ccdService = ccdService;
    }


    public List<SscsCaseData> findCase(IdamTokens idamTokens) {
        return citizenCcdClient.searchForCitizen(idamTokens)
            .stream()
            .map(f -> sscsCcdConvertService.getCaseData(f.getData()))
            .collect(Collectors.toList());
    }

    public SaveCaseResult saveCase(SscsCaseData caseData, IdamTokens idamTokens) {
        List<CaseDetails> caseDetailsList = citizenCcdClient.searchForCitizen(idamTokens);

        CaseDetails caseDetails;
        if (CollectionUtils.isNotEmpty(caseDetailsList)) {
            String caseId = caseDetailsList.get(0).getId().toString();
            caseDetails = updateCase(caseData, EventType.UPDATE_DRAFT.getCcdType(), "Update draft",
                "Update draft in CCD", idamTokens, caseId);
            return SaveCaseResult.builder()
                .caseDetailsId(caseDetails.getId())
                .saveCaseOperation(SaveCaseOperation.UPDATE)
                .build();
        } else {
            caseDetails = newCase(caseData, EventType.CREATE_DRAFT.getCcdType(), "Create draft",
                "Create draft in CCD", idamTokens);
            return SaveCaseResult.builder()
                .caseDetailsId(caseDetails.getId())
                .saveCaseOperation(SaveCaseOperation.CREATE)
                .build();
        }
    }

    public Optional<SscsCaseDetails> draftArchived(SscsCaseData caseData, IdamTokens citizenIdamTokens,
                                                   IdamTokens userIdamTokens) {
        List<CaseDetails> caseDetailsList = citizenCcdClient.searchForCitizen(citizenIdamTokens);

        if (!caseDetailsList.isEmpty()) {
            Long caseId = caseDetailsList.get(0).getId();

            SscsCaseDetails sscsCaseDetails = ccdService.updateCase(caseData, caseId, DRAFT_ARCHIVED.getCcdType(),
                    "SSCS - draft archived", "SSCS - draft archived", userIdamTokens);

            return Optional.of(sscsCaseDetails);
        }
        return Optional.empty();
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
