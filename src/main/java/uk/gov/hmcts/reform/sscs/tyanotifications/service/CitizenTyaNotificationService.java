package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@Slf4j
@Service
public class CitizenTyaNotificationService {

    private static final String TYA_NUMBERS_PLACEHOLDER = "tya_numbers";

    private final NotificationClient testNotificationClient;
    private final String resendTyaEmailTemplateId;

    public CitizenTyaNotificationService(
            @Qualifier("testNotificationClient") NotificationClient testNotificationClient,
            @Value("${notification.english.resendTya.emailId}") String resendTyaEmailTemplateId
    ) {
        this.testNotificationClient = testNotificationClient;
        this.resendTyaEmailTemplateId = resendTyaEmailTemplateId;
    }

    public void sendTyaNumbers(String emailAddress, List<String> tyas) throws NotificationClientException {

        Map<String, Object> personalisation = Map.of(
                TYA_NUMBERS_PLACEHOLDER,
                tyas.stream()
                        .map(tya -> "- " + tya)
                        .collect(Collectors.joining("\n"))
        );

        testNotificationClient.sendEmail(
                resendTyaEmailTemplateId,
                emailAddress,
                personalisation,
                "resend-tya-" + UUID.randomUUID()
        );

        log.info("Sent resend TYA email to {} with {} TYA references", emailAddress, tyas.size());
    }

}