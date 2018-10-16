package uk.gov.hmcts.reform.sscs.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.sscs.builder.TrackYourAppealJsonBuilder;
import uk.gov.hmcts.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.sscs.model.tya.SubscriptionRequest;
import uk.gov.hmcts.sscs.model.tya.SurnameResponse;
import uk.gov.hmcts.sscs.service.exceptions.InvalidSurnameException;
import uk.gov.hmcts.sscs.service.referencedata.RegionalProcessingCenterService;

@Service
@Slf4j
public class TribunalsService {
    private CcdService ccdService;
    private RegionalProcessingCenterService regionalProcessingCenterService;
    private TrackYourAppealJsonBuilder trackYourAppealJsonBuilder;
    private IdamService idamService;

    @Autowired
    TribunalsService(CcdService ccdService,
                     RegionalProcessingCenterService regionalProcessingCenterService,
                     TrackYourAppealJsonBuilder trackYourAppealJsonBuilder,
                     IdamService idamService) {
        this.ccdService = ccdService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.trackYourAppealJsonBuilder = trackYourAppealJsonBuilder;
        this.idamService = idamService;
    }

    public ObjectNode findAppeal(String appealNumber) {
        SscsCaseDetails caseByAppealNumber = ccdService.findCaseByAppealNumber(appealNumber, idamService.getIdamTokens());
        if (caseByAppealNumber == null) {
            log.info("Appeal does not exist for appeal number: " + appealNumber);
            throw new AppealNotFoundException(appealNumber);
        }

        return trackYourAppealJsonBuilder.build(caseByAppealNumber.getData(), getRegionalProcessingCenter(caseByAppealNumber.getData()), caseByAppealNumber.getId());
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

    public SurnameResponse validateSurname(String appealNumber, String surname) {
        SscsCaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(appealNumber, surname, idamService.getIdamTokens());
        if (caseData == null) {
            log.info("Not a valid surname: " + surname);
            throw new InvalidSurnameException();
        }
        return new SurnameResponse(caseData.getCcdCaseId(), appealNumber, surname);
    }
}
