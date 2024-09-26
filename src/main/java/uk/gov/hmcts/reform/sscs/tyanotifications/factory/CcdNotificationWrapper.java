package uk.gov.hmcts.reform.sscs.tyanotifications.factory;

import static java.util.Objects.isNull;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType.ORAL;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType.PAPER;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.*;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.reference.data.model.ConfidentialityPartyMembers;
import uk.gov.hmcts.reform.sscs.reference.data.model.ConfidentialityType;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.scheduler.CcdActionSerializer;

@Slf4j
public class CcdNotificationWrapper implements NotificationWrapper {

    private final NotificationSscsCaseDataWrapper responseWrapper;

    private boolean notificationEventTypeOverridden = false;

    private boolean languageSwitched = false;

    public CcdNotificationWrapper(NotificationSscsCaseDataWrapper responseWrapper) {
        this.responseWrapper = responseWrapper;
    }

    @Override
    public NotificationEventType getNotificationType() {
        return responseWrapper.getNotificationEventType();
    }

    @Override
    public void setNotificationType(NotificationEventType notificationEventType) {
        responseWrapper.setNotificationEventType(notificationEventType);
    }

    @Override
    public SscsCaseData getNewSscsCaseData() {
        return responseWrapper.getNewSscsCaseData();
    }

    @Override
    public Subscription getAppellantSubscription() {
        return responseWrapper.getNewSscsCaseData().getSubscriptions().getAppellantSubscription();
    }

    @Override
    public Subscription getRepresentativeSubscription() {
        return responseWrapper.getNewSscsCaseData().getSubscriptions().getRepresentativeSubscription();
    }

    @Override
    public Subscription getAppointeeSubscription() {
        return responseWrapper.getNewSscsCaseData().getSubscriptions().getAppointeeSubscription();
    }

    @Override
    public Subscription getJointPartySubscription() {
        return responseWrapper.getNewSscsCaseData().getSubscriptions().getJointPartySubscription();
    }

    @Override
    public List<SubscriptionWithType> getOtherPartySubscriptions(SscsCaseData newSscsCaseData, NotificationEventType notificationEventType) {
        return emptyIfNull(newSscsCaseData.getOtherParties()).stream()
            .map(CcdValue::getValue)
            .flatMap(o -> filterOtherPartySubscription(newSscsCaseData, notificationEventType, o).stream())
            .collect(Collectors.toList());
    }

    @Override
    public NotificationSscsCaseDataWrapper getSscsCaseDataWrapper() {
        return responseWrapper;
    }

    @Override
    public String getCaseId() {
        return responseWrapper.getNewSscsCaseData().getCcdCaseId();
    }

    public AppealHearingType getHearingType() {
        final String hearingType = responseWrapper.getNewSscsCaseData().getAppeal().getHearingType();
        AppealHearingType returnHearingType = ORAL;
        if (StringUtils.equalsAnyIgnoreCase(PAPER.name(), hearingType)) {
            returnHearingType = PAPER;
        }
        return returnHearingType;
    }

    @Override
    public String getSchedulerPayload() {
        return new CcdActionSerializer().serialize(getCaseId());
    }

    @Override
    public SscsCaseData getOldSscsCaseData() {
        return responseWrapper.getOldSscsCaseData();
    }

