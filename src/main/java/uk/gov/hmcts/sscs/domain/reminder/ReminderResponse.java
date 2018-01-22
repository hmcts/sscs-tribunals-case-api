package uk.gov.hmcts.sscs.domain.reminder;

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

    @Override
    public String toString() {
        return "ReminderResponse{"
                + " caseId='" + caseId + '\''
                + ", eventId='" + eventId + '\''
                + '}';
    }
}
