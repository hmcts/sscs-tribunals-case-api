package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI trackYourAppealNotificationsApi() {
        return new OpenAPI()
            .info(new Info().title("Track Your Appeal Notifications")
                .description("Track Your Appeal Notifications")
                .version("v0.0.1")
                .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }

}
