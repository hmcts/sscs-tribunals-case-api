package uk.gov.hmcts.reform.sscs.model.draft;

import lombok.Value;

@Value
public class SessionReasonForAppealingItem {
    private String whatYouDisagreeWith;
    private String reasonForAppealing;
}
