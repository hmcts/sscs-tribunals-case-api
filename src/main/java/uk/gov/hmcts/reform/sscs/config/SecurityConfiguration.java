package uk.gov.hmcts.reform.sscs.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.spring.serviceonly.AuthCheckerServiceOnlyFilter;
import uk.gov.hmcts.reform.auth.checker.spring.useronly.AuthCheckerUserOnlyFilter;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;


@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final RequestAuthorizer<User> userRequestAuthorizer;
    private final AuthorisationService authorisationService;
    private final AuthenticationManager authenticationManager;

    public SecurityConfiguration(
            RequestAuthorizer<User> userRequestAuthorizer, AuthorisationService authorisationService,
            AuthenticationManager authenticationManager
    ) {
        this.userRequestAuthorizer = userRequestAuthorizer;
        this.authorisationService = authorisationService;
        this.authenticationManager = authenticationManager;
    }

    @Bean
    public SecurityFilterChain configureUserAuth(HttpSecurity http) throws Exception {

        AuthCheckerUserOnlyFilter<User> authCheckerUserOnlyFilter =
            new AuthCheckerUserOnlyFilter<>(userRequestAuthorizer);
        authCheckerUserOnlyFilter.setAuthenticationManager(authenticationManager);

        http
            .securityMatcher(
                "/drafts",
                "/api/citizen",
                "/api/request"
            )
            .addFilter(authCheckerUserOnlyFilter)
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public SecurityFilterChain configureServiceAuth(HttpSecurity http) throws Exception {
        AuthCheckerServiceOnlyFilter authCheckerServiceOnlyFilter =
                new AuthCheckerServiceOnlyFilter(authorisationService);
        authCheckerServiceOnlyFilter.setAuthenticationManager(authenticationManager);

        http.securityMatcher("/api/continuous-online-hearings/**",
                        "/document",
                        "/evidence/upload",
                        "/appeals")
                .addFilter(authCheckerServiceOnlyFilter)
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
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
            "/tokens/**"
        );
    }
}
