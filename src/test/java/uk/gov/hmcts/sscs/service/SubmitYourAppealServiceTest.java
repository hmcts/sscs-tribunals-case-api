package uk.gov.hmcts.sscs.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.json.CcdCaseDeserializer;

@RunWith(MockitoJUnitRunner.class)
public class SubmitYourAppealServiceTest {

    @Mock
    private CcdCaseDeserializer ccdCaseDeserializer;

    @Mock
    private EmailService emailService;

    private SubmitYourAppealEmail email = new SubmitYourAppealEmail("from@hmcts.net",
            "to@hmcts.net", "Your appeal", "Your appeal has been created");

    private SubmitYourAppealService service;

    @Before
    public void setUp() {
        service = new SubmitYourAppealService(ccdCaseDeserializer, emailService, email);
    }

    @Test
    public void shoulSendSubmitYourAppealEmail() throws IOException {

        Map<String, Object> appealData = new HashMap<>();
        appealData.put("benefit_name", "Personal Independence Payment (PIP)");

        service.submitAppeal(appealData);

        verify(emailService).sendEmail(any(SubmitYourAppealEmail.class));
    }
}