package uk.gov.hmcts.reform.sscs.functional.tyanotifications;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class Retry implements TestRule {
    private int retryCount;

    public Retry(int retryCount) {
        this.retryCount = retryCount;
    }

    public Statement apply(Statement base, Description description) {
        if (retryCount != 0) {
            return statement(base, description);
        }
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
            }
        };
    }

    private Statement statement(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable caughtThrowable = null;

                // implement retry logic here
                for (int i = 1; i <= retryCount; i++) {
                    try {
                        base.evaluate();
                        return;
                    } catch (Throwable t) {
                        caughtThrowable = t;
                        System.err.println(description.getDisplayName() + ": run " + i + " failed");
                    }
                }
                System.err.println(description.getDisplayName() + ": giving up after " + retryCount + " failures");
                throw caughtThrowable;
            }
        };
    }
}
