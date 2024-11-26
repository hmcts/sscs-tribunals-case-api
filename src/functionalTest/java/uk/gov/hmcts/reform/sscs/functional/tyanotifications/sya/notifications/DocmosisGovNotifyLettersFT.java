package uk.gov.hmcts.reform.sscs.functional.tyanotifications.sya.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.DOCMOSIS_LETTERS;

import java.io.IOException;
import java.util.List;
import junitparams.JUnitParamsRunner;
import lombok.SneakyThrows;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.functional.tyanotifications.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public class DocmosisGovNotifyLettersFT extends AbstractFunctionalTest {

    public DocmosisGovNotifyLettersFT() {
        super(30);
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(600);

    @SneakyThrows
    @Test
    public void shouldSendDocmosisLettersViaGovNotify() {
        DOCMOSIS_LETTERS.forEach(notificationEventType -> {
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
}
