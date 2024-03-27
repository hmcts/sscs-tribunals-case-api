package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.slf4j.LoggerFactory.getLogger;

import feign.FeignException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.AuthorisationException;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.ClientAuthorisationException;

@Service
public class AuthorisationService {

    private static final org.slf4j.Logger LOG = getLogger(AuthorisationService.class);
    public static final String SERVICE_AUTHORISATION_HEADER = "ServiceAuthorization";

    private final ServiceAuthorisationApi serviceAuthorisationApi;

    public AuthorisationService(ServiceAuthorisationApi serviceAuthorisationApi) {
        this.serviceAuthorisationApi = serviceAuthorisationApi;
    }

    public Boolean authorise(String serviceAuthHeader) {
        try {
            LOG.info("About to authorise Notification request");
            serviceAuthorisationApi.getServiceName(serviceAuthHeader);
            LOG.info("Notification request authorised");
            return true;
        } catch (FeignException exc) {
            RuntimeException authExc = (exc.status() >= 400 && exc.status() <= 499) ? new ClientAuthorisationException(exc) : new AuthorisationException(exc);

            LOG.error("Authorisation failed for Notification request with status " + exc.status(), authExc);

            throw authExc;
        }
    }
}
