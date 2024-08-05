package uk.gov.hmcts.reform.sscs.tyanotifications.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;

@Configuration
@Validated
@ConfigurationProperties(prefix = "idam")
@Getter
@Setter
public class IdamProperties {

    private Oauth2 oauth2;

    @Getter
    @Setter
    @ToString
    public static class Oauth2 {
        private User user;
        private Client client;
        private String redirectUrl;
        private MediaType contenttype;

        @Getter
        @Setter
        @ToString
        public static class User {
            private String email;
            private String password;
        }

        @Getter
        @Setter
        @ToString
        public static class Client {
            private String id;
            private String secret;
        }
    }
}
