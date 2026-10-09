package uk.gov.hmcts.reform.sscs.config;

import feign.FeignException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.auth.checker.core.CachingSubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.AuthCheckerException;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.BearerTokenInvalidException;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.spring.AuthCheckerProperties;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

@Configuration
public class UserResolverConfiguration {

    @Bean
    public SubjectResolver<User> userResolver(IdamService idamService, AuthCheckerProperties properties) {
        SubjectResolver<User> resolver = bearerToken -> {
            String bearer = bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken;
            try {
                UserDetails userDetails = idamService.getUserDetails(bearer);
                Set<String> roles = userDetails.getRoles() == null
                        ? Set.of() : new HashSet<>(userDetails.getRoles());
                return new User(userDetails.getId(), roles);
            } catch (FeignException e) {
                if (e.status() == 401) {
                    throw new BearerTokenInvalidException(e);
                }

                throw new AuthCheckerException("Error retrieving user details from IDAM", e);
            } catch (Exception e) {
                throw new AuthCheckerException("Error retrieving user details from IDAM", e);
            }
        };

        return new CachingSubjectResolver<>(
                resolver,
                properties.getUser().getTtlInSeconds(),
                properties.getUser().getMaximumSize()
        );
    }
}
