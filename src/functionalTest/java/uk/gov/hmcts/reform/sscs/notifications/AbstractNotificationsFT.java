package uk.gov.hmcts.reform.sscs.notifications;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import junitparams.JUnitParamsRunner;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.functional.tyanotifications.AbstractFunctionalTest;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public abstract class AbstractNotificationsFT extends AbstractFunctionalTest {

    private static final Logger log = getLogger(AbstractNotificationsFT.class);

    @Autowired
    @Qualifier("testNotificationClient")
    @Getter
    private NotificationClient client;

    protected SscsCaseData caseData;

    public AbstractNotificationsFT(int maxSecondsToWaitForNotification) {
        super(maxSecondsToWaitForNotification);
    }

    protected void simulateCcdCallbackToSendLetter(NotificationEventType eventType) throws IOException {
        String callbackJsonName = BASE_PATH_TYAN + "appealCreatedAppointeeCallback.json";
        simulateCcdCallback(eventType, callbackJsonName);
    }

    public List<UUID> saveLetterPdfs(List<Notification> notifications) {
        List<UUID> savedLetters = new ArrayList<>();

        while (notifications.size() > savedLetters.size()) {
            notifications.stream()
                    .filter(notification -> notification.getNotificationType().equals("letter"))
                    .filter(notification -> !savedLetters.contains(notification.getId()))
                    .forEach(notification -> {
                        try {
                            final byte[] pdfForLetter = client.getPdfForLetter(String.valueOf(notification.getId()));
                            FileUtils.writeByteArrayToFile(
                                    new File("notification_pdfs/" + notification.getTemplateId() + ".pdf"),
                                    pdfForLetter
                            );
                            savedLetters.add(notification.getId());
                        } catch (NotificationClientException | IOException e) {
                            log.error("Failed to save all letter pdfs, {} remain unsaved", notifications.size());
                        }
                    });
            delayInSeconds(60);
        }
        return savedLetters;
    }

    public void logFailedEventNotification(NotificationEventType notificationType, Exception e) {
        log.error("Failed testing notification type {} with the following", notificationType, e);
    }

    @Override
    public String updateJson(String json, NotificationEventType eventType) {
        json = super.updateJson(json, eventType);
        json = json.replace("event_id_value", eventType.getEvent().getCcdType());

        log.info("Functional test: updating case [{}]", caseId);

        return json;
    }
}
