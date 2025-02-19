package uk.gov.hmcts.reform.sscs.notifications;

import static java.time.LocalTime.now;
import static java.util.Objects.isNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
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

    protected void simulateCcdCallbackToSendLetter(NotificationEventType eventType, boolean isWelsh) throws IOException {
        log.info("Simulating CCD callback to send notificaiton of type {}", eventType);
        String fileSuffix = isWelsh ? "CallbackWelsh.json" : "Callback.json";
        String callbackJsonName = BASE_PATH_TYAN + eventType.getId() + fileSuffix;
        if (isNull(getClass().getClassLoader().getResource(callbackJsonName))) {
            callbackJsonName = BASE_PATH_TYAN + "missingFileFallbackCallback.json";
            log.info("No callback json found for {}, using fallback file", eventType);
        }
        simulateCcdCallback(eventType, callbackJsonName);
        log.info("{} notification successfully sent", eventType);
    }

    public List<UUID> saveLetterPdfs(List<Notification> notifications, NotificationEventType eventType) {
        log.info("Saving notification pdfs for {} notifications triggered by {}",
                notifications.size(), eventType);
        List<UUID> savedLetters = new ArrayList<>();
        LocalTime startTime = now();
        int maxMinutesToWaitForPdf = 10;

        while (notifications.size() > savedLetters.size()
                && now().isBefore(startTime.plusMinutes(maxMinutesToWaitForPdf))) {
            notifications.stream()
                    .filter(notification -> notification.getNotificationType().equals("letter"))
                    .filter(notification -> !savedLetters.contains(notification.getId()))
                    .forEach(notification -> {
                        try {
                            final byte[] pdfForLetter = client.getPdfForLetter(String.valueOf(notification.getId()));
                            var pdfName = "notification_pdfs/" + eventType + "_" + notification.getTemplateId() + ".pdf";
                            FileUtils.writeByteArrayToFile(new File(pdfName), pdfForLetter);
                            savedLetters.add(notification.getId());
                        } catch (NotificationClientException | IOException e) {
                            log.error("Failed to save pdf for template {}", notification.getTemplateId());
                            delayInSeconds(60);
                        }
                    });
        }
        log.info("Finished saving letter pdfs : {} out of {} successfully saved",
                notifications.size() - savedLetters.size(), notifications.size());
        return savedLetters;
    }

    public void logFailedEventNotification(NotificationEventType notificationType, Exception e) {
        log.error("Failed testing notification type {} with the following", notificationType, e);
    }
}
