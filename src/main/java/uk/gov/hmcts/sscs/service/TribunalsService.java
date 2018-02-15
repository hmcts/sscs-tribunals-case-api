package uk.gov.hmcts.sscs.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.builder.TrackYourAppealJsonBuilder;
import uk.gov.hmcts.sscs.domain.corecase.Appeal;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.domain.corecase.Subscription;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.service.exceptions.InvalidSurnameException;
import uk.gov.hmcts.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDeserializer;

@Service
public class TribunalsService {
    private CcdService ccdService;
    private EmailService emailService;
    private SubmitYourAppealEmail email;
    private SubmitYourAppealToCcdCaseDeserializer transformer;
    private AppealNumberGenerator appealNumberGenerator;

    @Autowired
    TribunalsService(CcdService ccdService,
                     EmailService emailService, SubmitYourAppealEmail email,
                     SubmitYourAppealToCcdCaseDeserializer transformer, AppealNumberGenerator appealNumberGenerator) {
        this.ccdService = ccdService;
        this.emailService = emailService;
        this.email = email;
        this.transformer = transformer;
        this.appealNumberGenerator = appealNumberGenerator;
    }

    public HttpStatus submitAppeal(SyaCaseWrapper syaCaseWrapper) throws CcdException {
        CcdCase ccdCase = transformer.convertSyaToCcdCase(syaCaseWrapper);
        String appealNumber = appealNumberGenerator.generate();
        HttpStatus status = saveAppeal(ccdCase, appealNumber);
        emailService.sendEmail(email);
        return status;
    }

    public HttpStatus saveAppeal(CcdCase ccdCase, String appealNumber) throws CcdException {
        Appeal appeal = ccdCase.getAppeal();
        appeal.setAppealNumber(appealNumber);
        ccdCase.setAppeal(appeal);
        return ccdService.createCase(ccdCase);
    }

    public ObjectNode findAppeal(String appealNumber) throws CcdException {
        return TrackYourAppealJsonBuilder.buildTrackYourAppealJson(
                ccdService.findCcdCaseByAppealNumber(appealNumber));
    }

    public String unsubscribe(String appealNumber, String reason) throws CcdException {
        return ccdService.unsubscribe(appealNumber, reason);
    }

    public String updateSubscription(String appealNumber, Subscription subscription) throws CcdException {
        return ccdService.updateSubscription(appealNumber, subscription);
    }

    public boolean validateSurname(String appealNumber, String surname) throws CcdException {
        CcdCase ccdCase = ccdService.findCcdCaseByAppealNumberAndSurname(appealNumber, surname);
        if (ccdCase == null) {
            throw new InvalidSurnameException();
        }
        return true;
    }
}
