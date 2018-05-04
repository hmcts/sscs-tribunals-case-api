package uk.gov.hmcts.sscs.model.ccd;

import com.google.common.base.CaseFormat;

public enum EventType {

    APPEAL_RECEIVED("appealReceived", "appealReceived", 1, true),
    DWP_RESPOND("dwpRespond", "responseReceived", 2, true),
    HEARING_BOOKED("hearingBooked", "hearingBooked", 3, true),
    HEARING("hearing", "hearing", 4, false),
    ADJOURNED("adjourned", "hearingAdjourned", 5, true),
    LAPSED_REVISED("lapsedRevised", "appealLapsed", 6, true),
    WITHDRAWN("withdrawn", "appealWithdrawn", 7, true),
    POSTPONED("postponed", "hearingPostponed",8, true),
    NEW_HEARING_BOOKED("newHearingBooked", "newHearingBooked", 9, true),
    PAST_HEARING_BOOKED("pastHearingBooked", "pastHearingBooked", 10, true),
    DORMANT("dormant", "appealDormant", 11, false),
    CLOSED("closed", "appealClosed", 12, false),
    DWP_RESPOND_OVERDUE("dwpRespondOverdue", "responseOverdue", 13, true),
    EVIDENCE_RECEIVED("evidenceReceived", "evidenceReceived", -1, true),
    EVIDENCE_REMINDER("evidenceRemainder", "evidenceReminder", -2, true),
    SUBSCRIBE_APPELLANT(0, true),
    SUBSCRIBE_SUPPORTER(0, true),
    SUBSCRIBE_APPELLANT_UPDATE_OLD(0, true),
    SUBSCRIBE_APPELLANT_UPDATE_NEW(0, true);

    private String type;
    private String ccdType;
    private final int order;
    private boolean notifiable;

    EventType(int order, boolean notifiable) {
        this.order = order;
        this.notifiable = notifiable;
    }

    EventType(String type, String ccdType, int order, boolean notifiable) {
        this.type = type;
        this.ccdType = ccdType;
        this.order = order;
        this.notifiable = notifiable;
    }

    public String getType() {
        return type;
    }

    public String getCcdType() {
        return ccdType;
    }

    public int getOrder() {
        return order;
    }

    public boolean isStatusEvent() {
        return order > 0;
    }

    public String getContentKey() {
        return "status." + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());
    }

    public boolean isNotifiable() {
        return notifiable;
    }

    public static EventType getEventTypeByCcdType(String ccdType) {
        EventType e = null;
        for (EventType event : EventType.values()) {
            if (ccdType.equals(event.ccdType)) {
                e = event;
            }
        }
        return e;
    }
}

