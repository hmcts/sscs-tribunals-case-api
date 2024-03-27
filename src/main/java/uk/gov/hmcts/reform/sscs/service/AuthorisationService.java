package uk.gov.hmcts.reform.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.sscs.service.exceptions.AuthorisationException;
import uk.gov.hmcts.reform.sscs.service.exceptions.ClientAuthorisationException;

@Service
@Slf4j
public class AuthorisationService {

    private static final org.slf4j.Logger LOG = getLogger(AuthorisationService.class);
    public static final String SERVICE_AUTHORISATION_HEADER = "ServiceAuthorization";

    private final ServiceAuthorisationApi serviceAuthorisationApi;

    public AuthorisationService(ServiceAuthorisationApi serviceAuthorisationApi) {
        this.serviceAuthorisationApi = serviceAuthorisationApi;
    }

    public boolean authorise(String serviceAuthHeader) {
        try {
            LOG.info("About to authorise request");
            serviceAuthorisationApi.getServiceName(serviceAuthHeader);
            LOG.info("Request authorised");
            return true;
        } catch (FeignException exc) {
            RuntimeException authExc = (exc.status() >= 400 && exc.status() <= 499) ? new ClientAuthorisationException(exc) : new AuthorisationException(exc);

            log.error("Authorisation failed for Notification request with status " + exc.status(), authExc);

            throw authExc;
        }
    }
}
