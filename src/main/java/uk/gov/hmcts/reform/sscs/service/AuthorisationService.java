package uk.gov.hmcts.reform.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import feign.FeignException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.sscs.service.exceptions.AuthorisationException;

@Service
public class AuthorisationService {

    private static final org.slf4j.Logger LOG = getLogger(AuthorisationService.class);
    public static final String SERVICE_AUTHORISATION_HEADER = "ServiceAuthorization";

    private final ServiceAuthorisationApi serviceAuthorisationApi;

    public AuthorisationService(ServiceAuthorisationApi serviceAuthorisationApi) {
        this.serviceAuthorisationApi = serviceAuthorisationApi;
    }

    public void authorise(String serviceAuthHeader) {
        try {
            LOG.info("About to authorise request");
            serviceAuthorisationApi.getServiceName(serviceAuthHeader);
            LOG.info("Request authorised");
        } catch (FeignException exc) {
            RuntimeException authExc = new AuthorisationException(exc);

            LOG.error("Authorisation failed with status " + exc.status(), authExc);

            throw authExc;
        }
    }
}
