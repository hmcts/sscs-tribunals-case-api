package uk.gov.hmcts.sscs.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.builder.TrackYourAppealJsonBuilder;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDeserializer;

@Service
public class TribunalsService {
    private CcdService ccdService;
    private EmailService emailService;
    private SubmitYourAppealEmail email;
    private SubmitYourAppealToCcdCaseDeserializer transformer;

    @Autowired
    TribunalsService(CcdService ccdService,
                     EmailService emailService, SubmitYourAppealEmail email,
                     SubmitYourAppealToCcdCaseDeserializer transformer) {
        this.ccdService = ccdService;
        this.emailService = emailService;
        this.email = email;
        this.transformer = transformer;
    }

    public HttpStatus submitAppeal(SyaCaseWrapper syaCaseWrapper) throws CcdException {
        CcdCase ccdCase = transformer.convertSyaToCcdCase(syaCaseWrapper);

        emailService.sendEmail(email);

        HttpStatus status = ccdService.createCase(ccdCase);

        return status;
    }
    
    public ObjectNode findAppeal(String appealNumber) throws CcdException {
        return TrackYourAppealJsonBuilder.buildTrackYourAppealJson(
                ccdService.findCcdCaseByAppealNumber(appealNumber));
    }
}
