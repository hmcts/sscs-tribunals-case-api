package helper;

import static uk.gov.hmcts.reform.sscs.model.AppConstants.FUNCTIONAL_RETRY_LIMIT;

import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * JUnit 5 extension that retries a test method when it fails.
 * It reads the @Retry annotation on the method or the test class.
 */
public class RetryExtension implements InvocationInterceptor {

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {

        int attempts = getRetryCount(extensionContext);
        if (attempts <= 1) {
            invocation.proceed();
            return;
        }

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                invocation.proceed();
                return; // success
            } catch (Throwable ex) {
                System.err.printf("Test %s failed on attempt %d/%d: %s%n",
                                  getDisplayName(extensionContext), attempt, attempts, ex);
                if (attempt == attempts) {
                    throw ex;
                }
            }
        }
    }

    private int getRetryCount(ExtensionContext ctx) {
        Optional<Retry> methodAnn = ctx.getElement()
            .flatMap(el -> AnnotationSupport.findAnnotation(el, Retry.class));

        if (methodAnn.isPresent()) {
            return Math.max(1, methodAnn.get().value());
        }

        Optional<Retry> classAnn = ctx.getTestClass()
            .flatMap(cls -> AnnotationSupport.findAnnotation(cls, Retry.class));

        return classAnn.map(retry -> Math.max(1, retry.value()))
                .orElse(FUNCTIONAL_RETRY_LIMIT);

    }

    private String getDisplayName(ExtensionContext ctx) {
        return ctx.getDisplayName();
    }
}
