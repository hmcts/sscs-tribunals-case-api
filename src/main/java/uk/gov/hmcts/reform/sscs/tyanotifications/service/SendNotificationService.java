package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.CORRECTION_GRANTED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.PERMISSION_TO_APPEAL_GRANTED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.PERMISSION_TO_APPEAL_REFUSED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.REVIEW_AND_SET_ASIDE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.SET_ASIDE_GRANTED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.SET_ASIDE_REFUSED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationValidService.isBundledLetter;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.docmosis.PdfLetterService;
import uk.gov.service.notify.NotificationClientException;

@Service
@Slf4j
public class SendNotificationService {
    private static final String NOTIFICATION_TYPE_LETTER = "Letter";

    @Value("${reminder.dwpResponseLateReminder.delay.seconds}")
    private long delay;

    private final NotificationSender notificationSender;
    private final NotificationHandler notificationHandler;
    private final NotificationValidService notificationValidService;
    private final PdfLetterService pdfLetterService;
    private final PdfStoreService pdfStoreService;

    @Autowired
    public SendNotificationService(
        NotificationSender notificationSender,
        NotificationHandler notificationHandler,
        NotificationValidService notificationValidService,
        PdfLetterService pdfLetterService,
        PdfStoreService pdfStoreService
    ) {
        this.notificationSender = notificationSender;
        this.notificationHandler = notificationHandler;
        this.notificationValidService = notificationValidService;
        this.pdfLetterService = pdfLetterService;
        this.pdfStoreService = pdfStoreService;
    }

    boolean sendEmailSmsLetterNotification(
        NotificationWrapper wrapper,
        Notification notification,
        SubscriptionWithType subscriptionWithType,
        NotificationEventType eventType) {
        boolean emailSent = sendEmailNotification(wrapper, subscriptionWithType.getSubscription(), notification);
        notificationSuccessLog(wrapper, "Email", notification, notification.getEmailTemplate(), emailSent);

        boolean smsSent = sendSmsNotification(wrapper, subscriptionWithType.getSubscription(), notification, eventType);
        if (nonNull(notification.getSmsTemplate())) {
            notificationSuccessLog(wrapper, "SMS", notification, String.join(", ", notification.getSmsTemplate()), smsSent);
        }

        boolean isInterlocLetter = NotificationEventTypeLists.EVENT_TYPES_FOR_INTERLOC_LETTERS.contains(eventType);
        boolean isDocmosisLetter = NotificationEventTypeLists.DOCMOSIS_LETTERS.contains(eventType);

        boolean letterSent = false;
        if (shouldSendLetter(wrapper, notification, isInterlocLetter, isDocmosisLetter)) {
            letterSent = sendLetterNotification(wrapper, notification, subscriptionWithType, eventType);
            if (isDocmosisLetter) {
                notificationSuccessLog(wrapper, "Docmosis Letter", notification, notification.getDocmosisLetterTemplate(), letterSent);
            } else {
                notificationSuccessLog(wrapper, "Gov Notify Letter", notification, notification.getLetterTemplate(), letterSent);
            }
        }

        boolean notificationSent = emailSent || smsSent || letterSent;

        if (!notificationSent) {
            log.error("Did not send a notification for event {} for case id {}.", eventType.getId(), wrapper.getCaseId());
        }

        return notificationSent;
    }

    private static void notificationSuccessLog(NotificationWrapper wrapper, String notificationType,
                                               Notification notification, String templates, boolean wasSuccessful) {
        Object partyType = Optional.ofNullable(notification)
            .map(Notification::getPlaceholders)
            .map(map -> map.get(PARTY_TYPE))
            .orElse(null);
        Object entityType = Optional.ofNullable(notification)
            .map(Notification::getPlaceholders)
            .map(map -> map.get(ENTITY_TYPE))
            .orElse(null);
        log.info("{} {} with template/s {} was {} for party {}, entity {} and Case Id {}",
            wrapper.getNotificationType(),
            notificationType,
            templates,
            wasSuccessful ? "sent successfully" : "was unsuccessful in sending",
            partyType,
            entityType,
            wrapper.getCaseId());
    }

    private boolean shouldSendLetter(NotificationWrapper wrapper, Notification notification, boolean isInterlocLetter, boolean isDocmosisLetter) {
        String createdInGapsFrom = wrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getCreatedInGapsFrom();
        log.info("isDocmosisLetter: {}, DocmosisLetterTemplate: {}, isInterlocLetter: {}, LetterTemplate: {}, createdInGapsFrom: {}",
            isDocmosisLetter, notification.getDocmosisLetterTemplate(),
            isInterlocLetter, notification.getLetterTemplate(),
            createdInGapsFrom);
        return allowNonInterlocLetterToBeSent(notification, isInterlocLetter, createdInGapsFrom)
            || allowInterlocLetterToBeSent(notification, isInterlocLetter)
            || allowDocmosisLetterToBeSent(notification, isDocmosisLetter);
    }

