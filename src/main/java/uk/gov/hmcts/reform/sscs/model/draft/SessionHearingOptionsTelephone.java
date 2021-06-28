package uk.gov.hmcts.reform.sscs.model.draft;

import lombok.Value;

@Value
public class SessionHearingOptionsTelephone {
    private Boolean requested;
    private String phoneNumber;
}
