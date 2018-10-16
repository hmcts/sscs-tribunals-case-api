package uk.gov.hmcts.reform.sscs.config;

import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Configuration for swagger to auto generate our REST API documentation.
 * For more info please {@see http://swagger.io/getting-started/}
 */
@Configuration
@EnableSwagger2
public class SwaggerDocumentationConfig {

    ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("tribunals-case-api")
                .description("SSCS Tribunals Case API")
                .version("1.0.0")
                .contact(new Contact("SSCS","http://sscs.net/", "sscs@hmcts.net"))
                .build();
    }

    @Bean
    public Docket customImplementation() {
        return new Docket(SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("uk.gov.hmcts.sscs.controller"))
                .build()
                .apiInfo(apiInfo());
    }
}
