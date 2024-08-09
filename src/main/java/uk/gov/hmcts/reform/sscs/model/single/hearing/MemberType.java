package uk.gov.hmcts.reform.sscs.model.single.hearing;

import lombok.Getter;

@Getter
public enum MemberType {

    JOH("JOH"),
    PANEL_MEMBER("PANEL_MEMBER");

    private final String value;

    MemberType(String value) {
        this.value = value;
    }
}