    private boolean allowDocmosisLetterToBeSent(Notification notification, boolean isDocmosisLetter) {
        return isDocmosisLetter && isNotBlank(notification.getDocmosisLetterTemplate());
    }

    private boolean allowInterlocLetterToBeSent(Notification notification, boolean isInterlocLetter) {
        return isInterlocLetter && isNotBlank(notification.getLetterTemplate());
    }

    private boolean allowNonInterlocLetterToBeSent(Notification notification, boolean isInterlocLetter, String createdInGapsFrom) {
        return !isInterlocLetter && isNotBlank(notification.getLetterTemplate()) && State.READY_TO_LIST.getId().equals(createdInGapsFrom);
    }

    private boolean sendSmsNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification, NotificationEventType eventType) {
        if (isOkToSendSmsNotification(wrapper, subscription, notification, eventType, notificationValidService)) {
            return Optional.ofNullable(notification.getSmsTemplate()).map(Collection::stream).orElseGet(Stream::empty)
                .map(smsTemplateId -> sendSmsNotification(wrapper, notification, smsTemplateId)).reduce((previous, current) -> previous && current).orElse(false);
        }
        return false;
    }

    private boolean sendSmsNotification(NotificationWrapper wrapper, Notification notification, String smsTemplateId) {
        NotificationHandler.SendNotification sendNotification = () ->
            notificationSender.sendSms(
                smsTemplateId,
                notification.getMobile(),
                notification.getPlaceholders(),
                notification.getReference(),
                notification.getSmsSenderTemplate(),
                wrapper.getNotificationType(),
                wrapper.getNewSscsCaseData()
            );
        log.info("In sendSmsNotification method notificationSender is available {} ", notificationSender != null);

        notificationLog(notification, "sms", notification.getMobile(), wrapper);

        return notificationHandler.sendNotification(wrapper, smsTemplateId, "SMS", sendNotification);
    }

    private boolean sendEmailNotification(NotificationWrapper wrapper, Subscription subscription, Notification notification) {
        if (isOkToSendEmailNotification(wrapper, subscription, notification, notificationValidService)) {

            NotificationHandler.SendNotification sendNotification = () ->
                notificationSender.sendEmail(
                    notification.getEmailTemplate(),
                    notification.getEmail(),
                    notification.getPlaceholders(),
                    notification.getReference(),
                    wrapper.getNotificationType(),
                    wrapper.getNewSscsCaseData()
                );

            log.info("In sendEmailNotification method notificationSender is available {} ", notificationSender != null);

            notificationLog(notification, "email", notification.getEmail(), wrapper);

            return notificationHandler.sendNotification(wrapper, notification.getEmailTemplate(), "Email", sendNotification);
        }

        return false;
    }

    protected boolean sendLetterNotification(NotificationWrapper wrapper, Notification notification, SubscriptionWithType subscriptionWithType, NotificationEventType eventType) {
        log.info("Sending the letter for event {} and case id {}.", eventType.getId(), wrapper.getCaseId());
        Address addressToUse = getAddressToUseForLetter(wrapper, subscriptionWithType);

        if (isValidLetterAddress(addressToUse)) {
            return sendMandatoryLetterNotification(wrapper, notification, subscriptionWithType, addressToUse);
        } else {
            log.error("Failed to send letter for event id: {} for case id: {}, no address present", wrapper.getNotificationType().getId(), wrapper.getCaseId());
            return false;
        }
    }

    private boolean sendMandatoryLetterNotification(NotificationWrapper wrapper, Notification notification, SubscriptionWithType subscriptionWithType, Address addressToUse) {
        if (NotificationEventTypeLists.EVENT_TYPES_FOR_MANDATORY_LETTERS.contains(wrapper.getNotificationType())) {
            if (isBundledLetter(wrapper.getNotificationType()) || (isNotBlank(notification.getDocmosisLetterTemplate()))) {
                return sendBundledAndDocmosisLetterNotification(wrapper, notification, getNameToUseForLetter(wrapper, subscriptionWithType), subscriptionWithType);
            } else if (hasLetterTemplate(notification)) {
                NotificationHandler.SendNotification sendNotification = () ->
                    sendLetterNotificationToAddress(wrapper, notification, addressToUse, subscriptionWithType);

                return notificationHandler.sendNotification(wrapper, notification.getLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
            }
        }
        return false;
    }

    protected void sendLetterNotificationToAddress(NotificationWrapper wrapper, Notification notification, final Address address, SubscriptionWithType subscriptionWithType) throws NotificationClientException {
        if (address != null) {
            Map<String, Object> placeholders = notification.getPlaceholders();
            String fullNameNoTitle = getNameToUseForLetter(wrapper, subscriptionWithType);

            placeholders.putAll(getAddressPlaceholders(address, fullNameNoTitle));

            placeholders.put(NAME, fullNameNoTitle);
            if (SubscriptionType.REPRESENTATIVE.equals(subscriptionWithType.getSubscriptionType())) {
                placeholders.put(REPRESENTATIVE_NAME, fullNameNoTitle);
            }
            placeholders.put(APPELLANT_NAME, wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName().getFullNameNoTitle());

            placeholders.put(CLAIMANT_NAME, wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName().getFullNameNoTitle());

            if (!placeholders.containsKey(APPEAL_RESPOND_DATE)) {
                ZonedDateTime appealReceivedDate = ZonedDateTime.now().plusSeconds(delay);
                placeholders.put(APPEAL_RESPOND_DATE, appealReceivedDate.format(DateTimeFormatter.ofPattern(AppConstants.RESPONSE_DATE_FORMAT)));
            }

            log.info("In sendLetterNotificationToAddress method notificationSender is available {} ", notificationSender != null);
            notificationLog(notification, "GovNotify letter", address.getPostcode(), wrapper);

            notificationSender.sendLetter(
                notification.getLetterTemplate(),
                address,
                notification.getPlaceholders(),
                wrapper.getNotificationType(),
                fullNameNoTitle,
                wrapper.getCaseId()
            );
        }
    }
    
    public static Map<String, Object> getAddressPlaceholders(final Address address, String fullNameNoTitle) {
        Map<String, Object> addressPlaceHolders = new HashMap<>();

        addressPlaceHolders.put(ADDRESS_LINE_1, fullNameNoTitle);

        List<String> addressConstants = List.of(ADDRESS_LINE_2, ADDRESS_LINE_3, ADDRESS_LINE_4,
                ADDRESS_LINE_5, POSTCODE_LITERAL);

        List<String> lines = lines(address);

        for (int i = 0; i < lines.size(); i++) {
            addressPlaceHolders.put(addressConstants.get(i), defaultToEmptyStringIfNull(lines.get(i)));
        }
        return addressPlaceHolders;
    }

    private static boolean isValidLetterAddress(Address addressToUse) {
        return null != addressToUse
            && isNotBlank(addressToUse.getLine1())
            && isNotBlank(addressToUse.getPostcode());
    }

    private boolean sendBundledAndDocmosisLetterNotification(NotificationWrapper wrapper, Notification notification, String nameToUse, SubscriptionWithType subscriptionWithType) {
        try {
            byte[] bundledLetter;
            if (isNotBlank(notification.getDocmosisLetterTemplate())) {
                byte[] letter = pdfLetterService.generateLetter(wrapper, notification, subscriptionWithType);
                final byte[] associatedCasePdf = downloadAssociatedCasePdf(wrapper);
                if (ArrayUtils.isNotEmpty(associatedCasePdf)) {
                    letter = buildBundledLetter(addBlankPageAtTheEndIfOddPage(letter), associatedCasePdf);
                }

                byte[] coversheet = pdfLetterService.buildCoversheet(wrapper, subscriptionWithType);
                if (ArrayUtils.isNotEmpty(coversheet)) {
                    letter = buildBundledLetter(addBlankPageAtTheEndIfOddPage(letter), coversheet);
                }
                bundledLetter = letter;

                boolean alternativeLetterFormat = isAlternativeLetterFormatRequired(wrapper, subscriptionWithType);
                NotificationHandler.SendNotification sendNotification = alternativeLetterFormat
                    ? () -> notificationSender.saveLettersToReasonableAdjustment(bundledLetter,
                    wrapper.getNotificationType(),
                    nameToUse,
                    wrapper.getCaseId(),
                    subscriptionWithType.getSubscriptionType())
                    : () -> notificationSender.sendBundledLetter(
                    wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getPostcode(),   // Used for whitelisting only
                    bundledLetter,
                    wrapper.getNotificationType(),
                    nameToUse,
                    wrapper.getCaseId());

                log.info("In sendBundledAndDocmosisLetterNotification method notificationSender is available {} ", notificationSender != null);

                notificationLog(notification, "Docmosis Letter", nameToUse, wrapper);

                if (ArrayUtils.isNotEmpty(bundledLetter)) {
                    notificationHandler.sendNotification(wrapper, notification.getDocmosisLetterTemplate(), NOTIFICATION_TYPE_LETTER, sendNotification);
                    return true;
                }
            }
        } catch (IOException ioe) {
            NotificationServiceException exception = new NotificationServiceException(wrapper.getCaseId(), ioe);
            log.error("Error on GovUKNotify for case id: " + wrapper.getCaseId() + ", sendBundledAndDocmosisLetterNotification", exception);
            throw exception;
        }
        return false;
    }

    private void notificationLog(Notification notification, String notificationType, String recipient, NotificationWrapper wrapper) {
        Object partyType = Optional.ofNullable(notification)
            .map(Notification::getPlaceholders)
            .map(map -> map.get(PARTY_TYPE))
            .orElse(null);
        Object entityType = Optional.ofNullable(notification)
            .map(Notification::getPlaceholders)
            .map(map -> map.get(ENTITY_TYPE))
            .orElse(null);
        log.info("Sending {} Notification for Party {}, Entity {}, Contact {} and Notification Type {}",
            notificationType, partyType, entityType, recipient, wrapper.getNotificationType());
    }

    private byte[] downloadAssociatedCasePdf(NotificationWrapper wrapper) {
        NotificationEventType notificationEventType = wrapper.getSscsCaseDataWrapper().getNotificationEventType();
        SscsCaseData newSscsCaseData = wrapper.getNewSscsCaseData();

        byte[] associatedCasePdf = null;
        String documentUrl = getBundledLetterDocumentUrl(notificationEventType, newSscsCaseData);

        if (null != documentUrl) {
            associatedCasePdf = pdfStoreService.download(documentUrl);
        }
        return associatedCasePdf;
    }

    protected static String getBundledLetterDocumentUrl(NotificationEventType notificationEventType, SscsCaseData newSscsCaseData) {
        if (DIRECTION_ISSUED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DIRECTION_NOTICE));
        } else if (DECISION_ISSUED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DECISION_NOTICE));
        } else if (ISSUE_FINAL_DECISION.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(FINAL_DECISION_NOTICE));
        } else if (ISSUE_FINAL_DECISION_WELSH.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestWelshDocumentForDocumentType(FINAL_DECISION_NOTICE).orElse(null));
        } else if (ISSUE_ADJOURNMENT_NOTICE.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(ADJOURNMENT_NOTICE));
        } else if (ISSUE_ADJOURNMENT_NOTICE_WELSH.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(ADJOURNMENT_NOTICE));
        } else if (DECISION_ISSUED_WELSH.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestWelshDocumentForDocumentType(DECISION_NOTICE).orElse(null));
        } else if (DIRECTION_ISSUED_WELSH.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestWelshDocumentForDocumentType(DIRECTION_NOTICE).orElse(null));
        } else if (PROCESS_AUDIO_VIDEO.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE));
        } else if (PROCESS_AUDIO_VIDEO_WELSH.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestWelshDocumentForDocumentType(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE).orElse(null));
        } else if (ACTION_POSTPONEMENT_REQUEST.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(POSTPONEMENT_REQUEST_DIRECTION_NOTICE));
        } else if (ACTION_POSTPONEMENT_REQUEST_WELSH.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestWelshDocumentForDocumentType(POSTPONEMENT_REQUEST_DIRECTION_NOTICE).orElse(null));
        } else if (CORRECTION_GRANTED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DocumentType.CORRECTION_GRANTED));
        } else if (NotificationEventType.CORRECTION_REFUSED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DocumentType.CORRECTION_REFUSED));
        } else if (REVIEW_AND_SET_ASIDE.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DocumentType.REVIEW_AND_SET_ASIDE));
        } else if (PERMISSION_TO_APPEAL_GRANTED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DocumentType.PERMISSION_TO_APPEAL_GRANTED));
        } else if (PERMISSION_TO_APPEAL_REFUSED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DocumentType.PERMISSION_TO_APPEAL_REFUSED));
        } else if (SET_ASIDE_GRANTED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DocumentType.SET_ASIDE_GRANTED));
        } else if (SET_ASIDE_REFUSED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DocumentType.SET_ASIDE_REFUSED));
        } else if (SOR_EXTEND_TIME.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(STATEMENT_OF_REASONS_GRANTED));
        } else if (SOR_REFUSED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(STATEMENT_OF_REASONS_REFUSED));
        } else if (NotificationEventType.LIBERTY_TO_APPLY_GRANTED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DocumentType.LIBERTY_TO_APPLY_GRANTED));
        } else if (NotificationEventType.LIBERTY_TO_APPLY_REFUSED.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(DocumentType.LIBERTY_TO_APPLY_REFUSED));
        } else if (ADMIN_CORRECTION_HEADER.equals(notificationEventType)) {
            return getDocumentForType(newSscsCaseData.getLatestDocumentForDocumentType(CORRECTED_DECISION_NOTICE));
        }

        return null;
    }

    private static String getDocumentForType(AbstractDocument sscsDocument) {
        if (sscsDocument != null) {
            return sscsDocument.getValue().getDocumentLink().getDocumentUrl();
        }
        return null;
    }

    private static String defaultToEmptyStringIfNull(String value) {
        return (value == null) ? StringUtils.EMPTY : value;
    }

}
