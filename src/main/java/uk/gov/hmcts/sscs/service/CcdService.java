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

import rx.Subscription;
import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.domain.corecase.CcdCaseResponse;
import uk.gov.hmcts.sscs.exception.CcdException;

@Service
public class CcdService {
    private static final Logger LOG = getLogger(CcdService.class);

    private CoreCaseDataClient coreCaseDataClient;
    private AuthClient authClient;
    private IdamClient idamClient;
    private String caseWorkerId;
    private String userToken;
    private String serviceToken;


    @Autowired
    CcdService(CoreCaseDataClient coreCaseDataClient, AuthClient authClient,
               IdamClient idamClient,
               @Value("${ccd.case.worker.id}") String caseWorkerId) {
        this.coreCaseDataClient = coreCaseDataClient;
        this.authClient = authClient;
        this.idamClient = idamClient;
        this.caseWorkerId = caseWorkerId;
    }

    public HttpStatus createCase(CcdCase ccdCase) throws CcdException {
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

        // Implement this properly
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

    public CcdCase findCcdCaseByAppealNumber(String appealNumber) throws CcdException {

        ResponseEntity<CcdCaseResponse> responseEntity = null;
        try {
            serviceToken = authClient.sendRequest("lease", POST, "");
            userToken = "Bearer " + idamClient.post("testing-support/lease");
            String url = "caseworkers/%s/jurisdictions/SSCS/case-types/Benefit/cases/%s";
            String ccdPath = format(url, caseWorkerId, appealNumber);
            responseEntity = coreCaseDataClient.get(userToken, serviceToken, ccdPath);
        } catch (Exception ex) {
            LOG.error("Error while getting case from ccd", ex);
            throw new CcdException("Error while getting case from ccd" + ex.getMessage());
        }
        return responseEntity.getBody().getCaseData();
    }

    public String unsubscribe(String appealNumber) throws CcdException {

        String benefitType = null;
        try {
            CcdCase ccdCase = findCcdCaseByAppealNumber(appealNumber);
            ccdCase.setIsEmailSubscribe(Boolean.FALSE);
            ccdCase.setIsMobileSubscribe(Boolean.FALSE);
            createCase(ccdCase);
            benefitType = ccdCase.getBenefitType();
        } catch (Exception ex) {
            LOG.error("Error while unsubscribing case from ccd", ex);
            throw new CcdException("Error while unsubscribing case from ccd" + ex.getMessage());
        }
        return benefitType != null ? benefitType.toLowerCase() : "";
    }
}
