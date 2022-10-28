package uk.gov.hmcts.reform.sscs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for swagger to auto generate our REST API documentation.
 * For more info please {@see http://swagger.io/getting-started/}
 */
@Configuration
public class SwaggerDocumentationConfig {

    @Bean
    public OpenAPI customImplementation() {
        return new OpenAPI()
            .info(new Info()
                .title("tribunals-case-api")
                .description("SSCS Tribunals Case API")
                .version("1.0.0")
                .contact(new Contact().name("SSCS")
                    .url("http://sscs.net/")
                    .email("sscs@hmcts.net")));
    }
}
