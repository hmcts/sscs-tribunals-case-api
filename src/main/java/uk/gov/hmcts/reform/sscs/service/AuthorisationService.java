package uk.gov.hmcts.reform.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.base.Splitter;
import feign.FeignException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.UnauthorizedException;
import uk.gov.hmcts.reform.sscs.service.exceptions.AuthorisationException;
import uk.gov.hmcts.reform.sscs.service.exceptions.ClientAuthorisationException;

@Service
@Slf4j
public class AuthorisationService {
    private static final org.slf4j.Logger LOG = getLogger(AuthorisationService.class);
    public static final String SERVICE_AUTHORISATION_HEADER = "ServiceAuthorization";
    private final List<String> allowedServices;
    private final ServiceAuthorisationApi serviceAuthorisationApi;

    public AuthorisationService(ServiceAuthorisationApi serviceAuthorisationApi,
                                @Value("${allowed-services-for-callback}") String configuredServicesCsv) {
        this.serviceAuthorisationApi = serviceAuthorisationApi;
        this.allowedServices = Splitter.on(",").splitToList(configuredServicesCsv);
    }

    public boolean authorise(String serviceAuthHeader) {
        try {
            LOG.info("About to authorise request");
            serviceAuthorisationApi.getServiceName(serviceAuthHeader);
            LOG.info("Request authorised");
            return true;
        } catch (FeignException exc) {
            RuntimeException authExc = (exc.status() >= 400 && exc.status() <= 499) ? new ClientAuthorisationException(exc) : new AuthorisationException(exc);

            log.error("Authorisation failed for Notification request with status {}", exc.status(), authExc);

            throw authExc;
        }
    }

    public String authenticate(String authHeader) {
        if (authHeader == null) {
            throw new UnauthorizedException("Missing ServiceAuthorization header");
        }
        try {
            return serviceAuthorisationApi.getServiceName(authHeader);
        } catch (FeignException exc) {
            RuntimeException authExc = (exc.status() >= 400 && exc.status() <= 499) ? new ClientAuthorisationException(exc) : new AuthorisationException(exc);
            log.error("Authorisation failed for authenticate request with status {}", exc.status(), authExc);
            throw authExc;
        }
    }

    public void assertIsAllowedToHandleCallback(String serviceName) {
        if (!allowedServices.contains(serviceName)) {
            throw new ForbiddenException(
                "Service " + serviceName + " does not have permissions to request case creation"
            );
        }
    }
}
