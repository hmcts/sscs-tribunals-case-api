package uk.gov.hmcts.reform.sscs.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.spring.useronly.AuthCheckerUserOnlyFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final RequestAuthorizer<User> userRequestAuthorizer;
    private final AuthenticationManager authenticationManager;

    public SecurityConfiguration(
        RequestAuthorizer<User> userRequestAuthorizer,
        AuthenticationManager authenticationManager
    ) {
        this.userRequestAuthorizer = userRequestAuthorizer;
        this.authenticationManager = authenticationManager;
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {

        AuthCheckerUserOnlyFilter<User> authCheckerUserOnlyFilter =
            new AuthCheckerUserOnlyFilter<>(userRequestAuthorizer);

        authCheckerUserOnlyFilter.setAuthenticationManager(authenticationManager);
        // @formatter:off
        http
            .securityMatcher(
                "/drafts",
                "/api/citizen",
                "/api/request"
            )
            .addFilter(authCheckerUserOnlyFilter)
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .csrf(csrf -> csrf.disable())
            .formLogin(formLogin -> formLogin.disable())
            .logout(logout -> logout.disable())
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());

        // @formatter:on

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
            "/health",
            "/health/liveness",
            "/loggers/**",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/regionalcentre/**",
            "/tokens/**",
            "/appeals",
            "/appeals/**",
            "/evidence/upload"
        );
    }
}
