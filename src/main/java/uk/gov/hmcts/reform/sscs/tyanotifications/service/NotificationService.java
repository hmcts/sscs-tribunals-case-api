package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCodeOrThrowException;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SUBSCRIPTION_UPDATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.getSubscription;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.isOkToSendNotification;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationValidService.isMandatoryLetterEventType;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.utility.PhoneNumbersUtil;

@Service
@Slf4j
public class NotificationService {
    private static final List<String> PROCESS_AUDIO_VIDEO_ACTIONS_THAT_REQUIRES_NOTICE = asList("issueDirectionsNotice", "excludeEvidence", "admitEvidence");
    private static final String READY_TO_LIST = "readyToList";

    private final NotificationFactory notificationFactory;
    private final ReminderService reminderService;
    private final NotificationValidService notificationValidService;
    private final NotificationHandler notificationHandler;
    private final OutOfHoursCalculator outOfHoursCalculator;
    private final NotificationConfig notificationConfig;

    @SuppressWarnings("squid:S107")
    @Autowired
    public NotificationService(
        NotificationFactory notificationFactory,
        ReminderService reminderService,
        NotificationValidService notificationValidService,
        NotificationHandler notificationHandler,
        OutOfHoursCalculator outOfHoursCalculator,
        NotificationConfig notificationConfig,
        SendNotificationService sendNotificationService,
        @Value("${feature.covid19}") boolean covid19Feature) {

        this.notificationFactory = notificationFactory;
        this.reminderService = reminderService;
        this.notificationValidService = notificationValidService;
        this.notificationHandler = notificationHandler;
        this.outOfHoursCalculator = outOfHoursCalculator;
        this.notificationConfig = notificationConfig;
        this.sendNotificationService = sendNotificationService;
        this.covid19Feature = covid19Feature;
    }

    private final SendNotificationService sendNotificationService;

    private final boolean covid19Feature;

    public void manageNotificationAndSubscription(NotificationWrapper notificationWrapper, boolean fromReminderService) {
        NotificationEventType notificationType = notificationWrapper.getNotificationType();
        final String caseId = notificationWrapper.getCaseId();

        log.info("Checking if notification event {} is valid for case id {}", notificationType.getId(), caseId);
        if (!isEventAllowedToProceedWithValidData(notificationWrapper, notificationType)) {
            return;
        }

        log.info("Notification event triggered {} for case id {}", notificationType.getId(), caseId);

        if (notificationType.isAllowOutOfHours() || !outOfHoursCalculator.isItOutOfHours()) {
            if (notificationType.isToBeDelayed()
                && !fromReminderService
                && !functionalTest(notificationWrapper.getNewSscsCaseData())) {
                log.info("Notification event {} is delayed and scheduled for case id {}", notificationType.getId(), caseId);
                notificationHandler.scheduleNotification(notificationWrapper, ZonedDateTime.now().plusSeconds(notificationType.getDelayInSeconds()));
            } else {
                log.info("Sending notification for Notification event {} and case id {}", notificationType.getId(), caseId);
                sendNotificationPerSubscription(notificationWrapper);
                reminderService.createReminders(notificationWrapper);
                sendSecondNotification(notificationWrapper);
            }
        } else if (outOfHoursCalculator.isItOutOfHours()) {
            log.info("Notification event {} is out of hours and scheduled for case id {}", notificationType.getId(), caseId);
            notificationHandler.scheduleNotification(notificationWrapper);
        }
    }

    private boolean functionalTest(SscsCaseData newSscsCaseData) {
        return YesNo.isYes(newSscsCaseData.getFunctionalTest());
    }

    private void sendSecondNotification(NotificationWrapper notificationWrapper) {
        if (notificationWrapper.getNotificationType().equals(ISSUE_FINAL_DECISION_WELSH)) {
            // Gov Notify has a limit of 10 pages, so for long notifications (especially Welsh) we need to split the sending into 2 parts
            log.info("Trigger second notification event for {}", ISSUE_FINAL_DECISION.getId());
            notificationWrapper.getSscsCaseDataWrapper().setNotificationEventType(ISSUE_FINAL_DECISION);
            notificationWrapper.setSwitchLanguageType(true);
            sendNotificationPerSubscription(notificationWrapper);
        } else if (notificationWrapper.getNotificationType().equals(DWP_UPLOAD_RESPONSE)) {
            log.info("Trigger second notification event for {}", UPDATE_OTHER_PARTY_DATA.getId());
            notificationWrapper.getSscsCaseDataWrapper().setNotificationEventType(UPDATE_OTHER_PARTY_DATA);
            sendNotificationPerSubscription(notificationWrapper);
        }
    }

