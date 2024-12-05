package uk.gov.hmcts.reform.sscs.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.DOCMOSIS_LETTERS;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.ACTION_HEARING_RECORDING_REQUEST;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.HEARING_BOOKED;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import lombok.SneakyThrows;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public class DocmosisGovNotifyLettersFT extends AbstractNotificationsFT {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(30*60);

    public DocmosisGovNotifyLettersFT() {
        super(30);
    }

    @BeforeEach
    public void setup() {
        super.setup();
    }

    @SneakyThrows
    @Test
    public void shouldSendDocmosisLettersViaGovNotify() {
        getNotificationList().forEach(notificationEventType -> {
            try {
                simulateCcdCallbackToSendLetter(notificationEventType);
                List<Notification> notifications = fetchLetters();
                saveLetterPdfs(notifications);
                assertThat(notifications)
                        .extracting(Notification::getSubject)
                        .allSatisfy(subject -> assertThat(subject).isPresent());

            } catch (IOException | NotificationClientException e) {
                logFailedEventNotification(notificationEventType, e);
            }
        });
    }

    private static Set<NotificationEventType> getNotificationList() {
        return DOCMOSIS_LETTERS.stream()
                .filter(notificationEventType ->
                        !List.of(ACTION_HEARING_RECORDING_REQUEST, HEARING_BOOKED).contains(notificationEventType))
                .collect(Collectors.toSet());
    }
}
