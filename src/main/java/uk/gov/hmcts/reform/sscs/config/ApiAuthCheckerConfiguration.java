package uk.gov.hmcts.reform.sscs.config;

import com.google.common.collect.ImmutableSet;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "security")
public class ApiAuthCheckerConfiguration {

    @Value("#{'${security.authorisedRoles}'.split(',')}")
    private List<String> authorisedRoles;

    public List<String> getAuthorisedRoles() {
        return authorisedRoles;
    }

    @Bean
    public Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor() {
        return any -> ImmutableSet.copyOf(authorisedRoles);
    }

    @Bean
    public Function<HttpServletRequest, Optional<String>> userIdExtractor() {
        return any -> Optional.empty();
    }
}
