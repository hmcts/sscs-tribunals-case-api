package helper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Apply to a test method or test class to retry failed tests.
 * Example: @Retry(3) retries up to 3 times.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(RetryExtension.class)
public @interface Retry {
    /**
     * Number of attempts (total). Must be >= 1.
     */
    int value() default 1;
}
