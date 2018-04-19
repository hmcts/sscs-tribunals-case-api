package uk.gov.hmcts.sscs.model.reminder;

public class ReminderResponse {

    private String caseId;

    private String eventId;

    public String getCaseId() {
        return caseId;
    }

    public String getEventId() {
        return eventId;
    }

    public ReminderResponse() {
    }

    public ReminderResponse(String caseId, String eventId) {
        this.caseId = caseId;
        this.eventId = eventId;
    }
}
