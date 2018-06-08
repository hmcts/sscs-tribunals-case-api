package uk.gov.hmcts.sscs.service.evidence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory;

@Component("em")
public class EvidenceManagementAuthTokenGenerator implements AuthTokenGenerator {

    private final String emAuthSecret;
    private final String emAuthMicroService;
    private final ServiceAuthorisationApi serviceAuthorisationApi;

    @Autowired
    public EvidenceManagementAuthTokenGenerator(
        @Value("${idam.em.s2s-auth.totp_secret}") String emAuthSecret,
        @Value("${idam.em.s2s-auth.microservice}") String emAuthMicroService,
        ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        this.emAuthSecret = emAuthSecret;
        this.emAuthMicroService = emAuthMicroService;
        this.serviceAuthorisationApi = serviceAuthorisationApi;
    }

    public String generate() {

        AuthTokenGenerator authTokenGenerator =
            AuthTokenGeneratorFactory.createDefaultGenerator(
                emAuthSecret,
                emAuthMicroService,
                serviceAuthorisationApi
            );

        return authTokenGenerator.generate();
    }

}
