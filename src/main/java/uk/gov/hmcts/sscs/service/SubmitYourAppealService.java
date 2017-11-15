package uk.gov.hmcts.sscs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.Map;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.json.CcdCaseDeserializer;
import uk.gov.hmcts.sscs.tribunals.domain.corecase.CcdCase;

@Service
public class SubmitYourAppealService {

    private final CcdCaseDeserializer ccdCaseDeserializer;
    private final EmailService emailService;
    private final SubmitYourAppealEmail email;

    @Autowired
    public SubmitYourAppealService(CcdCaseDeserializer ccdCaseDeserializer,
                                   EmailService emailService, SubmitYourAppealEmail email) {
        this.ccdCaseDeserializer = ccdCaseDeserializer;
        this.emailService = emailService;
        this.email = email;
    }

    public void submitAppeal(Map<String, Object> appeal)  {
        convertJsonToCase(appeal);

        emailService.sendEmail(email);
    }

    public void convertJsonToCase(Map<String, Object> appeal) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(CcdCase.class, ccdCaseDeserializer);

        JSONObject json = new JSONObject(appeal);

        //TODO: Save the case to the database
        try {
            CcdCase ccd = mapper.readValue(json.toString(), CcdCase.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
