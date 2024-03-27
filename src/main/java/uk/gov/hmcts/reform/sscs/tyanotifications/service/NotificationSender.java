package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationTestRecipients;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.service.notify.*;

@Component
@Slf4j
public class NotificationSender {

    private static final String USING_TEST_GOV_NOTIFY_KEY_FOR = "Using test GovNotify key {} for {}";
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMM y HH:mm");
    static final ZoneId ZONE_ID_LONDON = ZoneId.of("Europe/London");

    private final NotificationClient notificationClient;
    private final NotificationClient testNotificationClient;
    private final NotificationTestRecipients notificationTestRecipients;
    private final MarkdownTransformationService markdownTransformationService;
    private final SaveCorrespondenceAsyncService saveCorrespondenceAsyncService;
    private final Boolean saveCorrespondence;

    @Autowired
    public NotificationSender(@Qualifier("notificationClient") NotificationClient notificationClient,
                              @Qualifier("testNotificationClient") NotificationClient testNotificationClient,
                              NotificationTestRecipients notificationTestRecipients,
                              MarkdownTransformationService markdownTransformationService,
                              SaveCorrespondenceAsyncService saveCorrespondenceAsyncService,
                              @Value("${feature.save_correspondence}") Boolean saveCorrespondence
    ) {
        this.notificationClient = notificationClient;
        this.testNotificationClient = testNotificationClient;
        this.notificationTestRecipients = notificationTestRecipients;
        this.markdownTransformationService = markdownTransformationService;
        this.saveCorrespondence = saveCorrespondence;
        this.saveCorrespondenceAsyncService = saveCorrespondenceAsyncService;
    }

    public void sendEmail(String templateId, String emailAddress, Map<String, Object> personalisation, String reference,
                          NotificationEventType notificationEventType,
                          SscsCaseData sscsCaseData) throws NotificationClientException {

        NotificationClient client;

        if (notificationTestRecipients.getEmails().contains(emailAddress)
            || emailAddress.matches("test[\\d]+@hmcts.net")) {
            log.info(USING_TEST_GOV_NOTIFY_KEY_FOR, testNotificationClient.getApiKey(), emailAddress);
            client = testNotificationClient;
        } else {
            client = notificationClient;
        }

        final SendEmailResponse sendEmailResponse = getSendEmailResponse(templateId, emailAddress, personalisation, reference, client);

        if (saveCorrespondence && sendEmailResponse != null) {
            final Correspondence correspondence = getEmailCorrespondence(sendEmailResponse, emailAddress, notificationEventType);
            saveCorrespondenceAsyncService.saveEmailOrSms(correspondence, sscsCaseData);
            log.info("Uploaded correspondence email into ccd for case id {}.", sscsCaseData.getCcdCaseId());
        }

        log.info("Email Notification send for case id : {}, Gov notify id: {} ", sscsCaseData.getCcdCaseId(),
            (sendEmailResponse != null) ? sendEmailResponse.getNotificationId() : null);
    }

    @Retryable
    private SendEmailResponse getSendEmailResponse(String templateId, String emailAddress, Map<String, Object> personalisation, String reference, NotificationClient client) throws NotificationClientException {
        final SendEmailResponse sendEmailResponse;
        try {
            sendEmailResponse = client.sendEmail(templateId, emailAddress, personalisation, reference);
        } catch (NotificationClientException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationClientException(e);
        }
        return sendEmailResponse;
    }

    public void sendSms(
        String templateId,
        String phoneNumber,
        Map<String, Object> personalisation,
        String reference,
        String smsSender,
        NotificationEventType notificationEventType,
        SscsCaseData sscsCaseData
    ) throws NotificationClientException {

        NotificationClient client;

        if (notificationTestRecipients.getSms().contains(phoneNumber)) {
            log.info(USING_TEST_GOV_NOTIFY_KEY_FOR, testNotificationClient.getApiKey(), phoneNumber);
            client = testNotificationClient;
        } else {
            client = notificationClient;
        }

        final SendSmsResponse sendSmsResponse = getSendSmsResponse(templateId, phoneNumber, personalisation, reference, smsSender, client);

        if (saveCorrespondence && sendSmsResponse != null) {
            final Correspondence correspondence = getSmsCorrespondence(sendSmsResponse, phoneNumber, notificationEventType);
            saveCorrespondenceAsyncService.saveEmailOrSms(correspondence, sscsCaseData);
            log.info("Uploaded correspondence sms into ccd for case id {}.", sscsCaseData.getCcdCaseId());
        }

        log.info("Sms Notification send for case id : {}, Gov notify id: {} ", sscsCaseData.getCcdCaseId(),
            (sendSmsResponse != null) ? sendSmsResponse.getNotificationId() : null);
    }

    @Retryable
    private SendSmsResponse getSendSmsResponse(String templateId, String phoneNumber, Map<String, Object> personalisation, String reference, String smsSender, NotificationClient client) throws NotificationClientException {
        final SendSmsResponse sendSmsResponse;
        try {
            sendSmsResponse = client.sendSms(
                templateId,
                phoneNumber,
                personalisation,
                reference,
                smsSender
            );
        } catch (NotificationClientException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationClientException(e);
        }
        return sendSmsResponse;
    }

