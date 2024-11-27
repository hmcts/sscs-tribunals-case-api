package uk.gov.hmcts.reform.sscs.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.DOCMOSIS_LETTERS;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import junitparams.JUnitParamsRunner;
import lombok.SneakyThrows;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public class DocmosisGovNotifyLettersFT extends AbstractNotificationsFT {

    public DocmosisGovNotifyLettersFT() {
        super(30);
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(600);

    @BeforeEach
    public void setup() {
        super.setup();
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("notificationEventTypesProvider")
    public void shouldSendDocmosisLettersViaGovNotify(NotificationEventType notificationEventType) {
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
    }

    private static Set<NotificationEventType> notificationEventTypesProvider() {
        return DOCMOSIS_LETTERS;
    }
}
