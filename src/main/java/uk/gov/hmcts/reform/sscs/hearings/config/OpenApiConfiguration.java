package uk.gov.hmcts.reform.sscs.hearings.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
            .info(new Info()
                .title(applicationName)
                .description("Hearings API is a spring boot based application to manage hearings for SSCS")
                .version("v0.0.1")
                .contact(new Contact()
                        .name("SSCS")
                        .url("http://sscs.net/")
                        .email("sscs@hmcts.net"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .externalDocs(new ExternalDocumentation()
                .description("README")
                .url("https://github.com/hmcts/sscs-hearings-api"));
    }

}
