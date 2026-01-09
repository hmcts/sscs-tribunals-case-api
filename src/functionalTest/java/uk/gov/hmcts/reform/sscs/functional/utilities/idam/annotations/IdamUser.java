package uk.gov.hmcts.reform.sscs.functional.utilities.idam.annotations;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IdamUser {
    String email() default "";
}

