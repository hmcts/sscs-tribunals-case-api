package uk.gov.hmcts.reform.sscs.functional.tyanotifications.sya.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.functional.tyanotifications.sya.notifications.DocmosisWithGovNotifyLetterFunctionalTest.DOCMOSIS_LETTERS_WITH_NO_TEST_CALLBACK;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.DOCMOSIS_LETTERS;

import java.util.List;
import junitparams.JUnitParamsRunner;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.functional.tyanotifications.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;

@RunWith(JUnitParamsRunner.class)
public class DocmosisGovNotifyLettersFT extends AbstractFunctionalTest {

    public DocmosisGovNotifyLettersFT() {
        super(30);
    }

    @SneakyThrows
    @Test
    public void shouldSendDocmosisLettersViaGovNotify() {
        DOCMOSIS_LETTERS.stream()
                .filter(notificationEventType ->
                        !DOCMOSIS_LETTERS_WITH_NO_TEST_CALLBACK.contains(notificationEventType))
                .forEach(this::simulateCcdCallback);

        List<Notification> notifications = fetchLetters();
        saveLetterPdfs(notifications);

        assertThat(notifications)
            .extracting(Notification::getSubject)
            .allSatisfy(subject -> assertThat(subject).isPresent());
    }
}
