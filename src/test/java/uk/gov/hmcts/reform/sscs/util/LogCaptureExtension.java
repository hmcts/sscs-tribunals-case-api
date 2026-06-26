package uk.gov.hmcts.reform.sscs.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

public class LogCaptureExtension implements BeforeEachCallback, AfterEachCallback {

    private final Class<?> loggerClass;
    private ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender;

    public LogCaptureExtension(Class<?> loggerClass) {
        this.loggerClass = loggerClass;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        appender = new ListAppender<>();
        appender.start();

        Logger logger = (Logger) LoggerFactory.getLogger(loggerClass);
        logger.addAppender(appender);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerClass);
        logger.detachAppender(appender);
    }

    public void assertLogContains(String message, Level level) {
        boolean found = appender.list.stream()
            .anyMatch(event -> event.getLevel().equals(level) && event.getFormattedMessage().contains(message));

        if (!found) {
            throw new AssertionError("Expected log not found: [" + level + "] " + message);
        }
    }
}
