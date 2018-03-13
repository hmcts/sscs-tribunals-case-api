package uk.gov.hmcts.sscs.service;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.builder.TrackYourAppealJsonBuilder;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.ccd.Subscription;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;
import uk.gov.hmcts.sscs.service.exceptions.InvalidSurnameException;
import uk.gov.hmcts.sscs.service.referencedata.RegionalProcessingCenterService;
import uk.gov.hmcts.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;

@Service
public class TribunalsService {
    private CcdService ccdService;
    private EmailService emailService;
    private SubmitYourAppealEmail email;
    private SubmitYourAppealToCcdCaseDataDeserializer transformer;
    private AppealNumberGenerator appealNumberGenerator;
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Autowired
    TribunalsService(CcdService ccdService,
                     EmailService emailService, SubmitYourAppealEmail email,
                     SubmitYourAppealToCcdCaseDataDeserializer transformer,
                     AppealNumberGenerator appealNumberGenerator,
                     RegionalProcessingCenterService regionalProcessingCenterService) {
        this.ccdService = ccdService;
        this.emailService = emailService;
        this.email = email;
        this.transformer = transformer;
        this.appealNumberGenerator = appealNumberGenerator;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
    }

    public CaseDetails submitAppeal(SyaCaseWrapper syaCaseWrapper) throws CcdException {
        CaseData caseData = transformer.convertSyaToCcdCaseData(syaCaseWrapper);
        String appealNumber = appealNumberGenerator.generate();
        caseData.getSubscriptions().getAppellantSubscription().toBuilder().tya(appealNumber);
        CaseDetails caseDetails = saveAppeal(caseData);
        emailService.sendEmail(email);
        return caseDetails;
    }

    public CaseDetails saveAppeal(CaseData caseData) throws CcdException {
        return ccdService.createCase(caseData);
    }

    public ObjectNode findAppeal(String appealNumber) throws CcdException {
        CaseData caseByAppealNumber = ccdService.findCcdCaseByAppealNumber(appealNumber);
        RegionalProcessingCenter regionalProcessingCenter =
                regionalProcessingCenterService.getByScReferenceCode(caseByAppealNumber.getCaseReference());
        return TrackYourAppealJsonBuilder.buildTrackYourAppealJson(
                caseByAppealNumber, regionalProcessingCenter);
    }

    public String unsubscribe(String appealNumber, String reason) throws CcdException {
        return ccdService.unsubscribe(appealNumber, reason);
    }

    public String updateSubscription(String appealNumber, Subscription subscription) throws CcdException {
        return ccdService.updateSubscription(appealNumber, subscription);
    }

    public boolean validateSurname(String appealNumber, String surname) throws CcdException {
        CaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(appealNumber, surname);
        if (caseData == null) {
            throw new InvalidSurnameException();
        }
        return true;
    }
}
