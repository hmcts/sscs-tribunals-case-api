package uk.gov.hmcts.sscs.service;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpMethod.POST;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.transform.SubmitYourAppealToCcdCaseTransformer;

@Service
public class CcdService {
    private static final Logger LOG = getLogger(CcdService.class);

    private CoreCaseDataClient coreCaseDataClient;
    private AuthClient authClient;
    private IdamClient idamClient;
    private String caseWorkerId;
    private String userToken;
    private String serviceToken;
    private EmailService emailService;
    private SubmitYourAppealEmail email;
    private SubmitYourAppealToCcdCaseTransformer transformer;

    @Autowired
    CcdService(CoreCaseDataClient coreCaseDataClient, AuthClient authClient,
               IdamClient idamClient,
               @Value("${ccd.case.worker.id}") String caseWorkerId,
               EmailService emailService, SubmitYourAppealEmail email,
               SubmitYourAppealToCcdCaseTransformer transformer) {
        this.coreCaseDataClient = coreCaseDataClient;
        this.authClient = authClient;
        this.idamClient = idamClient;
        this.caseWorkerId = caseWorkerId;
        this.emailService = emailService;
        this.email = email;
        this.transformer = transformer;
    }

    public HttpStatus submitAppeal(SyaCaseWrapper syaCaseWrapper) throws CcdException {
        CcdCase ccdCase = transformer.convertSyaToCcdCase(syaCaseWrapper);

        emailService.sendEmail(email);

        HttpStatus status = saveCase(ccdCase);

        return status;
    }

    public HttpStatus saveCase(CcdCase ccdCase) throws CcdException {
        ResponseEntity<Object> responseEntity = null;
        try {
            serviceToken = authClient.sendRequest("lease", POST, "");
            userToken = "Bearer " + idamClient.post("testing-support/lease");
            String url = "caseworkers/%s/jurisdictions/SSCS/case-types/Benefit/cases";
            String ccdPath = format(url, caseWorkerId);
            responseEntity = coreCaseDataClient
                    .post(userToken,serviceToken,ccdPath,body(ccdCase));
        } catch (Exception ex) {
            LOG.error("Error while sending case to ccd", ex);
            throw new CcdException("Error while sending case to ccd" + ex.getMessage());
        }
        return responseEntity.getStatusCode();
    }

    protected Map<String,Object> body(CcdCase ccdCase) throws CcdException {
        Map<String,Object> event = new HashMap<>();
        event.put("description","Creating sscs case");
        event.put("id","appealReceived");
        event.put("summary","Request to create an appeal case in ccd");
        Map<String,Object> request = new HashMap<>();
        request.put("event", event);
        //TODO: Implement this properly
        request.put("data", ccdCase);
        request.put("event_token", caseToken());
        request.put("ignore_warning", true);
        return request;
    }

    public String caseToken() throws CcdException {
        String token = null;
        try {
            String url = "caseworkers/%s/jurisdictions/SSCS/case-types/"
                    +
                    "Benefit/event-triggers/appealReceived/token";
            String ccdPath = format(url, caseWorkerId);
            ResponseEntity<Object> responseEntity = coreCaseDataClient
                    .sendRequest(userToken, serviceToken,
                            ccdPath, HttpMethod.GET, new HashMap<>());
            Map<String,Object> response = (Map<String, Object>) responseEntity.getBody();
            token = (String) response.get("token");
        } catch (Exception ex) {
            LOG.error("Error while getting case caseToken", ex);
            throw new CcdException("Error while getting case caseToken: " + ex.getMessage());
        }
        return token;
    }
}