    @Override
    public List<SubscriptionWithType> getSubscriptionsBasedOnNotificationType() {
        List<SubscriptionWithType> subscriptionWithTypeList = new ArrayList<>();

        SscsCaseData newSscsCaseData = getNewSscsCaseData();
        SscsCaseData oldSscsCaseData = getOldSscsCaseData();
        Appeal appeal = newSscsCaseData.getAppeal();
        Appellant appellant = appeal.getAppellant();
        JointParty jointParty = newSscsCaseData.getJointParty();
        NotificationEventType notificationEventType = getNotificationType();

        if (isNotificationEventValidToSendToAppointee(newSscsCaseData, oldSscsCaseData, notificationEventType)) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getAppointeeSubscription(), APPOINTEE,
                appellant, appellant.getAppointee()));
        } else if (isNotificationEventValidToSendToAppellant(newSscsCaseData, oldSscsCaseData, notificationEventType)) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getAppellantSubscription(), APPELLANT,
                appellant, appellant));
        }

        if (isNotificationEventValidToSendToRep(newSscsCaseData, oldSscsCaseData, notificationEventType)) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getRepresentativeSubscription(), REPRESENTATIVE,
                appellant, appeal.getRep()));
        }

        if (isNotificationEventValidToSendToJointParty(newSscsCaseData, oldSscsCaseData, notificationEventType)) {
            subscriptionWithTypeList.add(new SubscriptionWithType(getJointPartySubscription(), JOINT_PARTY,
                jointParty, jointParty));
        }

        subscriptionWithTypeList.addAll(getOtherPartySubscriptions(newSscsCaseData, notificationEventType));

        return subscriptionWithTypeList;
    }

    private List<SubscriptionWithType> filterOtherPartySubscription(SscsCaseData newSscsCaseData, NotificationEventType notificationEventType, OtherParty otherParty) {
        List<SubscriptionWithType> otherPartySubscription = new ArrayList<>();

        log.info("isSendNewOtherPartyNotification {}", otherParty.getSendNewOtherPartyNotification());
        log.info("Notification Type {}", notificationEventType);
        log.info("Other Party id {} isSendNewOtherPartyNotification {}", otherParty.getId(), otherParty.getSendNewOtherPartyNotification());

        boolean isSendNewOtherPartyNotification = YesNo.isYes(otherParty.getSendNewOtherPartyNotification());

        if (hasAppointee(otherParty.getAppointee(), otherParty.getIsAppointee())
            && isNotificationEventValidToSendToOtherPartySubscription(otherParty.getOtherPartyAppointeeSubscription(), isSendNewOtherPartyNotification, newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.OTHER_PARTY_APPOINTEE.getCode())) {
            otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartyAppointeeSubscription(),
                OTHER_PARTY, otherParty, otherParty.getAppointee(), otherParty.getAppointee().getId()));
        } else if (isNotificationEventValidToSendToOtherPartySubscription(otherParty.getOtherPartySubscription(), isSendNewOtherPartyNotification, newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.OTHER_PARTY.getCode())) {
            otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartySubscription(), OTHER_PARTY,
                otherParty, otherParty, otherParty.getId()));
        }

        if (hasRepresentative(otherParty)
            && isNotificationEventValidToSendToOtherPartySubscription(otherParty.getOtherPartyRepresentativeSubscription(), isSendNewOtherPartyNotification, newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.OTHER_PARTY_REP.getCode())) {
            otherPartySubscription.add(new SubscriptionWithType(otherParty.getOtherPartyRepresentativeSubscription(),
                OTHER_PARTY, otherParty, otherParty.getRep(), otherParty.getRep().getId()));
        }

        log.info("Number of subscription {}", otherPartySubscription.size());

        return otherPartySubscription;
    }

    private boolean isOtherPartyPresent(SscsCaseData sscsCaseData) {
        return sscsCaseData.getOtherParties() != null && sscsCaseData.getOtherParties().size() > 0;
    }

    private List<String> getEligiblePartyMembersInTheCaseToSendNotification(SscsCaseData caseData) {
        List<String> eligiblePartyMembers = new ArrayList<>();
        // the party members must exist in the case and the user has selected to send the notification via the radio button in issue direction notice.
        if (YesNo.isYes(caseData.getSendDirectionNoticeToAppellantOrAppointee())) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode());
        }
        if (YesNo.isYes(caseData.getSendDirectionNoticeToJointParty()) && caseData.isThereAJointParty()) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.JOINT_PARTY.getCode());
        }

        boolean hasOtherPartyAppointee = Optional.ofNullable(caseData.getOtherParties()).orElse(Collections.emptyList()).stream().map(CcdValue::getValue).anyMatch(OtherParty::hasAppointee);
        if (YesNo.isYes(caseData.getSendDirectionNoticeToOtherParty()) && isOtherPartyPresent(caseData) && !hasOtherPartyAppointee) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.OTHER_PARTY.getCode());
        }
        if (YesNo.isYes(caseData.getSendDirectionNoticeToOtherPartyAppointee()) && hasOtherPartyAppointee) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.OTHER_PARTY_APPOINTEE.getCode());
        }
        boolean hasOtherPartyRep = Optional.ofNullable(caseData.getOtherParties()).orElse(Collections.emptyList()).stream().map(CcdValue::getValue).anyMatch(OtherParty::hasRepresentative);
        if (YesNo.isYes(caseData.getSendDirectionNoticeToOtherPartyRep()) && hasOtherPartyRep) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.OTHER_PARTY_REP.getCode());
        }
        if (YesNo.isYes(caseData.getSendDirectionNoticeToRepresentative()) && caseData.isThereARepresentative()) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.REPRESENTATIVE.getCode());
        }
        if (YesNo.isYes(caseData.getSendDirectionNoticeToFTA())) {
            eligiblePartyMembers.add(ConfidentialityPartyMembers.FTA.getCode());
        }

        return eligiblePartyMembers;
    }

    private boolean canSendBasedOnConfidentiality(SscsCaseData newSscsCaseData, NotificationEventType notificationEventType, String partyMember) {
        if (!(DIRECTION_ISSUED.equals(notificationEventType)
            || DIRECTION_ISSUED_WELSH.equals(notificationEventType))) {
            return true;
        }

        String confidentialityType = newSscsCaseData.getConfidentialityType();
        if (isNull(confidentialityType)
            || ConfidentialityType.GENERAL.getCode().equalsIgnoreCase(confidentialityType)) {
            return true;
        }

        List<String> eligiblePartyMembers = getEligiblePartyMembersInTheCaseToSendNotification(newSscsCaseData);
        log.info("For caseID: {}, canSendNotificationBasedOnConfidentiality, notificationEventType: {}, partyMember: {}, eligiblePartyMembers: {}", newSscsCaseData.getCcdCaseId(), notificationEventType.getId(), partyMember, String.join(", ", eligiblePartyMembers));
        return eligiblePartyMembers.contains(partyMember);
    }

    private boolean isNotificationEventValidToSendToAppointee(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData, NotificationEventType notificationEventType) {
        boolean isValid = hasAppointeeSubscriptionOrIsMandatoryAppointeeLetter(responseWrapper)
            && (EVENTS_VALID_FOR_ALL_ENTITIES.contains(notificationEventType)
            || EVENTS_VALID_FOR_APPOINTEE.contains(notificationEventType)
            || isValidProcessHearingRequestEventForParty(newSscsCaseData, oldSscsCaseData, notificationEventType, PartyItemList.APPELLANT)
            || isValidRequestForInformationEventForParty(newSscsCaseData, notificationEventType, PartyItemList.APPELLANT));
        return canSendBasedOnConfidentiality(newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode()) && isValid;
    }

    private boolean isNotificationEventValidToSendToAppellant(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData, NotificationEventType notificationEventType) {
        boolean isValid = (oldSscsCaseData != null && isValidReviewConfidentialityRequest(notificationEventType, oldSscsCaseData.getConfidentialityRequestOutcomeAppellant(), newSscsCaseData.getConfidentialityRequestOutcomeAppellant()))
            || isValidProcessHearingRequestEventForParty(newSscsCaseData, oldSscsCaseData, notificationEventType, PartyItemList.APPELLANT)
            || isValidRequestForInformationEventForParty(newSscsCaseData, notificationEventType, PartyItemList.APPELLANT)
            || !EVENTS_MAYBE_INVALID_FOR_APPELLANT.contains(notificationEventType);
        return canSendBasedOnConfidentiality(newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.APPELLANT_OR_APPOINTEE.getCode()) && isValid;
    }

    private boolean isValidProcessHearingRequestEventForParty(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData, NotificationEventType notificationEventType, PartyItemList partyItemList) {
        return ACTION_HEARING_RECORDING_REQUEST.equals(notificationEventType) && hasHearingRecordingRequestsForParty(newSscsCaseData, oldSscsCaseData, partyItemList);
    }

    private boolean hasHearingRecordingRequestsForParty(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData, PartyItemList partyItemList) {
        List<HearingRecordingRequest> oldReleasedRecordings = new ArrayList<>();
        if (oldSscsCaseData != null && oldSscsCaseData.getSscsHearingRecordingCaseData() != null) {
            oldReleasedRecordings = Optional.ofNullable(oldSscsCaseData.getSscsHearingRecordingCaseData().getCitizenReleasedHearings())
                .orElse(new ArrayList<>());
        }
        return hasNewReleasedHearingRecordingForParty(newSscsCaseData, oldReleasedRecordings).stream()
            .anyMatch(v -> partyItemList.getCode().equals(v.getValue().getRequestingParty()));
    }

    @NotNull
    private List<HearingRecordingRequest> hasNewReleasedHearingRecordingForParty(SscsCaseData newSscsCaseData, List<HearingRecordingRequest> oldReleasedRecordings) {
        return newSscsCaseData.getSscsHearingRecordingCaseData().getCitizenReleasedHearings().stream()
            .filter(e -> !oldReleasedRecordings.contains(e))
            .collect(Collectors.toList());
    }

    private boolean isNotificationEventValidToSendToRep(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData, NotificationEventType notificationEventType) {
        boolean isValid = hasRepSubscriptionOrIsMandatoryRepLetter(responseWrapper)
            && (EVENTS_VALID_FOR_ALL_ENTITIES.contains(notificationEventType)
            || EVENTS_VALID_FOR_REP.contains(notificationEventType)
            || isValidProcessHearingRequestEventForParty(newSscsCaseData, oldSscsCaseData, notificationEventType, PartyItemList.REPRESENTATIVE)
            || isValidRequestForInformationEventForParty(newSscsCaseData, notificationEventType, PartyItemList.REPRESENTATIVE));
        return canSendBasedOnConfidentiality(newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.REPRESENTATIVE.getCode()) && isValid;
    }

    private boolean isNotificationEventValidToSendToJointParty(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData, NotificationEventType notificationEventType) {
        boolean isValid = hasJointPartySubscription(responseWrapper)
            && (EVENTS_VALID_FOR_ALL_ENTITIES.contains(notificationEventType)
            || EVENTS_VALID_FOR_JOINT_PARTY.contains(notificationEventType)
            || isValidRequestForInformationEventForParty(newSscsCaseData, notificationEventType, PartyItemList.JOINT_PARTY)
            || isValidProcessHearingRequestEventForParty(newSscsCaseData, oldSscsCaseData, notificationEventType, PartyItemList.JOINT_PARTY)
            || (oldSscsCaseData != null && isValidReviewConfidentialityRequest(notificationEventType, oldSscsCaseData.getConfidentialityRequestOutcomeJointParty(), newSscsCaseData.getConfidentialityRequestOutcomeJointParty())));
        return canSendBasedOnConfidentiality(newSscsCaseData, notificationEventType, ConfidentialityPartyMembers.JOINT_PARTY.getCode()) && isValid;
    }

    private boolean isNotificationEventValidToSendToOtherPartySubscription(Subscription subscription, boolean isSendNewOtherPartyNotification, SscsCaseData newSscsCaseData, NotificationEventType notificationEventType, String partyMember) {
        boolean isValid = isValidSubscriptionOrIsMandatoryLetter(subscription, responseWrapper.getNotificationEventType())
            && (EVENTS_VALID_FOR_ALL_ENTITIES.contains(notificationEventType)
            || EVENTS_VALID_FOR_OTHER_PARTY.contains(notificationEventType)
            || (UPDATE_OTHER_PARTY_DATA.equals(notificationEventType) && isSendNewOtherPartyNotification));
        return canSendBasedOnConfidentiality(newSscsCaseData, notificationEventType, partyMember) && isValid;
    }


    private boolean isValidRequestForInformationEventForParty(SscsCaseData newSscsCaseData, NotificationEventType notificationEventType, PartyItemList partyItem) {
        return REQUEST_FOR_INFORMATION.equals(notificationEventType)
            && newSscsCaseData.getInformationFromPartySelected() != null
            && newSscsCaseData.getInformationFromPartySelected().getValue() != null
            && partyItem.getCode().equals(newSscsCaseData.getInformationFromPartySelected().getValue().getCode());
    }

    private boolean isValidReviewConfidentialityRequest(NotificationEventType notificationEventType, DatedRequestOutcome previousRequestOutcome, DatedRequestOutcome latestRequestOutcome) {
        return REVIEW_CONFIDENTIALITY_REQUEST.equals(notificationEventType)
            && checkConfidentialityRequestOutcomeIsValidToSend(previousRequestOutcome, latestRequestOutcome);
    }

    private boolean checkConfidentialityRequestOutcomeIsValidToSend(DatedRequestOutcome previousRequestOutcome, DatedRequestOutcome latestRequestOutcome) {
        return latestRequestOutcome == null ? false : checkConfidentialityRequestOutcomeIsValidToSend(previousRequestOutcome, latestRequestOutcome.getRequestOutcome());
    }

    private boolean checkConfidentialityRequestOutcomeIsValidToSend(DatedRequestOutcome previousRequestOutcome, RequestOutcome latestRequestOutcome) {
        return (RequestOutcome.GRANTED.equals(latestRequestOutcome) && !isMatchingOutcome(previousRequestOutcome, RequestOutcome.GRANTED))
            || (RequestOutcome.REFUSED.equals(latestRequestOutcome) && !isMatchingOutcome(previousRequestOutcome, RequestOutcome.REFUSED));
    }

    private boolean isMatchingOutcome(DatedRequestOutcome datedRequestOutcome, RequestOutcome requestOutcome) {
        return datedRequestOutcome != null && requestOutcome != null && requestOutcome.equals(datedRequestOutcome.getRequestOutcome());
    }

    @Override
    public void setNotificationEventTypeOverridden(boolean notificationEventTypeOverridden) {
        this.notificationEventTypeOverridden = notificationEventTypeOverridden;
    }

    @Override
    public boolean hasNotificationEventBeenOverridden() {
        return notificationEventTypeOverridden;
    }

    @Override
    public void setSwitchLanguageType(boolean languageSwitched) {
        this.languageSwitched = languageSwitched;
    }

    @Override
    public boolean hasLanguageSwitched() {
        return languageSwitched;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CcdNotificationWrapper that = (CcdNotificationWrapper) o;
        return Objects.equals(responseWrapper, that.responseWrapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseWrapper);
    }
}

