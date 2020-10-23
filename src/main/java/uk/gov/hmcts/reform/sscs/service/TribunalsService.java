package uk.gov.hmcts.reform.sscs.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.builder.TrackYourAppealJsonBuilder;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.tya.SubscriptionRequest;

@Service
@Slf4j
public class TribunalsService {
    private final CcdService ccdService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final TrackYourAppealJsonBuilder trackYourAppealJsonBuilder;
    private final IdamService idamService;
    private final CcdClient ccdClient;
    private final SscsCcdConvertService sscsCcdConvertService;

    @Autowired
    TribunalsService(CcdService ccdService,
                     RegionalProcessingCenterService regionalProcessingCenterService,
                     TrackYourAppealJsonBuilder trackYourAppealJsonBuilder,
                     IdamService idamService,
                     CcdClient ccdClient,
                     SscsCcdConvertService sscsCcdConvertService) {
        this.ccdService = ccdService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.trackYourAppealJsonBuilder = trackYourAppealJsonBuilder;
        this.idamService = idamService;
        this.ccdClient = ccdClient;
        this.sscsCcdConvertService = sscsCcdConvertService;
    }

    public ObjectNode findAppeal(Long caseId, boolean mya) {
        CaseDetails caseDetails = ccdClient.readForCaseworker(idamService.getIdamTokens(), caseId);
        SscsCaseDetails sscsCaseDetails = sscsCcdConvertService.getCaseDetails(caseDetails);

        if (sscsCaseDetails == null) {
            log.info("Appeal does not exist for case id: " + caseId);
            throw new AppealNotFoundException(caseId);
        }

        return trackYourAppealJsonBuilder.build(sscsCaseDetails.getData(),
                getRegionalProcessingCenter(sscsCaseDetails.getData()), sscsCaseDetails.getId(), mya, caseDetails.getState());
    }

    private RegionalProcessingCenter getRegionalProcessingCenter(SscsCaseData caseByAppealNumber) {
        RegionalProcessingCenter regionalProcessingCenter;

        if (null == caseByAppealNumber.getRegionalProcessingCenter()) {
            regionalProcessingCenter =
                    regionalProcessingCenterService.getByScReferenceCode(caseByAppealNumber.getCaseReference());
        } else {
            regionalProcessingCenter = caseByAppealNumber.getRegionalProcessingCenter();
        }
        return regionalProcessingCenter;
    }

    public String unsubscribe(String appealNumber) {
        SscsCaseDetails sscsCaseDetails = ccdService.updateSubscription(appealNumber, null, idamService.getIdamTokens());

        return sscsCaseDetails != null ? sscsCaseDetails.getData().getAppeal().getBenefitType().getCode().toLowerCase() : "";
    }

    public String updateSubscription(String appealNumber, SubscriptionRequest subscriptionRequest) {
        SscsCaseDetails sscsCaseDetails = ccdService.updateSubscription(appealNumber, subscriptionRequest.getEmail(), idamService.getIdamTokens());

        return sscsCaseDetails != null ? sscsCaseDetails.getData().getAppeal().getBenefitType().getCode().toLowerCase() : "";
    }
}