    private void sendNotificationPerSubscription(NotificationWrapper notificationWrapper) {
        overrideNotificationType(notificationWrapper);
        String subscriptionTypes = notificationWrapper.getSubscriptionsBasedOnNotificationType().stream()
            .map(sub -> String.format("Party: %s, Entity %s, Party Id %s, Subscription Type %s, Subscription %s",
                Optional.ofNullable(sub.getParty()).map(Object::getClass).orElse(null),
                Optional.ofNullable(sub.getEntity()).map(Object::getClass).orElse(null),
                sub.getPartyId(),
                sub.getSubscriptionType(),
                sub.getSubscription()))
            .collect(Collectors.joining("\n", "\n", ""));
        log.info("Processing for the Notification Type {} and Case Id {} the following subscriptions: {}",
            notificationWrapper.getNotificationType(),
            notificationWrapper.getCaseId(),
            subscriptionTypes);

        for (SubscriptionWithType subscriptionWithType : notificationWrapper.getSubscriptionsBasedOnNotificationType()) {
            if (isSubscriptionValidToSendAfterOverride(notificationWrapper, subscriptionWithType)
                && isValidNotification(notificationWrapper, subscriptionWithType)) {
                sendNotification(notificationWrapper, subscriptionWithType);
                resendLastNotification(notificationWrapper, subscriptionWithType);
            } else {
                log.error("Is not a valid notification event {} for case id {}, not sending notification.",
                    notificationWrapper.getNotificationType().getId(), notificationWrapper.getCaseId());
            }
        }
    }

    private void resendLastNotification(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        if (subscriptionWithType.getSubscription() != null && shouldProcessLastNotification(notificationWrapper, subscriptionWithType)) {
            NotificationEventType lastEvent = NotificationEventType.getNotificationByCcdEvent(notificationWrapper.getNewSscsCaseData().getEvents().get(0)
                .getValue().getEventType());
            log.info("Resending the last notification for event {} and case id {}.", lastEvent.getId(), notificationWrapper.getCaseId());
            scrubEmailAndSmsIfSubscribedBefore(notificationWrapper, subscriptionWithType);
            notificationWrapper.getSscsCaseDataWrapper().setNotificationEventType(lastEvent);
            sendNotification(notificationWrapper, subscriptionWithType);
            notificationWrapper.getSscsCaseDataWrapper().setNotificationEventType(NotificationEventType.SUBSCRIPTION_UPDATED);
        }
    }

