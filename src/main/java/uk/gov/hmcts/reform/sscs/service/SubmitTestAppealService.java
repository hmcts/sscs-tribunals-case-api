package uk.gov.hmcts.reform.sscs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
@ConditionalOnProperty("create_ccd_endpoint")
public class SubmitTestAppealService {
    private final CoreCaseDataApi coreCaseDataApi;
    private final SubmitAppealService submitAppealService;
    private final CcdClient ccdClient;
    private final IdamService idamService;
    private final SscsCcdConvertService sscsCcdConvertService;
    private final CcdRequestDetails ccdRequestDetails;

    @Autowired
    public SubmitTestAppealService(CoreCaseDataApi coreCaseDataApi,
                                   SubmitAppealService submitAppealService,
                                   CcdClient ccdClient,
                                   IdamService idamService,
                                   SscsCcdConvertService sscsCcdConvertService,
                                   CcdRequestDetails ccdRequestDetails) {
        this.coreCaseDataApi = coreCaseDataApi;
        this.submitAppealService = submitAppealService;
        this.ccdClient = ccdClient;
        this.idamService = idamService;
        this.sscsCcdConvertService = sscsCcdConvertService;
        this.ccdRequestDetails = ccdRequestDetails;
    }

    public Long submitAppeal(SyaCaseWrapper appeal, String userToken, String caseType) {
        IdamTokens idamTokens = idamService.getIdamTokens();

        SscsCaseData sscsCaseData = submitAppealService.convertAppealToSscsCaseData(appeal);
        EventType eventType = submitAppealService.findEventType(sscsCaseData, false);

        BenefitType benefitType = sscsCaseData.getAppeal() != null ? sscsCaseData.getAppeal().getBenefitType() : null;
        String nino = getAppellantNino(sscsCaseData);
        log.info("Creating test CCD case for Nino {} and benefit type {} with event {}", nino, benefitType, eventType);
        StartEventResponse startEventResponse = ccdClient.startCaseForCaseworker(idamTokens, eventType.getCcdType());

        CaseDataContent caseDataContent = sscsCcdConvertService.getCaseDataContent(sscsCaseData, startEventResponse,
                "SSCS - new test case created",
                "Created SSCS case from Submit Your Appeal online with event " + eventType.getCcdType());

        CaseDetails caseDetails = coreCaseDataApi.submitForCaseworker(idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                idamTokens.getUserId(),
                ccdRequestDetails.getJurisdictionId(),
                caseType,
                true,
                caseDataContent);
        log.info("Test case created with case id {} for nino {}", caseDetails.getId(), nino);
        SscsCaseDetails sscsCaseDetails = sscsCcdConvertService.getCaseDetails(caseDetails);
        submitAppealService.associateCase(idamTokens, sscsCaseDetails, userToken);
        return sscsCaseDetails.getId();
    }

    private String getAppellantNino(SscsCaseData caseData) {
        return caseData.getAppeal() != null && caseData.getAppeal().getAppellant() != null && caseData.getAppeal().getAppellant().getIdentity() != null ? caseData.getAppeal().getAppellant().getIdentity().getNino() : null;
    }

}
