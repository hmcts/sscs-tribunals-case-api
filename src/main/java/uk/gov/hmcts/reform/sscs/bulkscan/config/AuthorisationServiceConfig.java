package uk.gov.hmcts.reform.sscs.bulkscan.config;

import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;

@Configuration
@Lazy
@EnableFeignClients(basePackageClasses = ServiceAuthorisationApi.class)
public class AuthorisationServiceConfig {

    @Bean
    @ConditionalOnProperty(name = "idam.s2s-auth.url")
    public AuthTokenValidator tokenValidator(ServiceAuthorisationApi s2sApi) {
        return new ServiceAuthTokenValidator(s2sApi);
    }

    @Bean
    @ConditionalOnProperty(name = "idam.s2s-auth.url", havingValue = "false")
    public AuthTokenValidator tokenValidatorStub() {
        return new AuthTokenValidator() {
            public void validate(String token) {
                throw new NotImplementedException("AuthTokenValidator: validate");
            }

            public void validate(String token, List<String> roles) {
                throw new NotImplementedException("AuthTokenValidator: validate");
            }

            public String getServiceName(String token) {
                return "some_service_name";
            }
        };
    }
}