    private void overrideNotificationType(NotificationWrapper wrapper) {

        if (REISSUE_DOCUMENT.equals(wrapper.getNotificationType()) && null != wrapper.getNewSscsCaseData().getReissueArtifactUi().getReissueFurtherEvidenceDocument()) {
            String code = wrapper.getNewSscsCaseData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getValue().getCode();
            if (code.equals(EventType.ISSUE_FINAL_DECISION.getCcdType())) {
                wrapper.setNotificationType(ISSUE_FINAL_DECISION);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.ISSUE_FINAL_DECISION_WELSH.getCcdType())) {
                wrapper.setNotificationType(ISSUE_FINAL_DECISION_WELSH);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.DECISION_ISSUED.getCcdType())) {
                wrapper.setNotificationType(DECISION_ISSUED);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.DIRECTION_ISSUED.getCcdType())) {
                wrapper.setNotificationType(DIRECTION_ISSUED);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.ISSUE_ADJOURNMENT_NOTICE.getCcdType())) {
                wrapper.setNotificationType(ISSUE_ADJOURNMENT_NOTICE);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.ISSUE_ADJOURNMENT_NOTICE_WELSH.getCcdType())) {
                wrapper.setNotificationType(ISSUE_ADJOURNMENT_NOTICE_WELSH);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.DECISION_ISSUED_WELSH.getCcdType())) {
                wrapper.setNotificationType(DECISION_ISSUED_WELSH);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.DIRECTION_ISSUED_WELSH.getCcdType())) {
                wrapper.setNotificationType(DIRECTION_ISSUED_WELSH);
                wrapper.setNotificationEventTypeOverridden(true);
            }
        } else if (DRAFT_TO_VALID_APPEAL_CREATED.equals(wrapper.getNotificationType())) {
            wrapper.setNotificationType(VALID_APPEAL_CREATED);
        } else if (DRAFT_TO_NON_COMPLIANT.equals(wrapper.getNotificationType())) {
            wrapper.setNotificationType(NON_COMPLIANT);
        }
    }

    private static boolean isSubscriptionValidToSendAfterOverride(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType) {
        if (wrapper.hasNotificationEventBeenOverridden()) {
            if ((APPELLANT.equals(subscriptionWithType.getSubscriptionType()) || APPOINTEE.equals(subscriptionWithType.getSubscriptionType()))
                && !YesNo.YES.equals(wrapper.getNewSscsCaseData().getReissueArtifactUi().getResendToAppellant())) {
                return false;
            }

            if (REPRESENTATIVE.equals(subscriptionWithType.getSubscriptionType())
                && !YesNo.YES.equals(wrapper.getNewSscsCaseData().getReissueArtifactUi().getResendToRepresentative())) {
                return false;
            }

            return !OTHER_PARTY.equals(subscriptionWithType.getSubscriptionType()) || isResendTo(subscriptionWithType.getPartyId(), wrapper.getNewSscsCaseData());
        }
        return true;
    }

    private static boolean isResendTo(String partyId, SscsCaseData sscsCaseData) {
        return nonNull(partyId)
            && emptyIfNull(sscsCaseData.getReissueArtifactUi().getOtherPartyOptions()).stream()
            .map(OtherPartyOption::getValue)
            .filter(otherPartyOptionDetails -> partyId.equals(otherPartyOptionDetails.getOtherPartyOptionId()))
            .anyMatch(otherPartyOptionDetails -> YesNo.isYes(otherPartyOptionDetails.getResendToOtherParty()));
    }

    private void scrubEmailAndSmsIfSubscribedBefore(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        Subscription oldSubscription = getSubscription(notificationWrapper.getOldSscsCaseData(), subscriptionWithType.getSubscriptionType());
        Subscription newSubscription = subscriptionWithType.getSubscription();
        String email = oldSubscription != null && oldSubscription.isEmailSubscribed() ? null : newSubscription.getEmail();
        String mobile = oldSubscription != null && oldSubscription.isSmsSubscribed() ? null : newSubscription.getMobile();
        subscriptionWithType.setSubscription(newSubscription.toBuilder().email(email).mobile(mobile).build());
    }

    private boolean shouldProcessLastNotification(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        return NotificationEventType.SUBSCRIPTION_UPDATED.equals(notificationWrapper.getSscsCaseDataWrapper().getNotificationEventType())
            && hasCaseJustSubscribed(subscriptionWithType.getSubscription(), getSubscription(notificationWrapper.getOldSscsCaseData(), subscriptionWithType.getSubscriptionType()))
            && thereIsALastEventThatIsNotSubscriptionUpdated(notificationWrapper.getNewSscsCaseData());
    }

    static Boolean hasCaseJustSubscribed(Subscription newSubscription, Subscription oldSubscription) {
        return ((oldSubscription == null || !oldSubscription.isEmailSubscribed()) && newSubscription.isEmailSubscribed()
            || ((oldSubscription == null || !oldSubscription.isSmsSubscribed()) && newSubscription.isSmsSubscribed()));
    }

    private static boolean thereIsALastEventThatIsNotSubscriptionUpdated(final SscsCaseData newSscsCaseData) {
        boolean thereIsALastEventThatIsNotSubscriptionUpdated = newSscsCaseData.getEvents() != null
            && !newSscsCaseData.getEvents().isEmpty()
            && newSscsCaseData.getEvents().get(0).getValue().getEventType() != null
            && !SUBSCRIPTION_UPDATED.equals(newSscsCaseData.getEvents().get(0).getValue().getEventType());
        if (!thereIsALastEventThatIsNotSubscriptionUpdated) {
            log.info("Not re-sending the last subscription as there is no last event for ccdCaseId {}.", newSscsCaseData.getCcdCaseId());
        }
        return thereIsALastEventThatIsNotSubscriptionUpdated;
    }

    private void sendNotification(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        Notification notification = notificationFactory.create(notificationWrapper, subscriptionWithType);
        sendNotificationService.sendEmailSmsLetterNotification(notificationWrapper, notification, subscriptionWithType, notificationWrapper.getNotificationType());
        processOldSubscriptionNotifications(notificationWrapper, notification, subscriptionWithType, notificationWrapper.getNotificationType());
    }

    private boolean isValidNotification(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType) {
        Subscription subscription = subscriptionWithType.getSubscription();

        return (isMandatoryLetterEventType(wrapper.getNotificationType())
            || isOkToSendNotification(wrapper, wrapper.getNotificationType(), subscription, notificationValidService));
    }

    private void  processOldSubscriptionNotifications(NotificationWrapper wrapper, Notification notification, SubscriptionWithType subscriptionWithType, NotificationEventType eventType) {
        if (wrapper.getNotificationType() == NotificationEventType.SUBSCRIPTION_UPDATED) {
            Subscription newSubscription;
            Subscription oldSubscription;
            if (REPRESENTATIVE.equals(subscriptionWithType.getSubscriptionType())) {
                newSubscription = wrapper.getNewSscsCaseData().getSubscriptions().getRepresentativeSubscription();
                oldSubscription = wrapper.getOldSscsCaseData().getSubscriptions().getRepresentativeSubscription();
            } else if (APPOINTEE.equals(subscriptionWithType.getSubscriptionType())) {
                newSubscription = wrapper.getNewSscsCaseData().getSubscriptions().getAppointeeSubscription();
                oldSubscription = wrapper.getOldSscsCaseData().getSubscriptions().getAppointeeSubscription();
            } else {
                newSubscription = wrapper.getNewSscsCaseData().getSubscriptions().getAppellantSubscription();
                oldSubscription = wrapper.getOldSscsCaseData().getSubscriptions().getAppellantSubscription();
            }

            String emailAddress = getSubscriptionDetails(newSubscription.getEmail(), oldSubscription.getEmail());
            String smsNumber = getSubscriptionDetails(newSubscription.getMobile(), oldSubscription.getMobile());

            Destination destination = Destination.builder().email(emailAddress)
                .sms(PhoneNumbersUtil.cleanPhoneNumber(smsNumber).orElse(smsNumber)).build();

            Benefit benefit = getBenefitByCodeOrThrowException(wrapper.getSscsCaseDataWrapper()
                .getNewSscsCaseData().getAppeal().getBenefitType().getCode());

            Template template = notificationConfig.getTemplate(
                SUBSCRIPTION_OLD_ID, SUBSCRIPTION_OLD_ID, SUBSCRIPTION_OLD_ID, SUBSCRIPTION_OLD_ID,
                benefit, wrapper, "validAppeal");

            Notification oldNotification = Notification.builder().template(template).appealNumber(notification.getAppealNumber())
                .destination(destination)
                .reference(new Reference(wrapper.getOldSscsCaseData().getCaseReference()))
                .appealNumber(notification.getAppealNumber())
                .placeholders(notification.getPlaceholders()).build();

            SubscriptionWithType updatedSubscriptionWithType = new SubscriptionWithType(oldSubscription,
                subscriptionWithType.getSubscriptionType(), subscriptionWithType.getParty(), subscriptionWithType.getEntity());
            sendNotificationService.sendEmailSmsLetterNotification(wrapper, oldNotification, updatedSubscriptionWithType, eventType);
        }
    }

    private String getSubscriptionDetails(String newSubscription, String oldSubscription) {
        String subscription = "";
        if (null != newSubscription && null != oldSubscription) {
            subscription = newSubscription.equals(oldSubscription) ? null : oldSubscription;
        } else if (null == newSubscription && null != oldSubscription) {
            subscription = oldSubscription;
        }
        return subscription;
    }

    boolean isNotificationStillValidToSendSetAsideRequest(SscsCaseData caseData, NotificationEventType eventType) {
        List<String> originalSenders = Arrays.asList("dwp", "hmcts");
        if (EVENTS_FOR_ACTION_FURTHER_EVIDENCE.contains(eventType)) {
            return nonNull(caseData.getOriginalSender())
                && !originalSenders.contains(caseData.getOriginalSender().getValue().getCode());
        }
        return true;
    }

    private boolean isEventAllowedToProceedWithValidData(NotificationWrapper notificationWrapper,
                                                         NotificationEventType notificationType) {
        if (REQUEST_FOR_INFORMATION.equals(notificationType)
            && !isYes(notificationWrapper.getNewSscsCaseData().getInformationFromAppellant())) {
            log.info("Request for Information with empty or no Information From Appellant for ccdCaseId {}.",
                notificationWrapper.getNewSscsCaseData().getCcdCaseId());
            return false;
        }

        if (!isNotificationStillValidToSendSetAsideRequest(notificationWrapper.getNewSscsCaseData(), notificationType)) {
            log.info("Incomplete Information with empty or no Information regarding sender for event {}.",
                notificationType.getId());
            return false;
        }

        if (notificationWrapper.getSscsCaseDataWrapper().getState() != null
            && notificationWrapper.getSscsCaseDataWrapper().getState().equals(State.DORMANT_APPEAL_STATE)
            && !EVENT_TYPES_FOR_DORMANT_CASES.contains(notificationType)) {
            log.info("Cannot complete notification {} as the appeal was dormant for caseId {}.",
                notificationType.getId(), notificationWrapper.getCaseId());
            return false;
        }

        if (HEARING_BOOKED.equals(notificationType)
            && DwpState.FINAL_DECISION_ISSUED.equals(notificationWrapper.getNewSscsCaseData().getDwpState())) {
            log.info("Cannot complete notification {} as the notification has been fired in error for caseId {}.",
                notificationType.getId(), notificationWrapper.getCaseId());
            return false;
        }

        if (notificationWrapper.getNewSscsCaseData().isLanguagePreferenceWelsh()
            && (EVENT_TYPES_NOT_FOR_WELSH_CASES.contains(notificationType))) {
            log.info("Cannot complete notification {} as the appeal is Welsh for caseId {}.",
                notificationType.getId(), notificationWrapper.getCaseId());
            return false;
        }
        final String processAudioVisualAction = ofNullable(notificationWrapper.getNewSscsCaseData().getProcessAudioVideoAction())
            .map(f -> f.getValue().getCode()).orElse(null);

        if (notificationType.equals(PROCESS_AUDIO_VIDEO)
            && !PROCESS_AUDIO_VIDEO_ACTIONS_THAT_REQUIRES_NOTICE.contains(processAudioVisualAction)) {
            log.info("Cannot complete notification {} since the action {} does not require a notice to be sent for caseId {}.",
                notificationType.getId(), processAudioVisualAction, notificationWrapper.getCaseId());
            return false;
        }

        if (POSTPONEMENT.equals(notificationType)
            && !LIST_ASSIST.equals(notificationWrapper.getNewSscsCaseData().getSchedulingAndListingFields().getHearingRoute())) {
            log.info("Cannot complete notification {} as the case is not set to list assist for case {}.",
                notificationType.getId(), notificationWrapper.getCaseId());
            return false;
        }

        if (!isDigitalCase(notificationWrapper)
            && DWP_UPLOAD_RESPONSE.equals(notificationType)) {
            log.info("Cannot complete notification {} as the appeal was dwpUploadResponse for caseId {}.",
                notificationType.getId(), notificationWrapper.getCaseId());
            return false;
        }

        if (DWP_RESPONSE_RECEIVED.equals(notificationType)
            && isDigitalCase(notificationWrapper)) {
            log.info("Cannot complete notification {} as the appeal was digital for caseId {}.",
                notificationType.getId(), notificationWrapper.getCaseId());
            return false;
        }

        if (covid19Feature
            && (HEARING_BOOKED.equals(notificationType)
            || HEARING_REMINDER.equals(notificationType))) {
            log.info("Notification not valid to send as covid 19 feature flag on for case id {} and event {} in state {}",
                notificationWrapper.getCaseId(),
                notificationType.getId(),
                notificationWrapper.getSscsCaseDataWrapper().getState());
            return false;
        }

        log.info("Notification valid to send for case id {} and event {} in state {}",
            notificationWrapper.getCaseId(),
            notificationType.getId(),
            notificationWrapper.getSscsCaseDataWrapper().getState());
        return true;
    }

    private boolean isDigitalCase(final NotificationWrapper notificationWrapper) {
        return READY_TO_LIST
            .equals(notificationWrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getCreatedInGapsFrom());
    }
}
