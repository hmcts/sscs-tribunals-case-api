package uk.gov.hmcts.sscs.domain.corecase;

import com.google.common.base.CaseFormat;

public enum EventType {

    APPEAL_RECEIVED(1, true),
    DWP_RESPOND(2, true),
    HEARING_BOOKED(3, true),
    HEARING(4, false),
    ADJOURNED(5, true),
    LAPSED_REVISED(6, true),
    WITHDRAWN(7, true),
    POSTPONED(8, true),
    NEW_HEARING_BOOKED(9, true),
    PAST_HEARING_BOOKED(10, true),
    DORMANT(11, false),
    CLOSED(12, false),
    DWP_RESPOND_OVERDUE(13, true),
    EVIDENCE_RECEIVED(-1, true),
    EVIDENCE_REMINDER(-2, true),
    SUBSCRIBE_APPELLANT(0, true),
    SUBSCRIBE_SUPPORTER(0, true),
    SUBSCRIBE_APPELLANT_UPDATE_OLD(0, true),
    SUBSCRIBE_APPELLANT_UPDATE_NEW(0, true);

    private final int order;
    private boolean notifiable;

    EventType(int order, boolean notifiable) {
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
}