    public void sendLetter(String templateId, Address address, Map<String, Object> personalisation,
                           NotificationEventType notificationEventType,
                           String name, String ccdCaseId) throws NotificationClientException {

        NotificationClient client = getLetterNotificationClient(address.getPostcode());

        final SendLetterResponse sendLetterResponse = getSendLetterResponse(templateId, personalisation, ccdCaseId, client);

        if (saveCorrespondence) {
            final Correspondence correspondence = getLetterCorrespondence(notificationEventType, name);
            saveCorrespondenceAsyncService.saveLetter(client, sendLetterResponse.getNotificationId().toString(), correspondence, ccdCaseId);
        }

        log.info("Letter Notification send for case id : {}, Gov notify id: {} ", ccdCaseId, (sendLetterResponse != null) ? sendLetterResponse.getNotificationId() : null);
    }

    @Retryable
    private SendLetterResponse getSendLetterResponse(String templateId, Map<String, Object> personalisation, String ccdCaseId, NotificationClient client) throws NotificationClientException {
        final SendLetterResponse sendLetterResponse;
        try {
            sendLetterResponse = client.sendLetter(templateId, personalisation, ccdCaseId);
        } catch (NotificationClientException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationClientException(e);
        }
        return sendLetterResponse;
    }

    public void sendBundledLetter(String appellantPostcode, byte[] directionText, NotificationEventType notificationEventType, String name, String ccdCaseId) throws NotificationClientException {
        if (directionText != null) {
            NotificationClient client = getLetterNotificationClient(appellantPostcode);

            ByteArrayInputStream bis = new ByteArrayInputStream(directionText);

            final LetterResponse sendLetterResponse = getBundledLetterResponse(ccdCaseId, client, bis);

            if (saveCorrespondence) {
                final Correspondence correspondence = getLetterCorrespondence(notificationEventType, name);
                saveCorrespondenceAsyncService.saveLetter(client, sendLetterResponse.getNotificationId().toString(), correspondence, ccdCaseId);
            }

            log.info("Letter Notification send for case id : {}, Gov notify id: {} ", ccdCaseId, (sendLetterResponse != null) ? sendLetterResponse.getNotificationId() : null);
        }
    }

    @Retryable
    private LetterResponse getBundledLetterResponse(String ccdCaseId, NotificationClient client, ByteArrayInputStream bis) throws NotificationClientException {
        final LetterResponse sendLetterResponse;
        try {
            sendLetterResponse = client.sendPrecompiledLetterWithInputStream(ccdCaseId, bis);
        } catch (NotificationClientException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationClientException(e);
        }
        return sendLetterResponse;
    }

    public void saveLettersToReasonableAdjustment(byte[] pdfForLetter, NotificationEventType notificationEventType, String name, String ccdCaseId, SubscriptionType subscriptionType) {
        if (pdfForLetter != null) {
            final Correspondence correspondence = getLetterCorrespondence(notificationEventType, name, ReasonableAdjustmentStatus.REQUIRED);
            saveCorrespondenceAsyncService.saveLetter(pdfForLetter, correspondence, ccdCaseId, subscriptionType);

            log.info("Letter Notification saved for case id : {}", ccdCaseId);
        }
    }

    private Correspondence getEmailCorrespondence(final SendEmailResponse sendEmailResponse, final String emailAddress, final NotificationEventType notificationEventType) {
        return Correspondence.builder().value(
            CorrespondenceDetails.builder()
                .body(markdownTransformationService.toHtml(sendEmailResponse.getBody()))
                .subject(sendEmailResponse.getSubject())
                .from(sendEmailResponse.getFromEmail().orElse(""))
                .to(emailAddress)
                .eventType(notificationEventType.getId())
                .correspondenceType(CorrespondenceType.Email)
                .sentOn(LocalDateTime.now(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER))
                .build()
        ).build();
    }

    private Correspondence getSmsCorrespondence(final SendSmsResponse sendSmsResponse, final String phoneNumber, final NotificationEventType notificationEventType) {
        return Correspondence.builder().value(
            CorrespondenceDetails.builder()
                .body(markdownTransformationService.toHtml(sendSmsResponse.getBody()))
                .subject("SMS correspondence")
                .from(sendSmsResponse.getFromNumber().orElse(""))
                .to(phoneNumber)
                .eventType(notificationEventType.getId())
                .correspondenceType(CorrespondenceType.Sms)
                .sentOn(LocalDateTime.now(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER))
                .build()
        ).build();
    }

    private Correspondence getLetterCorrespondence(NotificationEventType notificationEventType, String name) {
        return getLetterCorrespondence(notificationEventType, name, null);
    }

    private Correspondence getLetterCorrespondence(NotificationEventType notificationEventType, String name, ReasonableAdjustmentStatus status) {
        return Correspondence.builder().value(
            CorrespondenceDetails.builder()
                .eventType(notificationEventType.getId())
                .to(name)
                .correspondenceType(CorrespondenceType.Letter)
                .sentOn(LocalDateTime.now(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER))
                .reasonableAdjustmentStatus(status)
                .build()
        ).build();
    }

    private NotificationClient getLetterNotificationClient(String postcode) {
        NotificationClient client;
        if (notificationTestRecipients.getPostcodes().contains("*")
            || notificationTestRecipients.getPostcodes().contains(postcode)) {
            log.info(USING_TEST_GOV_NOTIFY_KEY_FOR, testNotificationClient.getApiKey(), postcode);
            client = testNotificationClient;
        } else {
            client = notificationClient;
        }
        return client;
    }

    @Recover
    public void getBackendResponseFallback(Throwable e) {
        log.error("Failed sending.....", e);
    }
}
