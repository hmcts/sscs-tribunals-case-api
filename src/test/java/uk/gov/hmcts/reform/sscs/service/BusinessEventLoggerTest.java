package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class BusinessEventLoggerTest {

    private BusinessEventLogger businessEventLogger;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        businessEventLogger = new BusinessEventLogger();
        logger = (Logger) LoggerFactory.getLogger(BusinessEventLogger.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    void shouldLogCaseEventWithCorrectFormat() {
        businessEventLogger.logCaseEvent("processEvent", "12345", "appealCreated", "PIP", "success");

        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage())
                .contains("operation=processEvent")
                .contains("caseId=12345")
                .contains("eventType=appealCreated")
                .contains("benefitType=PIP")
                .contains("outcome=success");
    }

    @Test
    void shouldLogNotificationEventWithCorrectFormat() {
        businessEventLogger.logNotificationEvent("sendNotification", "67890", "email", "template-abc", "success");

        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage())
                .contains("operation=sendNotification")
                .contains("caseId=67890")
                .contains("channel=email")
                .contains("templateId=template-abc")
                .contains("outcome=success");
    }

    @Test
    void shouldLogEvidenceShareEventWithCorrectFormat() {
        businessEventLogger.logEvidenceShareEvent("sendToBulkPrint", "11111", "success", "letterId=abc-123");

        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage())
                .contains("operation=sendToBulkPrint")
                .contains("caseId=11111")
                .contains("outcome=success")
                .contains("detail=letterId=abc-123");
    }

    @Test
    void shouldLogHearingsEventWithCorrectFormat() {
        businessEventLogger.logHearingsEvent("createHearing", "22222", "hearing-456", "success");

        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage())
                .contains("operation=createHearing")
                .contains("caseId=22222")
                .contains("hearingId=hearing-456")
                .contains("outcome=success");
    }

    @Test
    void shouldLogDependencyErrorAtErrorLevel() {
        businessEventLogger.logDependencyError("callCcd", "33333", "CCD", "Connection timeout");

        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage())
                .contains("operation=callCcd")
                .contains("caseId=33333")
                .contains("dependency=CCD")
                .contains("error=Connection timeout");
    }

    @Test
    void shouldHandleNullParametersGracefully() {
        businessEventLogger.logCaseEvent(null, null, null, null, null);

        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage())
                .contains("operation=null")
                .contains("caseId=null");
    }
}
