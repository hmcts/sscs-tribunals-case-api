package uk.gov.hmcts.reform.sscs.functional.tyanotifications.sya.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.APPEAL_RECEIVED;

import java.io.IOException;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.functional.tyanotifications.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public class DocmosisGovNotifyLettersFT extends AbstractFunctionalTest {

    public DocmosisGovNotifyLettersFT() {
        super(30);
    }

    @Test
    public void shouldSendDocmosisLetterViaGovNotify()
        throws IOException, NotificationClientException {
        simulateCcdCallback(APPEAL_RECEIVED);

        List<Notification> notifications = fetchLetters();
        saveLetterPdfs(notifications);

        assertThat(notifications)
            .extracting(Notification::getSubject)
            .allSatisfy(subject -> assertThat(subject).isPresent());
    }
}
