package uk.gov.hmcts.reform.sscs.service.evidenceshare;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.sscs.exception.evidenceshare.AuthorisationException;
import uk.gov.hmcts.reform.sscs.exception.evidenceshare.ClientAuthorisationException;

@Service
@Slf4j
public class AuthorisationService {

    public static final String SERVICE_AUTHORISATION_HEADER = "ServiceAuthorization";

    private final ServiceAuthorisationApi serviceAuthorisationApi;

    public AuthorisationService(ServiceAuthorisationApi serviceAuthorisationApi) {
        this.serviceAuthorisationApi = serviceAuthorisationApi;
    }

    public Boolean authorise(String serviceAuthHeader) {
        try {
            log.info("About to authorise Notification request");
            serviceAuthorisationApi.getServiceName(serviceAuthHeader);
            log.info("Notification request authorised");
            return true;
        } catch (FeignException exc) {
            RuntimeException authExc = (exc.status() >= 400 && exc.status() <= 499) ? new ClientAuthorisationException(exc) : new AuthorisationException(exc);

            log.error("Authorisation failed for Notification request with status " + exc.status(), authExc);

            throw authExc;
        }
    }
}
