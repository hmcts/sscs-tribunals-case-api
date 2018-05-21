package uk.gov.hmcts.sscs.service;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.sscs.builder.TrackYourAppealJsonBuilder;
import uk.gov.hmcts.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;
import uk.gov.hmcts.sscs.model.tya.SubscriptionRequest;
import uk.gov.hmcts.sscs.service.exceptions.InvalidSurnameException;
import uk.gov.hmcts.sscs.service.referencedata.RegionalProcessingCenterService;

@Service
@Slf4j
public class TribunalsService {
    private CcdService ccdService;
    private RegionalProcessingCenterService regionalProcessingCenterService;
    private TrackYourAppealJsonBuilder trackYourAppealJsonBuilder;

    @Autowired
    TribunalsService(CcdService ccdService,
                     RegionalProcessingCenterService regionalProcessingCenterService,
                     TrackYourAppealJsonBuilder trackYourAppealJsonBuilder) {
        this.ccdService = ccdService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.trackYourAppealJsonBuilder = trackYourAppealJsonBuilder;
    }

    public ObjectNode findAppeal(String appealNumber) {
        CaseData caseByAppealNumber = ccdService.findCcdCaseByAppealNumber(appealNumber);
        if (caseByAppealNumber == null) {
            log.info("Appeal not exists for appeal number: " + appealNumber);
            throw new AppealNotFoundException(appealNumber);
        }

        return trackYourAppealJsonBuilder.build(caseByAppealNumber, getRegionalProcessingCenter(caseByAppealNumber));
    }

    private RegionalProcessingCenter getRegionalProcessingCenter(CaseData caseByAppealNumber) {
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
        return ccdService.unsubscribe(appealNumber);
    }

    public String updateSubscription(String appealNumber, SubscriptionRequest subscriptionRequest) {
        return ccdService.updateSubscription(appealNumber, subscriptionRequest);
    }

    public boolean validateSurname(String appealNumber, String surname) {
        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(appealNumber, surname);
        if (caseData == null) {
            log.info("Not a valid surname: " + surname);
            throw new InvalidSurnameException();
        }
        return true;
    }
}
