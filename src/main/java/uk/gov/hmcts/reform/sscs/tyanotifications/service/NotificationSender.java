package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.buildBundledLetterFromPdfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReasonableAdjustmentStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationTestRecipients;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendLetterResponse;
import uk.gov.service.notify.SendSmsResponse;

@Component
@Slf4j
public class NotificationSender {

    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMM y HH:mm");
    static final ZoneId ZONE_ID_LONDON = ZoneId.of("Europe/London");
    private static final String USING_TEST_GOV_NOTIFY_KEY_FOR = "Using test GovNotify key {} for {}";
    private final NotificationClient notificationClient;
    private final NotificationClient testNotificationClient;
    private final NotificationTestRecipients notificationTestRecipients;
    private final MarkdownTransformationService markdownTransformationService;
    private final SaveCorrespondenceAsyncService saveCorrespondenceAsyncService;
    private final BulkPrintService bulkPrintService;
    private final Boolean saveCorrespondence;

    @Autowired
    public NotificationSender(@Qualifier("notificationClient") NotificationClient notificationClient,
        @Qualifier("testNotificationClient") NotificationClient testNotificationClient,
        BulkPrintService bulkPrintService,
        NotificationTestRecipients notificationTestRecipients,
        MarkdownTransformationService markdownTransformationService,
        SaveCorrespondenceAsyncService saveCorrespondenceAsyncService,
        @Value("${feature.save_correspondence}") Boolean saveCorrespondence
    ) {
        this.notificationClient = notificationClient;
        this.testNotificationClient = testNotificationClient;
        this.notificationTestRecipients = notificationTestRecipients;
        this.markdownTransformationService = markdownTransformationService;
        this.bulkPrintService = bulkPrintService;
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

        final SendEmailResponse sendEmailResponse =
            getSendEmailResponse(templateId, emailAddress, personalisation, reference, client);

        if (saveCorrespondence && sendEmailResponse != null) {
            final Correspondence correspondence =
                getEmailCorrespondence(sendEmailResponse, emailAddress, notificationEventType);
            saveCorrespondenceAsyncService.saveEmailOrSms(correspondence, sscsCaseData);
            log.info("Uploaded correspondence email into ccd for case id {}.", sscsCaseData.getCcdCaseId());
        }

        log.info("Email Notification send for case id : {}, Gov notify id: {} ", sscsCaseData.getCcdCaseId(),
            (sendEmailResponse != null) ? sendEmailResponse.getNotificationId() : null);
    }

    @Retryable
    private SendEmailResponse getSendEmailResponse(String templateId, String emailAddress,
        Map<String, Object> personalisation, String reference,
        NotificationClient client) throws NotificationClientException {
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

        final SendSmsResponse sendSmsResponse =
            getSendSmsResponse(templateId, phoneNumber, personalisation, reference, smsSender, client);

        if (saveCorrespondence && sendSmsResponse != null) {
            final Correspondence correspondence =
                getSmsCorrespondence(sendSmsResponse, phoneNumber, notificationEventType);
            saveCorrespondenceAsyncService.saveEmailOrSms(correspondence, sscsCaseData);
            log.info("Uploaded correspondence sms into ccd for case id {}.", sscsCaseData.getCcdCaseId());
        }

        log.info("Sms Notification send for case id : {}, Gov notify id: {} ", sscsCaseData.getCcdCaseId(),
            (sendSmsResponse != null) ? sendSmsResponse.getNotificationId() : null);
    }

    @Retryable
    private SendSmsResponse getSendSmsResponse(String templateId, String phoneNumber,
        Map<String, Object> personalisation, String reference, String smsSender,
        NotificationClient client) throws NotificationClientException {
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

        final SendLetterResponse sendLetterResponse =
            sendLetterViaGovNotify(templateId, personalisation, ccdCaseId, client);

        if (saveCorrespondence) {
            final Correspondence correspondence = getLetterCorrespondence(notificationEventType, name, null);
            saveCorrespondenceAsyncService.saveLetter(
                client, sendLetterResponse.getNotificationId().toString(), correspondence, ccdCaseId);
        }

        log.info("Letter Notification send for case id : {}, Gov notify id: {} ",
            ccdCaseId, (sendLetterResponse != null) ? sendLetterResponse.getNotificationId() : null);
    }

