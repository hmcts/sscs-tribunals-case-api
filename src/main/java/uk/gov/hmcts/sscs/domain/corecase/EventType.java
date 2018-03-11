package uk.gov.hmcts.sscs.domain.corecase;

import com.google.common.base.CaseFormat;

public enum EventType {

    APPEAL_RECEIVED("appealReceived", 1, true),
    DWP_RESPOND("dwpRespond", 2, true),
    HEARING_BOOKED("hearingBooked", 3, true),
    HEARING("hearing", 4, false),
    ADJOURNED("adjourned", 5, true),
    LAPSED_REVISED("lapsedRevised", 6, true),
    WITHDRAWN("appealWithdrawn", 7, true),
    POSTPONED("hearingPostponed", 8, true),
    NEW_HEARING_BOOKED("", 9, true),
    PAST_HEARING_BOOKED(10, true),
    DORMANT("appealDormant", 11, false),
    CLOSED(12, false),
    DWP_RESPOND_OVERDUE(13, true),
    EVIDENCE_RECEIVED("evidenceReceived", -1, true),
    EVIDENCE_REMINDER("evidenceRemainder", -2, true),
    SUBSCRIBE_APPELLANT(0, true),
    SUBSCRIBE_SUPPORTER(0, true),
    SUBSCRIBE_APPELLANT_UPDATE_OLD(0, true),
    SUBSCRIBE_APPELLANT_UPDATE_NEW(0, true);

    private String type;
    private final int order;
    private boolean notifiable;

    EventType(int order, boolean notifiable) {
        this.order = order;
        this.notifiable = notifiable;
    }

    EventType(String type, int order, boolean notifiable) {
        this.type = type;
        this.order = order;
        this.notifiable = notifiable;
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

    public static EventType getEventTypeByType(String type) {
        EventType e = null;
        for (EventType event : EventType.values()) {
            if (type.equals(event.type)) {
                e = event;
            }
        }
        return e;
    }
}
