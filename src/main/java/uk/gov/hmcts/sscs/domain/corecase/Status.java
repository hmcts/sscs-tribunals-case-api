package uk.gov.hmcts.sscs.domain.corecase;

import com.google.common.base.CaseFormat;

public enum Status {

    ADJOURNED, APPEAL_RECEIVED, CLOSED, DORMANT, DWP_RESPOND, DWP_RESPOND_OVERDUE,
    EVIDENCE_RECEIVED, HEARING, HEARING_BOOKED, LAPSED_REVISED, NEW_HEARING_BOOKED,
    PAST_HEARING_BOOKED, POSTPONED, WITHDRAWN;


    public String getContentKey() {
        return "status." + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());
    }
}