    @Retryable
    private SendLetterResponse sendLetterViaGovNotify(String templateId, Map<String, Object> personalisation,
        String ccdCaseId, NotificationClient client)
        throws NotificationClientException {
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

    public void sendBundledLetter(NotificationWrapper wrapper, byte[] content, String recipient)
        throws NotificationClientException {
        if (content != null) {
            boolean pageLimitExceeded = false;

            try (PDDocument pdfDoc = Loader.loadPDF(content)) {
                pageLimitExceeded = pdfDoc.getNumberOfPages() > 10;
                log.info(pageLimitExceeded ? "{} letter exceeds Gov.Notify 10-page limit for precompiled letters [{}]"
                        : "Sending {} precompiled letter of [{}] pages",
                    wrapper.getNotificationType(), pdfDoc.getNumberOfPages());
            } catch (IOException e) {
                log.info("Failed to calculate the number of pages contained in the letter {}", e.getMessage());
            }

            String govNotifyId = null;
            NotificationClient client = null;
            var caseData = wrapper.getNewSscsCaseData();

            if (wrapper.getNotificationType().equals(ISSUE_FINAL_DECISION) && pageLimitExceeded) {
                bulkPrintService
                    .sendToBulkPrint(List.of(new Pdf(content, ISSUE_FINAL_DECISION.name())), caseData, recipient);
                log.info("Sending {} Letter for case id : {} via BulkPrint because it exceeds 10 pages",
                    wrapper.getNotificationType(), wrapper.getCaseId());
            } else {
                client = getLetterNotificationClient(caseData.getAppeal().getAppellant().getAddress().getPostcode());
                ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
                final LetterResponse notifyResponse = sendBundledLetter(wrapper.getCaseId(), client, inputStream);
                govNotifyId = nonNull(notifyResponse) ? notifyResponse.getNotificationId().toString() : null;

                log.info("Letter Notification send for case id : {}, Gov notify id: {} ",
                        wrapper.getCaseId(), govNotifyId);
            }

            if (saveCorrespondence) {
                final var correspondence = getLetterCorrespondence(wrapper.getNotificationType(), recipient, null);
                if (isNull(govNotifyId)) {
                    saveCorrespondenceAsyncService.saveLetter(content, correspondence, wrapper.getCaseId());
                } else {
                    saveCorrespondenceAsyncService.saveLetter(client, govNotifyId, correspondence, wrapper.getCaseId());
                }
            }
        }
    }

    @Retryable
    private LetterResponse sendBundledLetter(String ccdCaseId, NotificationClient client, ByteArrayInputStream bis)
        throws NotificationClientException {
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

    public void sendBundledLetter(EventType eventType, SscsCaseData caseData, List<Pdf> pdfs,
        String recipient) {
        if (isNotEmpty(pdfs)) {
            byte[] pdfContent = buildBundledLetterFromPdfs(pdfs);
            int physicalPageCount;
            try (PDDocument pdfDoc = Loader.loadPDF(pdfContent)) {
                physicalPageCount = pdfDoc.getNumberOfPages();
            } catch (IOException e) {
                log.error("Failed to calculate the number of pages contained in the letter {} for case id {} and notification {}", e.getMessage(), caseData.getCcdCaseId(), eventType.getCcdType());
                throw new NotificationServiceException("Failed to calculate the number of pages contained in the letter %s for case id %s and notification %s".formatted(e.getMessage(), caseData.getCcdCaseId(), eventType.getCcdType()), e);
            }

            if (physicalPageCount > 10) {
                log.info("Sending {} Letter for case id {} via BulkPrint because it exceeds 10 pages ({} pages).", eventType.getCcdType(),
                    caseData.getCcdCaseId(), physicalPageCount);
                sendUsingBulkPrint(eventType, caseData, pdfs, pdfContent, recipient);
            } else {
                log.info("Sending {} Letter for case id {} via Gov Notify because it is less than or equal to 10 pages ({} pages).",
                    eventType.getCcdType(), caseData.getCcdCaseId(), physicalPageCount);
                try {
                    sendUsingGovNotify(eventType, caseData, pdfContent, recipient);
                } catch (NotificationClientException e) {
                    final NotificationServiceException exception = new NotificationServiceException(caseData.getCcdCaseId(), e);
                    log.error("Error sending notification for case id: {}", caseData.getCcdCaseId(), exception);
                    throw exception;
                }
            }
        }
    }

    private void sendUsingBulkPrint(EventType eventType, SscsCaseData caseData, List<Pdf> pdfs, byte[] pdfContent, String recipient) {
        final Optional<UUID> uuid = bulkPrintService.sendToBulkPrint(pdfs, caseData, recipient);
        if (uuid.isPresent()) {
            log.info("Letter Notification sent via Bulk Print for Letter {}, case id {} and Bulk print id {}",
                eventType.getCcdType(), caseData.getCcdCaseId(), uuid.get());
            saveLetterToCcd(eventType, Long.valueOf(caseData.getCcdCaseId()), pdfContent, recipient, uuid.get());
        } else {
            log.error("Failed to send to bulk print for case {}. No print id returned", caseData.getCcdCaseId());
        }
    }

    private void sendUsingGovNotify(EventType eventType, SscsCaseData caseData, byte[] pdfContent, String recipient) throws NotificationClientException {
        final NotificationClient client = getLetterNotificationClient(caseData.getAppeal().getAppellant().getAddress().getPostcode());
        final LetterResponse notifyResponse = sendBundledLetter(caseData.getCcdCaseId(), client, new ByteArrayInputStream(pdfContent));
        final String govNotifyId = nonNull(notifyResponse) ? notifyResponse.getNotificationId().toString() : null;
        if (nonNull(govNotifyId)) {
            log.info("Letter Notification sent via Gov Notify for Letter {}, case id {} and Gov Notify id {}",
                eventType.getCcdType(), caseData.getCcdCaseId(), govNotifyId);
            saveLetterToCcd(eventType, Long.valueOf(caseData.getCcdCaseId()), recipient, govNotifyId, client);
        } else {
            log.error("Failed to send letter for case {}. Gov notify id is null", caseData.getCcdCaseId());
        }
    }

    private void saveLetterToCcd(EventType eventType, Long caseId, byte[] pdfContent, String recipient, UUID uuid) {
        saveCorrespondenceAsyncService.saveLetter(pdfContent, getLetterCorrespondence(eventType, recipient), caseId.toString());
        log.info("Letter Notification recorded in CCD for Letter {}, case id {} and Bulk print id {}", eventType.getCcdType(),
            caseId, uuid);
    }

    private void saveLetterToCcd(EventType eventType, Long caseId, String recipient, String govNotifyId,
        NotificationClient client) throws NotificationClientException {
        saveCorrespondenceAsyncService.saveLetter(client, govNotifyId, getLetterCorrespondence(eventType, recipient),
            caseId.toString());
        log.info("Letter Notification recorded in CCD for Letter {}, case id {} and Gov Notify id {}", eventType.getCcdType(),
            caseId, govNotifyId);
    }

    public void saveLettersToReasonableAdjustment(byte[] pdfForLetter, NotificationEventType notificationEventType,
        String name, String ccdCaseId, SubscriptionType subscriptionType) {
        if (pdfForLetter != null) {
            final Correspondence correspondence =
                getLetterCorrespondence(notificationEventType, name, ReasonableAdjustmentStatus.REQUIRED);
            saveCorrespondenceAsyncService.saveLettersToReasonableAdjustment(pdfForLetter, correspondence, ccdCaseId,
                subscriptionType);

            log.info("Letter Notification saved for case id : {}", ccdCaseId);
        }
    }

    private Correspondence getEmailCorrespondence(final SendEmailResponse sendEmailResponse, final String emailAddress,
        final NotificationEventType notificationEventType) {
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

    private Correspondence getSmsCorrespondence(final SendSmsResponse sendSmsResponse, final String phoneNumber,
        final NotificationEventType notificationEventType) {
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

    private Correspondence getLetterCorrespondence(NotificationEventType notificationEventType, String name,
        ReasonableAdjustmentStatus status) {
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

    private Correspondence getLetterCorrespondence(EventType eventType, String name) {
        return Correspondence.builder().value(
            CorrespondenceDetails.builder()
                                 .eventType(eventType.getCcdType())
                                 .to(name)
                                 .correspondenceType(CorrespondenceType.Letter)
                                 .sentOn(LocalDateTime.now(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER))
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
