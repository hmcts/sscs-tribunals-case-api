package uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import java.util.Arrays;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

@Getter
@AllArgsConstructor
public enum NotificationEventType {
    ACTION_HEARING_RECORDING_REQUEST(EventType.ACTION_HEARING_RECORDING_REQUEST, true, true, true, false, false, 0),
    ACTION_POSTPONEMENT_REQUEST(EventType.ACTION_POSTPONEMENT_REQUEST, true, true, true, true, false, 0),
    ACTION_POSTPONEMENT_REQUEST_WELSH(EventType.ACTION_POSTPONEMENT_REQUEST_WELSH, true, true, true, true, false, 0),
    ADJOURNED(EventType.ADJOURNED, true, false, false, false, false, 0),
    ADMIN_APPEAL_WITHDRAWN(EventType.ADMIN_APPEAL_WITHDRAWN, true, true, true, false, false, 0),
    ADMIN_CORRECTION_HEADER(EventType.ADMIN_CORRECTION_HEADER, true, true, true, true, false, 0),
    APPEAL_DORMANT(EventType.DORMANT, true, true, false, false, false, 0),
    APPEAL_LAPSED(EventType.LAPSED_REVISED, true, true, false, false, false, 0),
    APPEAL_RECEIVED(EventType.APPEAL_RECEIVED, true, true, false, false, false, 300L),
    APPEAL_WITHDRAWN(EventType.WITHDRAWN, true, true, false, false, false, 0),
    BUNDLE_CREATED_FOR_UPPER_TRIBUNAL(EventType.BUNDLE_CREATED_FOR_UPPER_TRIBUNAL, false, false, false, false, false, 0),
    CASE_UPDATED(EventType.CASE_UPDATED, false, false, false, false, false, 0),
    CORRECTION_GRANTED(EventType.CORRECTION_GRANTED, true, true, true, false, false, 0),
    CORRECTION_REFUSED(EventType.CORRECTION_REFUSED, true, true, true, false, false, 0),
    CORRECTION_REQUEST(EventType.CORRECTION_REQUEST, false, false, false, true, false, 0),
    DEATH_OF_APPELLANT(EventType.DEATH_OF_APPELLANT, true, true, true, true, false, 0),
    DECISION_ISSUED(EventType.DECISION_ISSUED, true, true, true, false, false, 0),
    DECISION_ISSUED_WELSH(EventType.DECISION_ISSUED_WELSH, true, true, true, false, false, 0),
    DIRECTION_ISSUED(EventType.DIRECTION_ISSUED, true, true, true, false, false, 0),
    DIRECTION_ISSUED_WELSH(EventType.DIRECTION_ISSUED_WELSH, true, true, true, false, false, 0),
    DRAFT_TO_NON_COMPLIANT(EventType.DRAFT_TO_NON_COMPLIANT, true, true, true, false, false, 0),
    DRAFT_TO_VALID_APPEAL_CREATED(EventType.DRAFT_TO_VALID_APPEAL_CREATED, true, true, false, true, false, 240L),
    DWP_APPEAL_LAPSED(EventType.CONFIRM_LAPSED, true, true, false, false, false, 0),
    DWP_RESPONSE_RECEIVED(EventType.DWP_RESPOND, true, true, true, false, false, 0),
    DWP_UPLOAD_RESPONSE(EventType.DWP_UPLOAD_RESPONSE, true, true, true, false, false, 60L),
    EVIDENCE_RECEIVED(EventType.EVIDENCE_RECEIVED, true, true, true, false, false, 0),
    EVIDENCE_REMINDER(EventType.EVIDENCE_REMINDER, true, true, false, false, true, 0),
    HEARING_BOOKED(EventType.HEARING_BOOKED, true, false, false, false, false, 0),
    HEARING_REMINDER(EventType.HEARING_REMINDER, true, false, false, false, true, 0),
    HMCTS_APPEAL_LAPSED(EventType.HMCTS_LAPSE_CASE, true, true, false, false, false, 0),
    ISSUE_ADJOURNMENT_NOTICE(EventType.ISSUE_ADJOURNMENT_NOTICE, true, true, true, false, false, 0),
    ISSUE_ADJOURNMENT_NOTICE_WELSH(EventType.ISSUE_ADJOURNMENT_NOTICE_WELSH, true, true, true, false, false, 0),
    ISSUE_FINAL_DECISION(EventType.ISSUE_FINAL_DECISION, true, true, true, false, false, 0),
    ISSUE_FINAL_DECISION_WELSH(EventType.ISSUE_FINAL_DECISION_WELSH, true, true, true, false, false, 0),
    JOINT_PARTY_ADDED(EventType.JOINT_PARTY_ADDED, true, true, true, true, false, 0),
    JUDGE_DECISION_APPEAL_TO_PROCEED(EventType.JUDGE_DECISION_APPEAL_TO_PROCEED, true, true, true, false, false, 0),
    LIBERTY_TO_APPLY_REQUEST(EventType.LIBERTY_TO_APPLY_REQUEST, false, false, false, false, false, 0),
    LIBERTY_TO_APPLY_GRANTED(EventType.LIBERTY_TO_APPLY_GRANTED, true, true, true, true, false, 0),
    LIBERTY_TO_APPLY_REFUSED(EventType.LIBERTY_TO_APPLY_REFUSED, true, true, true, true, false, 0),
    NON_COMPLIANT(EventType.NON_COMPLIANT, true, true, true, false, false, 0),
    PERMISSION_TO_APPEAL_GRANTED(EventType.PERMISSION_TO_APPEAL_GRANTED, true, true, true, false, false, 0),
    PERMISSION_TO_APPEAL_REFUSED(EventType.PERMISSION_TO_APPEAL_REFUSED, true, true, true, false, false, 0),
    PERMISSION_TO_APPEAL_REQUEST(EventType.PERMISSION_TO_APPEAL_REQUEST, true, true, true, false, false, 0),
    POSTPONEMENT(EventType.POSTPONED, true, false, false, false, false, 0),
    PROCESS_AUDIO_VIDEO(EventType.PROCESS_AUDIO_VIDEO, true, true, true, false, false, 0),
    PROCESS_AUDIO_VIDEO_WELSH(EventType.PROCESS_AUDIO_VIDEO_WELSH, true, true, true, false, false, 0),
    PROVIDE_APPOINTEE_DETAILS(EventType.PROVIDE_APPOINTEE_DETAILS, true, true, true, true, false, 0),
    // Allow out of hours for this event as we rely on the case data to decide who to send to. It could get out of sync if we wait a few hours to send, for example they could try to reissue to 2 parties so this event would be triggered twice.
    // If the reminder service looks the case up from CCD, the original request for whom to send the notification to will be lost and the second party would receive the notification twice.
    REISSUE_DOCUMENT(EventType.REISSUE_DOCUMENT, true, true, true, true, false, 0),
    REQUEST_FOR_INFORMATION(EventType.REQUEST_FOR_INFORMATION, true, true, true, false, false, 0),
    RESEND_APPEAL_CREATED(EventType.RESEND_APPEAL_CREATED, true, true, false, true, false, 0),
    REVIEW_AND_SET_ASIDE(EventType.REVIEW_AND_SET_ASIDE, true, true, true, true, false, 0),
    REVIEW_CONFIDENTIALITY_REQUEST(EventType.REVIEW_CONFIDENTIALITY_REQUEST, true, true, true, false, false, 0),
    SET_ASIDE_GRANTED(EventType.SET_ASIDE_GRANTED, true, true, true, true, false, 0),
    SET_ASIDE_REFUSED(EventType.SET_ASIDE_REFUSED, true, true, true, true, false, 0),
    SOR_EXTEND_TIME(EventType.SOR_EXTEND_TIME, true, true, true, true, false, 0),
    SOR_REFUSED(EventType.SOR_REFUSED, true, true, true, true, false, 0),
    SET_ASIDE_REQUEST(EventType.SET_ASIDE_REQUEST, false, false, false, false, false, 0),
    STATEMENT_OF_REASONS_REQUEST(EventType.SOR_REQUEST, false, false, false, false, false, 0),
    STRUCK_OUT(EventType.STRUCK_OUT, true, true, false, false, false, 0),
    SUBSCRIPTION_CREATED(EventType.SUBSCRIPTION_CREATED, true, true, false, false, false, 0),
    SUBSCRIPTION_OLD(null, false, true, false, true, false, 0),
    SUBSCRIPTION_UPDATED(EventType.SUBSCRIPTION_UPDATED, true, true, false, true, false, 0),
    SYA_APPEAL_CREATED(EventType.SYA_APPEAL_CREATED, true, true, false, true, false, 0),
    TCW_DECISION_APPEAL_TO_PROCEED(EventType.TCW_DECISION_APPEAL_TO_PROCEED, true, true, true, false, false, 0),
    UPDATE_OTHER_PARTY_DATA(EventType.UPDATE_OTHER_PARTY_DATA, true, true, true, true, false, 0),
    VALID_APPEAL_CREATED(EventType.VALID_APPEAL_CREATED, true, true, false, true, false, 240L),

