package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public enum Status {

    ADJOURNED("adjourned"), APPEAL_RECEIVED("appealReceived"), CLOSED("closed"),
    DORMANT("dormant"), DWP_RESPOND("dwpRespond"), DWP_RESPOND_OVERDUE("dwpRespondOverdue"),
    EVIDENCE_RECEIVED("evidenceReceived"), HEARING("hearing"), HEARING_BOOKED("hearingBooked"),
    LAPSED_REVISED("lapsedRevised"), NEW_HEARING_BOOKED("newHearingBooked"),
    PAST_HEARING_BOOKED("pastHearingBooked"), POSTPONED("postponed"), WITHDRAWN("withdrawn");

    private String key;

    Status(String key) {
        this.key = key;
    }

    public String getContentKey() {
        return "status." + key;
    }
}
