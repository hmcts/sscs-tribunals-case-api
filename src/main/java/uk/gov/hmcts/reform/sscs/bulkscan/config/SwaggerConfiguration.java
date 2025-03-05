package uk.gov.hmcts.reform.sscs.bulkscan.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(applicationName)
                .description("SSCS Bulk Scan API")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("SSCS")
                    .url("http://sscs.net/")
                    .email("sscs@hmcts.net")));
    }

}
