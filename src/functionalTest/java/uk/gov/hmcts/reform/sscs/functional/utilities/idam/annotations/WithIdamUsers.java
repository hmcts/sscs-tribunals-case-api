package uk.gov.hmcts.reform.sscs.functional.utilities.idam.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(WithIdamUsersExtension.class)
public @interface WithIdamUsers {

    String[] emails();

    String config() default "/idam-users.json";

    String profile() default "default";
}
