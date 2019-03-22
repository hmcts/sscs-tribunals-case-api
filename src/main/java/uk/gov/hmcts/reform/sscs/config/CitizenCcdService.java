package uk.gov.hmcts.reform.sscs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.CreateCcdCaseException;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class CitizenCcdService {

    private CitizenCcdClient citizenCcdClient;
    private SscsCcdConvertService sscsCcdConvertService;

    @Autowired
    public CitizenCcdService(CitizenCcdClient citizenCcdClient, SscsCcdConvertService sscsCcdConvertService) {
        this.citizenCcdClient = citizenCcdClient;
        this.sscsCcdConvertService = sscsCcdConvertService;
    }

    public SscsCaseDetails createCase(SscsCaseData caseData, String eventType, String summary, String description, IdamTokens idamTokens) {
        log.info("Starting create case process with SC number {} and ccdID {} and eventType {} ...",
                caseData.getCaseReference(), caseData.getCcdCaseId(), eventType);
        try {
            return createCaseInCcd(caseData, eventType, summary, description, idamTokens);
        } catch (Exception e) {
            throw new CreateCcdCaseException(String.format(
                    "Error found in the case creation or callback process for the ccd case "
                            + "with SC (%s) and ccdID (%s) and Nino (%s) and Benefit Type (%s) and exception: (%s) ",
                    caseData.getCaseReference(), caseData.getCcdCaseId(), caseData.getGeneratedNino(),
                    caseData.getAppeal().getBenefitType().getCode(), e.getMessage()), e);
        }
    }

    private SscsCaseDetails createCaseInCcd(SscsCaseData caseData, String eventType, String summary, String description, IdamTokens idamTokens) {
        BenefitType benefitType = caseData.getAppeal() != null ? caseData.getAppeal().getBenefitType() : null;
        log.info("Creating CCD case for Nino {} and benefit type {} with event {}", caseData.getGeneratedNino(), benefitType, eventType);

        StartEventResponse startEventResponse = citizenCcdClient.startCaseForCitizen(idamTokens, eventType);

        CaseDataContent caseDataContent = sscsCcdConvertService.getCaseDataContent(caseData, startEventResponse, summary, description);
        CaseDetails caseDetails = citizenCcdClient.submitForCitizen(idamTokens, caseDataContent);

        return sscsCcdConvertService.getCaseDetails(caseDetails);
    }
}
