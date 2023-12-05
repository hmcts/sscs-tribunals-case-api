package uk.gov.hmcts.reform.sscs.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
            .addFilter(authCheckerUserOnlyFilter)
            .sessionManagement().sessionCreationPolicy(STATELESS)
            .and()
            .csrf().disable()
            .formLogin().disable()
            .logout().disable()
            .authorizeHttpRequests()
            .requestMatchers("/health").permitAll()
            .requestMatchers("/health/liveness").permitAll()
            .requestMatchers("/loggers/**").permitAll()
            .requestMatchers("/swagger-ui.html").permitAll()
            .requestMatchers("/swagger-ui/index.html").permitAll()
            .requestMatchers("/swagger-resources/**").permitAll()
            .requestMatchers("/v3/api-docs/**").permitAll()
            .requestMatchers("/regionalcentre/**").permitAll()
            .requestMatchers("/tokens/**").permitAll()
            .requestMatchers("/appeals").permitAll()
            .requestMatchers("/appeals/**").permitAll()
            .requestMatchers("/evidence/upload").permitAll()
            .requestMatchers("/drafts").authenticated()
            .requestMatchers("/api/citizen").authenticated()
            .requestMatchers("/api/request").authenticated();
        // @formatter:on

        return http.build();
    }
}
