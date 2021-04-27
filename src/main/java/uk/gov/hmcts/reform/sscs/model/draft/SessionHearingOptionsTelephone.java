package uk.gov.hmcts.reform.sscs.model.draft;

import lombok.Value;

@Value
public class SessionHearingOptionsTelephone {
    private String requested;
    private String phoneNumber;
}
