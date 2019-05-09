package uk.gov.hmcts.reform.sscs.model.draft;

import lombok.Value;

@Value
public class SessionAppellantName {
    private String title;
    private String firstName;
    private String lastName;
}