    @JsonEnumDefaultValue
    DO_NOT_SEND(null);

    public static final String SUBSCRIPTION_OLD_ID = "subscriptionOld";

    private final EventType event;
    private boolean sendForOralCase;
    private boolean sendForPaperCase;
    private boolean sendForCohCase;
    private boolean allowOutOfHours;
    private boolean isReminder;
    private long delayInSeconds;

    NotificationEventType(EventType event) {
        this.event = event;
    }

    public static NotificationEventType getNotificationByEvent(String eventId) {
        return Arrays.stream(NotificationEventType.values())
            .filter(notification -> nonNull(notification.getId()))
            .filter(notification -> notification.getId().equalsIgnoreCase(eventId))
            .findFirst()
            .orElse(DO_NOT_SEND);
    }

    public static NotificationEventType getNotificationByCcdEvent(EventType eventType) {
        return Arrays.stream(NotificationEventType.values())
            .filter(notification -> notification.getEvent() == eventType)
            .findFirst()
            .orElse(DO_NOT_SEND);
    }

    public static boolean checkEvent(String eventId) {
        return Arrays.stream(NotificationEventType.values())
            .map(NotificationEventType::getId)
            .filter(Objects::nonNull)
            .anyMatch(id -> id.equals(eventId));
    }

    public boolean isToBeDelayed() {
        return delayInSeconds > 0;
    }

    public String getId() {
        if (this == SUBSCRIPTION_OLD) {
            return SUBSCRIPTION_OLD_ID;
        }
        if (isNull(event)) {
            return "";
        }
        return event.getCcdType();
    }
}
