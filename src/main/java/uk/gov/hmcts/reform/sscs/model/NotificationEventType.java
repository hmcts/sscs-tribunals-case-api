package uk.gov.hmcts.reform.sscs.model;

public enum NotificationEventType {

    ADJOURNED_NOTIFICATION("hearingAdjourned"),
    SYA_APPEAL_CREATED_NOTIFICATION("appealCreated"),
    APPEAL_LAPSED_NOTIFICATION("appealLapsed"),
    APPEAL_RECEIVED_NOTIFICATION("appealReceived"),
    APPEAL_WITHDRAWN_NOTIFICATION("appealWithdrawn"),
    APPEAL_DORMANT_NOTIFICATION("appealDormant"),
    EVIDENCE_RECEIVED_NOTIFICATION("evidenceReceived"),
    DWP_RESPONSE_RECEIVED_NOTIFICATION("responseReceived"),
    HEARING_BOOKED_NOTIFICATION("hearingBooked"),
    POSTPONEMENT_NOTIFICATION("hearingPostponed"),
    SUBSCRIPTION_CREATED_NOTIFICATION("subscriptionCreated"),
    SUBSCRIPTION_UPDATED_NOTIFICATION("subscriptionUpdated"),
    SUBSCRIPTION_OLD_NOTIFICATION("subscriptionOld"),
    EVIDENCE_REMINDER_NOTIFICATION("evidenceReminder"),
    FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION("hearingHoldingReminder"),
    SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION("secondHearingHoldingReminder"),
    THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION("thirdHearingHoldingReminder"),
    FINAL_HEARING_HOLDING_REMINDER_NOTIFICATION("finalHearingHoldingReminder"),
    HEARING_REMINDER_NOTIFICATION("hearingReminder"),
    DWP_RESPONSE_LATE_REMINDER_NOTIFICATION("dwpResponseLateReminder"),
    QUESTION_ROUND_ISSUED_NOTIFICATION("question_round_issued"),
    QUESTION_DEADLINE_ELAPSED_NOTIFICATION("question_deadline_elapsed"),
    QUESTION_DEADLINE_REMINDER_NOTIFICATION("question_deadline_reminder"),
    HEARING_REQUIRED_NOTIFICATION("continuous_online_hearing_relisted"),
    VIEW_ISSUED("decision_issued"),
    DECISION_ISSUED_2("decision_issued_2"), // placeholder until COH name this notification
    CREATE_APPEAL_PDF("createAppealPDF"),
    RESEND_CASE_TO_GAPS2("resendCaseToGAPS2"),
    DO_NOT_SEND("");

    private final String id;

    NotificationEventType(String id) {
        this.id = id;
    }

    public static NotificationEventType getNotificationById(String id) {
        NotificationEventType b = null;
        for (NotificationEventType type : NotificationEventType.values()) {
            if (type.getId().equals(id)) {
                b = type;
            }
        }
        return b;
    }

    private String getId() {
        return id;
    }
}
