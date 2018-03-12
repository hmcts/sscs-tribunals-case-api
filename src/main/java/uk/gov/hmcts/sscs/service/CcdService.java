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
import uk.gov.hmcts.sscs.domain.reminder.ReminderResponse;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.ccd.Subscription;
import uk.gov.hmcts.sscs.model.ccd.Subscriptions;
import uk.gov.hmcts.sscs.service.ccd.ReadCoreCaseDataService;

@Service
public class CcdService {
    private static final Logger LOG = getLogger(CcdService.class);

    private CoreCaseDataClient coreCaseDataClient;
    private AuthClient authClient;
    private IdamClient idamClient;
    private String caseWorkerId;
    private String userToken;
    private String serviceToken;
    private ReadCoreCaseDataService readCoreCaseDataService;

    @Autowired
    CcdService(CoreCaseDataClient coreCaseDataClient, AuthClient authClient,
               IdamClient idamClient,
               @Value("${ccd.case.worker.id}") String caseWorkerId,
               ReadCoreCaseDataService readCoreCaseDataService) {
        this.coreCaseDataClient = coreCaseDataClient;
        this.authClient = authClient;
        this.idamClient = idamClient;
        this.caseWorkerId = caseWorkerId;
        this.readCoreCaseDataService = readCoreCaseDataService;
    }

    public HttpStatus createCase(CcdCase ccdCase) throws CcdException {
        Map<String,Object> event = new HashMap<>();
        event.put("description","Creating sscs case");
        event.put("id","appealReceived");
        event.put("summary","Request to create an appeal case in ccd");

        String tokenPath = format("caseworkers/%s/jurisdictions/SSCS/case-types/Benefit/event-triggers/appealReceived/token", caseWorkerId);
        String ccdPath = format("caseworkers/%s/jurisdictions/SSCS/case-types/Benefit/cases", caseWorkerId);

        return buildRequest(ccdCase, tokenPath, ccdPath, event);
    }

    public HttpStatus createEvent(CcdCase ccdCase, ReminderResponse reminderResponse) throws CcdException {
        Map<String,Object> event = new HashMap<>();
        event.put("description","Creating sscs event");
        event.put("id", reminderResponse.getEventId());
        event.put("summary","Request to create an appeal event in ccd");

        String tokenPath = format("caseworkers/%s/jurisdictions/SSCS/case-types/Benefit/cases/%s/event-triggers/%s/token", caseWorkerId, reminderResponse.getCaseId(), reminderResponse.getEventId());
        String ccdPath = format("caseworkers/%s/jurisdictions/SSCS/case-types/Benefit/cases/%s/events", caseWorkerId, reminderResponse.getCaseId());

        return buildRequest(ccdCase, tokenPath, ccdPath, event);
    }

    protected HttpStatus buildRequest(CcdCase ccdCase, String tokenPath, String ccdPath, Map<String, Object> event) throws CcdException {
        ResponseEntity<Object> responseEntity = null;
        try {
            serviceToken = authClient.sendRequest("lease", POST, "");
            userToken = "Bearer " + idamClient.post("testing-support/lease");
            responseEntity = coreCaseDataClient.post(userToken, serviceToken, ccdPath, body(ccdCase, tokenPath, event));

        } catch (Exception ex) {
            LOG.error("Error while sending case to ccd", ex);
            throw new CcdException("Error while sending case to ccd" + ex.getMessage());
        }
        return responseEntity.getStatusCode();
    }

    protected Map<String,Object> body(CcdCase ccdCase, String tokenPath, Map<String, Object> event) throws CcdException {
        Map<String,Object> request = new HashMap<>();
        request.put("event", event);

        // Implement this properly
        request.put("data", ccdCase);

        request.put("event_token", startRequestAndGetToken(tokenPath));
        request.put("ignore_warning", true);
        return request;
    }

    public String startRequestAndGetToken(String tokenPath) throws CcdException {
        String token;
        try {
            ResponseEntity<Object> responseEntity = coreCaseDataClient
                    .sendRequest(userToken, serviceToken,
                            tokenPath, HttpMethod.GET, new HashMap<>());
            Map<String,Object> response = (Map<String, Object>) responseEntity.getBody();
            token = (String) response.get("token");
        } catch (Exception ex) {
            LOG.error("Error while getting case caseToken", ex);
            throw new CcdException("Error while getting case caseToken: " + ex.getMessage());
        }
        return token;
    }

    public CaseData findCcdCaseByAppealNumber(String appealNumber) throws CcdException {

        try {
            return readCoreCaseDataService.getCcdCaseData(appealNumber);
        } catch (Exception ex) {
            LOG.error("Error while getting case from ccd", ex);
            throw new CcdException("Error while getting case from ccd" + ex.getMessage());
        }
    }

    public String unsubscribe(String appealNumber, String reason) throws CcdException {

        String benefitType = null;
        try {
            CaseData caseData = findCcdCaseByAppealNumber(appealNumber);
            //The following need to be implemented as per CCD def for subscriptions
            Subscription subscription = caseData.getSubscriptions().getAppellantSubscription();
            subscription.toBuilder().subscribeEmail("No").subscribeSms("No").reason(reason);
            caseData.getSubscriptions().toBuilder().appellantSubscription(subscription).build();

            //createCase(caseData);
            benefitType = caseData.getAppeal().getBenefitType().getCode();
        } catch (Exception ex) {
            LOG.error("Error while unsubscribing case from ccd: ", ex);
            throw new CcdException("Error while unsubscribing case from ccd: " + ex.getMessage());
        }
        return benefitType != null ? benefitType.toLowerCase() : "";
    }

    public String updateSubscription(String appealNumber, Subscription subscription) throws CcdException {
        String benefitType = null;
        try {
            CaseData caseData = findCcdCaseByAppealNumber(appealNumber);
            //The following need to be implemented as per CCD def for subscriptions
            Subscriptions subscriptions = Subscriptions.builder()
                    .appellantSubscription(subscription)
                    .build();
            caseData.toBuilder().subscriptions(subscriptions);
            //createCase(ccdCase);
            benefitType = caseData.getAppeal().getBenefitType().getCode();
        } catch (Exception ex) {
            LOG.error("Error while updating subscription details in ccd: ", ex);
            throw new CcdException("Error while updating case in ccd: " + ex.getMessage());
        }
        return benefitType != null ? benefitType.toLowerCase() : "";
    }

    public CaseData findCcdCaseByAppealNumberAndSurname(String appealNumber, String surname) throws CcdException {
        CaseData caseData = findCcdCaseByAppealNumber(appealNumber);
        return caseData.getAppeal().getAppellant().getName().getLastName().equals(surname) ? caseData : null;

    }
}
