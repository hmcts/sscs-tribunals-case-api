package uk.gov.hmcts.reform.sscs.model;

public enum NotificationEventType {

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

    public String getId() {
        return id;
    }
}
