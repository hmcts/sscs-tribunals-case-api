package uk.gov.hmcts.reform.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.AuthCheckerException;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.BearerTokenInvalidException;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.BearerTokenMissingException;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.UnauthorisedServiceException;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.parser.idam.core.service.token.ServiceTokenInvalidException;
import uk.gov.hmcts.reform.auth.parser.idam.core.service.token.ServiceTokenParsingException;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.sscs.bulkscan.exceptions.UnauthorizedException;
import uk.gov.hmcts.reform.sscs.service.exceptions.AuthorisationException;
import uk.gov.hmcts.reform.sscs.service.exceptions.ClientAuthorisationException;

@Component
@Slf4j
public class AuthorisationService implements RequestAuthorizer<Service> {

    private static final Logger LOG = getLogger(AuthorisationService.class);
    public static final String SERVICE_AUTHORISATION_HEADER = "ServiceAuthorization";
    public static final String SSCS = "sscs";
    public static final String CCD = "ccd_data";
    public static final String BULK_SCAN_PROC = "bulk_scan_processor";
    public static final String BULK_SCAN_ORCH = "bulk_scan_orchestrator";

    private final List<String> allowedServices;
    private final List<String> sscsOnlyEndpoints;
    private final List<String> ccdOnlyEndpoints;
    private final List<String> bulkScanOnlyEndpoints;

    private final ServiceAuthorisationApi serviceAuthorisationApi;
    private final SubjectResolver<Service> serviceResolver;

    public AuthorisationService(ServiceAuthorisationApi serviceAuthorisationApi,
                                SubjectResolver<Service> serviceResolver,
                                @Value("${allowed-services-for-callback}") List<String> allowedServices,
                                @Value("${s2s.service-to-endpoint-mapping.sscs}") List<String> sscsOnlyEndpoints,
                                @Value("${s2s.service-to-endpoint-mapping.ccd}") List<String> ccdOnlyEndpoints,
                                @Value("${s2s.service-to-endpoint-mapping.bulkscan}")
                                List<String> bulkScanOnlyEndpoints) {
        this.serviceAuthorisationApi = serviceAuthorisationApi;
        this.allowedServices = allowedServices;
        this.sscsOnlyEndpoints = sscsOnlyEndpoints;
        this.ccdOnlyEndpoints = ccdOnlyEndpoints;
        this.bulkScanOnlyEndpoints = bulkScanOnlyEndpoints;
        this.serviceResolver = serviceResolver;
    }

    public boolean authorise(String serviceAuthHeader) {
        try {
            LOG.info("About to authorise request");
            serviceAuthorisationApi.getServiceName(serviceAuthHeader);
            LOG.info("Request authorised");
            return true;
        } catch (FeignException exc) {
            RuntimeException authExc = exc.status() >= 400 && exc.status() <= 499
                    ? new ClientAuthorisationException(exc) : new AuthorisationException(exc);
            log.error("Authorisation failed for Notification request with status {}", exc.status(), authExc);
            throw authExc;
        }
    }

    @Override
    public Service authorise(HttpServletRequest request) throws UnauthorisedServiceException {
        String bearerToken = request.getHeader(SERVICE_AUTHORISATION_HEADER);
        if (bearerToken == null) {
            throw new BearerTokenMissingException();
        }

        Service service = getTokenDetails(bearerToken);
        if (!isAllowedService(service.getPrincipal().toLowerCase(), request.getRequestURI())) {
            throw new UnauthorisedServiceException();
        }
        return service;
    }

    public void assertIsAllowedToHandleCallback(String serviceAuthorisation) {
        String serviceName = authenticate(serviceAuthorisation);
        if (!allowedServices.contains(serviceName)) {
            throw new ForbiddenException(
                "Service " + serviceName + " does not have permissions to request case creation"
            );
        }
    }

    private String authenticate(String authHeader) {
        if (authHeader == null) {
            throw new UnauthorizedException("Missing ServiceAuthorization header");
        }
        try {
            var serviceName = serviceAuthorisationApi.getServiceName(authHeader);
            log.info("Authorising service {} to access endpoint", serviceName);
            return  serviceName;
        } catch (FeignException exc) {
            RuntimeException authExc = (exc.status() >= 400 && exc.status() <= 499)
                    ? new ClientAuthorisationException(exc) : new AuthorisationException(exc);
            log.error("Authorisation failed for authenticate request with status {}", exc.status(), authExc);
            throw authExc;
        }
    }

    private Service getTokenDetails(String bearerToken) {
        try {
            return serviceResolver.getTokenDetails(bearerToken);
        } catch (ServiceTokenInvalidException e) {
            throw new BearerTokenInvalidException(e);
        } catch (ServiceTokenParsingException e) {
            throw new AuthCheckerException("Error parsing JWT token", e);
        }
    }

    private boolean isAllowedService(String serviceName, String uri) {
        return switch (serviceName) {
            case SSCS -> sscsOnlyEndpoints.stream().anyMatch(uri::startsWith);
            case CCD -> ccdOnlyEndpoints.stream().anyMatch(uri::startsWith);
            case BULK_SCAN_ORCH, BULK_SCAN_PROC -> bulkScanOnlyEndpoints.stream().anyMatch(uri::startsWith);
            default -> false;
        };
    }
}
